package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import lombok.AllArgsConstructor;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;
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

    public static final int DEFAULT_ZK_TIMEOUT = 5000;

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
    final static String ROOT_PATH_LOCKS = "/beanszoo/locks";

    Map<String, ServiceDescriptor> allDescriptors = new HashMap<>();
    final FSTConfiguration fst = FSTConfiguration.createDefaultConfiguration();
    final String connectionString;
    ZooKeeper zkClient;
    int zkTimeout;

    public ZookeeperServiceDirectory(RpcServerAddress localAddr, String connectionString, int zkTimeout) {
        super(localAddr);
        this.connectionString = connectionString;
        this.zkTimeout = zkTimeout;
    }

    public ZookeeperServiceDirectory(RpcServerAddress localAddr, String connectionString) {
        super(localAddr);
        this.connectionString = connectionString;
        this.zkTimeout = DEFAULT_ZK_TIMEOUT;
    }

    @Override
    public void doStart() throws Exception {

        zkClient = new ZooKeeper(connectionString, zkTimeout, this);
        initializePath(ROOT_PATH_IDS);
        initializePath(ROOT_PATH_TYPES);
        initializePath(ROOT_PATH_LOCKS);
        retrieveRemoteServiceIds();
        retrieveRemoteServiceTypes();
        super.doStart();

    }

    @Override
    public void doStop() throws Exception {
        super.doStop();
        zkClient.close();
    }

    @Override
    boolean activateService(String id) throws Exception {
        if (!beforeRegistration(id, registeredServices.get(id)))
            return false;
        if (!super.activateService(id))
            return false;
        zkRegisterService(id, registeredServices.get(id));
        return true;
    }

    @Override
    boolean deactivateService(String id) throws Exception {
        if (!super.deactivateService(id))
            return false;
        zkUnregisterService(id);
        return true;
    }

    String encodeKey(String root, String key, String id) {
        return Paths.get(root, key + "@" + id + "@" + getLocalAddress().geAddressString()).toString();
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

            List<String> children = zkClient.getChildren(ROOT_PATH_IDS, true);
            for (String key : children) {
                String fullPath = Paths.get(ROOT_PATH_IDS, key).toString();
                logger.info("Retrieving child: " + fullPath);
                byte[] data = zkClient.getData(fullPath, true, null);
                ServiceDescriptor desc = (ServiceDescriptor) fst.asObject(data);
                if (!desc.getAddress().equals(getLocalAddress().geAddressString())) {
                    allDescriptors.put(fullPath, desc);
                    addRunningService(desc.getPath(), desc);
                }
            }

        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void retrieveRemoteServiceTypes()  {
        try {

            List<String> children = zkClient.getChildren(ROOT_PATH_TYPES, true);
            for (String key : children) {
                String fullPath = Paths.get(ROOT_PATH_TYPES, key).toString();
                logger.info("Retrieving child: " + fullPath);
                byte[] data = zkClient.getData(fullPath, true, null);
                ServiceDescriptor desc = (ServiceDescriptor) fst.asObject(data);
                if (!desc.getAddress().equals(getLocalAddress().geAddressString())) {
                    allDescriptors.put(fullPath, desc);
                    addRunningInterface(desc.getPath(), desc);
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

        if (path.startsWith(ROOT_PATH_IDS)) {
            Key key = decodeKey(path);
            removeRunningService(key.key);
            allDescriptors.remove(path);
        }
        else if (path.startsWith(ROOT_PATH_TYPES)) {
            Key key = decodeKey(path);
            removeRunningInterface(key.key, allDescriptors.remove(path));
        }
        else if (path.startsWith(ROOT_PATH_LOCKS)) {
            try {
                String relative = Paths.get(ROOT_PATH_LOCKS).relativize(Paths.get(path)).toString();
                unlock(relative);
            }
            catch (Exception e) {

            }
        }

    }

    synchronized void zkRegisterService(String id, Object service) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        Object metadata = (service instanceof HasMetadata) ? ((HasMetadata) service).getMetadata() : null;

        ServiceDescriptor idSvc = getIdLocalServiceDescriptor(id, metadata);
        String idkey = encodeKey(ROOT_PATH_IDS, idSvc.getPath(), id);
        logger.info("Registering local service key: " + idkey);
        allDescriptors.put(idkey, idSvc);
        zkClient.create(
                idkey, fst.asByteArray(idSvc),
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL
        );

        Map<String, ServiceDescriptor> ifSvc = getInterfaceLocalServiceDescriptors(service, id);
        for (Map.Entry<String, ServiceDescriptor> e : ifSvc.entrySet()) {
            String classkey = encodeKey(ROOT_PATH_TYPES, e.getKey(), id);
            allDescriptors.put(classkey, e.getValue());
            logger.info("Registering local service key: " + classkey);
            zkClient.create(
                    classkey, fst.asByteArray(e.getValue()),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL
            );
        }
    }

    synchronized void zkUnregisterService(String id) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        Map<String, ServiceDescriptor> ifSvc = getInterfaceLocalServiceDescriptors(registeredServices.get(id), id);
        for (Map.Entry<String, ServiceDescriptor> d : ifSvc.entrySet()) {
            String classkey = encodeKey(ROOT_PATH_TYPES, d.getKey(), id);
            allDescriptors.remove(classkey);
            logger.info("Unregistering local service key: " + classkey);
            zkClient.delete(classkey, -1);
        }

        String idkey = encodeKey(ROOT_PATH_IDS, id, id);
        logger.info("Unregistering local service key: " + idkey);
        allDescriptors.remove(idkey);
        zkClient.delete(idkey, -1);
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
                zkClient.create("/" + String.join("/", current), null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
            catch(KeeperException.NodeExistsException e) {

            }
        }
    }



    @Override
    public synchronized void putService(String id, Object service) throws Exception {
        super.putService(id, service);
    }

    @Override
    public synchronized void removeService(Object service) throws Exception {
        super.removeService(service);
    }

    @Override
    public synchronized void removeService(String id) throws Exception {
        super.removeService(id);
    }

    @Override
    public synchronized boolean addLock(String id, UnlockCb unlockCb) throws Exception {
        super.addLock(id, unlockCb);

        while (true) {

            try {
                zkClient.create(
                        Paths.get(ROOT_PATH_LOCKS, id).toString(), new byte[0],
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL
                );
                zkClient.exists(Paths.get(ROOT_PATH_LOCKS, id).toString(), true);
                return true;
            } catch (KeeperException e) {
                if (e.code().equals(KeeperException.Code.NODEEXISTS)) {
                    Stat node = zkClient.exists(Paths.get(ROOT_PATH_LOCKS, id).toString(), true);
                    if (node != null) return false;
                }
                throw e;
            } catch (Exception e) {
                throw e;
            }

        }
    }

    @Override
    public synchronized void removeLock(String id) throws Exception {

        super.removeLock(id);
        try {
            zkClient.delete(
                    Paths.get(ROOT_PATH_LOCKS, id).toString(), -1
            );
        } catch (KeeperException e) {
            if (!e.code().equals(KeeperException.Code.NONODE)) {
                throw e;
            }
        } catch (Exception e) {
            throw e;
        }

    }

    @Override
    synchronized public void process(WatchedEvent e) {

        if (!isRunning()) return;

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
