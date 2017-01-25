package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import lombok.AllArgsConstructor;
import org.apache.zookeeper.*;
import org.nustaq.serialization.FSTConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by matteopelati on 25/10/15.
 */

public class ZookeeperServiceDirectory extends AbstractServiceDirectory implements Watcher {

    @AllArgsConstructor
    static class Key {
        String root;
        String key;
        String id;
        String address;
    }


    final static Logger logger = LoggerFactory.getLogger(ZookeeperServiceDirectory.class);

    final static String ROOT_PATH_IDS = "/beanszoo/ids";
    final static String ROOT_PATH_TYPES = "/beanszoo/types";

    boolean running = false;
    Random random = new Random();

    Map<String, ServiceDescriptor> allDescriptors = new HashMap<>();
    final String connectionString;
    ZooKeeper zk;
    final FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();

    public ZookeeperServiceDirectory(RpcServerAddress localAddr, String connectionString) {
        super(localAddr);
        this.connectionString = connectionString;
    }

    @Override
    public synchronized void start() throws Exception {
        zk = new ZooKeeper(connectionString, 120000, this);
        initializePath(ROOT_PATH_IDS);
        initializePath(ROOT_PATH_TYPES);
        running = true;
        retrieveRemoteServiceIds();
        retrieveRemoteServiceTypes();
        for (Map.Entry<String,Object> s : localIds.entrySet())
            zkRegisterService(s.getKey(), s.getValue());
        super.start();
    }


    String encodeKey(String root, String key, String id) {
        return Paths.get(root, key + "@" + id + "@" + localAddress.geAddressString()).toString();
    }

    Key decodeKey(String key) {
        String[] tokens = key.split("@");
        int idx = tokens[0].lastIndexOf("/");
        return new Key(
                tokens[0].substring(0, idx), tokens[0].substring(idx + 1), tokens[1], tokens[2]
        );
    }

    synchronized void retrieveRemoteServiceIds()  {
        try {
            List<String> children = zk.getChildren(ROOT_PATH_IDS, true);
            for (String key : children) {
                String fullPath = Paths.get(ROOT_PATH_IDS, key).toString();
                logger.info("Retrieving child: " + fullPath);
                byte[] data = zk.getData(fullPath, true, null);
                ServiceDescriptor desc = (ServiceDescriptor) fst.asObject(data);
                if (!desc.getAddress().equals(localAddress.geAddressString())) {
                    allDescriptors.put(fullPath, desc);
                    allIds.put(desc.getPath(), desc);
                }
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void retrieveRemoteServiceTypes()  {
        try {
            List<String> children = zk.getChildren(ROOT_PATH_TYPES, true);
            for (String key : children) {
                String fullPath = Paths.get(ROOT_PATH_TYPES, key).toString();
                logger.info("Retrieving child: " + fullPath);
                byte[] data = zk.getData(fullPath, true, null);
                ServiceDescriptor desc = (ServiceDescriptor) fst.asObject(data);
                if (!desc.getAddress().equals(localAddress.geAddressString())) {
                    allDescriptors.put(fullPath, desc);
                    allInterfaces.put(desc.getPath(), desc);
                }
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void handleNodeChildrenChanged(String path) {

        logger.info("Children of path " + path + " changed. Retrieving new children...");
        if (path.startsWith(ROOT_PATH_IDS)) {
            retrieveRemoteServiceIds();
        }
        else if (path.startsWith(ROOT_PATH_TYPES)) {
            retrieveRemoteServiceTypes();
        }
    }

    synchronized void handleNodeDeleted(String path) {

        Key key = decodeKey(path);

        if (path.startsWith(ROOT_PATH_IDS)) {
            allIds.remove(key.key);
            allDescriptors.remove(path);
        }
        else if (path.startsWith(ROOT_PATH_TYPES)) {
            allInterfaces.remove(key.key, allDescriptors.remove(path));
        }

    }

    synchronized void zkRegisterService(String id, Object service) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        ServiceDescriptor idSvc = getIdLocalServiceDescriptor(id);
        String idkey = encodeKey(ROOT_PATH_IDS, idSvc.getPath(), id);
        logger.info("Registering local service key: " + idkey);
        allDescriptors.put(idkey, idSvc);
        zk.create(
                idkey, fst.asByteArray(idSvc),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL
        );

        Map<String, ServiceDescriptor> ifSvc = getInterfaceLocalServiceDescriptors(service, id);
        for (Map.Entry<String, ServiceDescriptor> e : ifSvc.entrySet()) {
            String classkey = encodeKey(ROOT_PATH_TYPES, e.getKey(), id);
            allDescriptors.put(classkey, e.getValue());
            logger.info("Registering local service key: " + classkey);
            zk.create(
                    classkey, fst.asByteArray(e.getValue()),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL
            );
        }
    }

    synchronized void zkUnregisterService(String id) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        Map<String, ServiceDescriptor> ifSvc = getInterfaceLocalServiceDescriptors(localIds.get(id), id);
        for (Map.Entry<String, ServiceDescriptor> d : ifSvc.entrySet()) {
            String classkey = encodeKey(ROOT_PATH_TYPES, d.getKey(), id);
            allDescriptors.remove(classkey);
            logger.info("Unregistering local service key: " + classkey);
            zk.delete(classkey, -1);
        }

        String idkey = encodeKey(ROOT_PATH_IDS, id, id);
        logger.info("Unregistering local service key: " + idkey);
        allDescriptors.remove(idkey);
        zk.delete(idkey, -1);
    }

    public String trim(String stringToTrim, String stringToRemove )
    {
        String answer = stringToTrim;
        while( answer.startsWith( stringToRemove ) )
            answer = answer.substring( stringToRemove.length() );

        while( answer.endsWith( stringToRemove ) )
            answer = answer.substring( 0, answer.length() - stringToRemove.length() );

        return answer;
    }


    void initializePath(String path) throws InterruptedException, KeeperException {

        String[] tokens = trim(path, "/").split("/");
        List<String> current = new ArrayList<>();
        for (String token : tokens) {
            current.add(token);
            try {
                zk.create("/" + String.join("/", current), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            catch(KeeperException.NodeExistsException e) {

            }
        }
    }

    @Override
    public synchronized void stop() throws Exception {
        super.stop();
        try {
            for (String sid : localIds.keySet())
                zkUnregisterService(sid);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }

        running = false;
        zk.close();
    }

    @Override
    public synchronized void putService(String id, Object service) {
        try {
            super.putService(id, service);
            if (running)
                zkRegisterService(id, service);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean removeService(Object service) {
        try {
            String id = localIdsInverted.get(service);
            if (id == null) return false;
            if (running)
                zkUnregisterService(localIdsInverted.get(service));
            super.removeService(service);
            return true;
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean removeService(String id) {
        try {
            Object service = localIdsInverted.get(id);
            if (service == null) return false;
            if (running)
                zkUnregisterService(localIdsInverted.get(service));
            super.removeService(service);
            return true;
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    synchronized public void process(WatchedEvent e) {

        if (!running) return;

        logger.info("BeansZoo Event " + ": " + e.toString());
        switch(e.getType()) {
            case NodeChildrenChanged:
                handleNodeChildrenChanged(e.getPath());
                break;
            case NodeCreated:
                break;
            case NodeDataChanged:
                break;
            case NodeDeleted:
                handleNodeDeleted(e.getPath());
                break;
        }
    }
}
