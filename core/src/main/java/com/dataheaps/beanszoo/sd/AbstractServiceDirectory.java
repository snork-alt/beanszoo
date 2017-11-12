package com.dataheaps.beanszoo.sd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.ClassUtils;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.policies.LocalRandomPolicy;
import com.dataheaps.beanszoo.utils.Multimap;

/**
 * Created by matteopelati on 25/10/15.
 */


public abstract class AbstractServiceDirectory implements ServiceDirectory {

    static final Class<?> DefaultPolicy = LocalRandomPolicy.class;

    private final RpcServerAddress localAddress;

    Map<String, Object> registeredServices = new HashMap<>();
    Map<Object, String> registeredServicesInvertedIndex = new HashMap<>();

    private Map<String, Object> runningLocalServices = new HashMap<>();
    private Multimap<String, Object> runningLocalInterfaces = new Multimap<>();

    private Map<String, UnlockCb> watchedServices = new HashMap<>();

    private Map<String, ServiceDescriptor> allRunningServices = new ConcurrentHashMap<>();
    private Multimap<String, ServiceDescriptor> allRunningInterfaces = new Multimap<>();

    private boolean running = false;

    AbstractServiceDirectory(RpcServerAddress localAddress) {
        this.localAddress = localAddress;
    }

    boolean isRunning() {
        return running;
    }

    void registerService(String id, Object service) throws Exception {
        registeredServices.put(id, service);
        registeredServicesInvertedIndex.put(service, id);
    }

    void unregisterService(String id) throws Exception {
        Object service = registeredServices.remove(id);
        registeredServicesInvertedIndex.remove(service);
    }

    void addRunningService(String path, ServiceDescriptor sd) {
        allRunningServices.put(path, sd);
    }

    void addRunningInterface(String path, ServiceDescriptor sd) {
        allRunningInterfaces.put(path, sd);
    }

    void removeRunningService(String path) {
        allRunningServices.remove(path);
    }

    void removeRunningInterface(String path, ServiceDescriptor sd) {
        allRunningInterfaces.remove(path, sd);
    }


    RpcServerAddress getLocalAddress() {
        return localAddress;
    }

    boolean activateService(String id) throws Exception {

        if (runningLocalServices.containsKey(id))
            return false;

        Object service = registeredServices.get(id);

        if (service instanceof Activable)
            ((Activable) service).activate();

        runningLocalServices.put(id, service);
        Map<String, ServiceDescriptor> ifaces = getInterfaceLocalServiceDescriptors(service, id);
        for (String i: ifaces.keySet())
            runningLocalInterfaces.put(i, service);

        Object metadata = (service instanceof HasMetadata) ? ((HasMetadata) service).getMetadata() : null;

        allRunningServices.put(id, getIdLocalServiceDescriptor(id, metadata));
        allRunningInterfaces.putAll(ifaces);

        return true;

    }

    boolean deactivateService(String id) throws Exception {

        if (!runningLocalServices.containsKey(id))
            return false;

        Object service = registeredServices.get(id);

        if (service instanceof Activable)
            ((Activable) service).deactivate();

        Map<String, ServiceDescriptor> ifaces = getInterfaceLocalServiceDescriptors(service, id);
        allRunningServices.remove(id);
        allRunningInterfaces.removeAll(ifaces);

        runningLocalServices.remove(id);
        for (String i: ifaces.keySet())
            runningLocalInterfaces.remove(i, service);

        return true;

    }

    public Object getLocalInstance(ServiceDescriptor d) {

        if (!d.getAddress().equals(localAddress.geAddressString()))
            return null;

        if (d.getId() != null)
            return runningLocalServices.get(d.getId());
        else {
            Set<Object> ls = runningLocalInterfaces.get(d.getPath());
            if (ls.isEmpty()) return null;
            return new ArrayList<Object>(ls).get(0);
        }
    }

    ServiceDescriptor getIdLocalServiceDescriptor(String id, Object metadata) {

        // FIXME: Make sure the same service type (the implementation, meaning same class) is not registered twice in a local ServiceDirectory
        return new ServiceDescriptor(localAddress.geAddressString(), id, null, null, id, metadata);
    }

    Map<String, ServiceDescriptor> getInterfaceLocalServiceDescriptors(Object service, String id) {

        Map<String, ServiceDescriptor> serviceDescriptors = new HashMap<>();
        Name names = service.getClass().getAnnotation(Name.class);
        Object metadata = (service instanceof HasMetadata) ? ((HasMetadata) service).getMetadata() : null;

        Set<Class<?>> allClasses = new HashSet<>();
        allClasses.addAll(ClassUtils.getAllInterfaces(service.getClass()));

        allClasses.forEach(
                c -> {
                    serviceDescriptors.put(
                            c.getCanonicalName(),
                            new ServiceDescriptor(
                                    localAddress.geAddressString(), null, c, null, c.getCanonicalName(), metadata
                            )
                    );
                    if (names != null) {
                        for (String name: names.value()) {
                            serviceDescriptors.put(
                                    c.getCanonicalName() + "!" + name,
                                    new ServiceDescriptor(
                                            localAddress.geAddressString(), null, c, name,
                                            c.getCanonicalName() + "!" + name, metadata
                                    )
                            );
                        }
                    }
                }
        );
        return serviceDescriptors;

    }



    @Override
    public synchronized ServiceDescriptor getService(String id) {

        if (!running)
            throw new IllegalStateException("Service Directory is not running");

        return allRunningServices.get(id);
    }

    @Override
    public synchronized Set<ServiceDescriptor> getServicesByType(Class<?> type, String name) {

        if (!running)
            throw new IllegalStateException("Service Directory is not running");

        Set<ServiceDescriptor> ret = allRunningInterfaces.get(type.getCanonicalName() + ((name == null) ? "" : ("!" + name)));
        if (ret == null || ret.size() == 0) return Collections.emptySet();
        return ret;
    }

    @Override
    public synchronized void putService(Object service) throws Exception {
        putService(UUID.randomUUID().toString(), service);
    }

    @Override
    public synchronized void putService(String id, Object service) throws Exception {
        registerService(id, service);
        if (running) activateService(id);
    }

    @Override
    public synchronized void removeService(Object service) throws Exception {
        removeService(registeredServicesInvertedIndex.get(service));
    }

    @Override
    public synchronized void removeService(String id) throws Exception {
        if (running)
            deactivateService(id);
        unregisterService(id);
    }


    boolean beforeRegistration(String id, Object service) throws Exception {
        OnRegister ann = service.getClass().getAnnotation(OnRegister.class);
        if (ann == null) return true;
        RegistrationHandler handler = (RegistrationHandler) ann.value().newInstance();
        return handler.beforeRegister(id, service, this);
    }

    void beforeDeregistration(String id, Object service) throws Exception {
        OnRegister ann = service.getClass().getAnnotation(OnRegister.class);
        if (ann == null) return;
        RegistrationHandler handler = (RegistrationHandler) ann.value().newInstance();
        handler.beforeUnregister(id, service, this);
    }

    @Override
    public synchronized boolean addLock(String id, UnlockCb unlockCb) throws Exception {
        watchedServices.put(id, unlockCb);
        return true;
    }

    @Override
    public synchronized void removeLock(String id) throws Exception {
        watchedServices.remove(id);
    }

    synchronized void unlock(String path) throws Exception {
        if (watchedServices.containsKey(path))
            watchedServices.get(path).unlocked(path);
    }

    public void doStart() throws Exception {
        for (String id : registeredServices.keySet()) {
            activateService(id);
        }
    }

    public void doStop() throws Exception {
        for (String id : registeredServices.keySet()) {
            deactivateService(id);
        }
    }

    @Override
    public final synchronized void start() throws Exception {
        doStart();
        running = true;
    }

    @Override
    public final synchronized void stop() throws Exception {
        doStop();
        running = false;
    }
}
