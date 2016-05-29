package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.utils.Multimap;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ClassUtils;
import org.apache.zookeeper.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matteopelati on 25/10/15.
 */

public class ZookeperServiceDirectory extends InMemoryServiceDirectory implements RemoteServiceDirectory, Watcher {

    final static Logger logger = LoggerFactory.getLogger(ZookeperServiceDirectory.class);

    final static String ROOT_PATH_IDS = "/beanszoo/ids";
    final static String ROOT_PATH_TYPES = "/beanszoo/types";

    Map<String,String> remoteNamedServices = new ConcurrentHashMap<>();
    Map<String,Map<String,String>> remoteClassServices = new ConcurrentHashMap<>();
    Multimap<String,String> localClassIndex = new Multimap<>();

    boolean running = false;
    Random random = new Random();

    final String connectionString;
    final String localAddr;
    ZooKeeper zk;

    public ZookeperServiceDirectory(String connectionString, RpcServerAddress localAddr) {
        this.connectionString = connectionString;
        this.localAddr = localAddr.geAddressString();
    }

    public ZookeperServiceDirectory(String connectionString) {
        this.connectionString = connectionString;
        this.localAddr = null;
    }

    @Override
    public synchronized void start() throws Exception {
        super.start();
        zk = new ZooKeeper(connectionString, 5000, this);
        initializePath(ROOT_PATH_IDS);
        initializePath(ROOT_PATH_TYPES);
        running = true;
        retrieveRemoteServiceIds();
        retrieveRemoteServiceTypes();
        for (Map.Entry<String,Object> s : namedServices.entrySet())
            registerService(s.getKey(), s.getValue());
    }

    synchronized void retrieveRemoteServiceIds()  {
        try {
            List<String> children = zk.getChildren(ROOT_PATH_IDS, true);
            for (String id : children) {
                byte[] data = zk.getData(ROOT_PATH_IDS + "/" + id, true, null);
                String addr = new String(data, "UTF-8");
                if (localAddr == null || (!addr.startsWith(localAddr)))
                    remoteNamedServices.put(id, new String(data, "UTF-8"));
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void retrieveRemoteServiceTypes()  {
        try {
            List<String> children = zk.getChildren(ROOT_PATH_TYPES, true);
            for (String id : children) {
                byte[] data = zk.getData(ROOT_PATH_TYPES + "/" + id, true, null);
                String addr = new String(data, "UTF-8");
                if (!namedServices.containsKey(addr)) {
                    String[] tokens = id.split("@");
                    Map<String,String> ids = remoteClassServices.get(tokens[0]);
                    if (ids == null) {
                        ids = new ConcurrentHashMap<>();
                        remoteClassServices.put(tokens[0], ids);
                    }
                    ids.put(tokens[1], addr);
                }
                else {
                    String[] tokens = id.split("@");
                    localClassIndex.put(addr, id);
                }
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized void registerService(String id, Object service) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        if (localAddr == null)
            throw new IllegalArgumentException("This service directory is read only");

        zk.create(ROOT_PATH_IDS + "/" + id, localAddr.getBytes("UTF-8"), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);

        Set<Class<?>> allClasses = new HashSet<Class<?>>();
        allClasses.addAll(ClassUtils.getAllInterfaces(service.getClass()));
        Name name = service.getClass().getAnnotation(Name.class);

        for (Class<?> klass: allClasses) {
            zk.create(
                    ROOT_PATH_TYPES + "/" + klass.getCanonicalName() + "@",
                    id.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL
            );
            if (name != null) {
                zk.create(
                        ROOT_PATH_TYPES + "/" + klass.getCanonicalName() + "!" + name.value() + "@",
                        id.getBytes(),
                        ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL
                );
            }
        }
    }

    synchronized void unregisterService(String id) throws KeeperException, InterruptedException, UnsupportedEncodingException {

        if (localAddr == null)
            throw new IllegalArgumentException("This service directory is read only");

        Set<String> klasses = localClassIndex.get(id);
        for (String klass : klasses) {
            zk.delete(ROOT_PATH_TYPES + "/" + klass, -1);
        }
        zk.delete(ROOT_PATH_IDS + "/" + id, -1);
    }

    public String trim( String stringToTrim, String stringToRemove )
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
            for (String sid : namedServices.keySet())
                unregisterService(sid);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }

        running = false;
        zk.close();
        remoteNamedServices.clear();
        remoteClassServices.clear();
    }

    @Override
    public RemoteService getRemoteService(String id, Class type) {
        String remoteHost = remoteNamedServices.get(id);
        if (remoteHost == null) return null;
        return new RemoteService(remoteHost, id, type);
    }

    @Override
    public RemoteService getRemoteServiceByType(Class type, String name) {
        Map<String,String> ids = remoteClassServices.get(type.getCanonicalName() + ((name == null) ? "" : ("!" + name)));
        if (ids == null || ids.size() == 0) return null;
        int index = random.nextInt(ids.size());
        return getRemoteService((String)ids.values().toArray()[index], type);
    }

    @Override
    public Set<RemoteService> getRemoteServicesByType(Class type) {
        Map<String,String> ids = remoteClassServices.get(type.getCanonicalName());
        if (ids == null) return Collections.emptySet();
        Set<RemoteService> remoteServices = new HashSet<>();
        for (String id : ids.values())
            remoteServices.add(getRemoteService(id, type));
        return remoteServices;
    }

    @Override
    public synchronized void putService(String id, Object service) {
        try {
            super.putService(id, service);
            if (running)
                registerService(id, service);
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized boolean removeService(String id, Object service) {
        try {
            if (!super.removeService(id, service))
                return false;
            if (running)
                unregisterService(id);
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
                if (e.getPath().startsWith(ROOT_PATH_IDS))
                    retrieveRemoteServiceIds();
                else if (e.getPath().startsWith(ROOT_PATH_TYPES))
                    retrieveRemoteServiceTypes();
                break;
            case NodeCreated:
                break;
            case NodeDataChanged:
                break;
            case NodeDeleted:
                if (e.getPath().startsWith(ROOT_PATH_IDS)) {
                    int idx = e.getPath().lastIndexOf("/");
                    remoteNamedServices.remove(e.getPath().substring(idx+1));
                    localClassIndex.removeAll(e.getPath().substring(idx + 1));
                }
                else if (e.getPath().startsWith(ROOT_PATH_TYPES)) {
                    String[] tokens = e.getPath().split("@");
                    int idx = tokens[0].lastIndexOf("/");
                    remoteClassServices.get(tokens[0].substring(idx+1)).remove(tokens[1]);
                }
                break;
        }
    }
}
