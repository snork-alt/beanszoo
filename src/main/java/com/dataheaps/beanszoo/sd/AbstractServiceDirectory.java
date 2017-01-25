package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.dataheaps.beanszoo.sd.policies.LocalRandomPolicy;
import com.dataheaps.beanszoo.utils.Multimap;
import org.apache.commons.lang3.ClassUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matteopelati on 25/10/15.
 */


public abstract class AbstractServiceDirectory implements ServiceDirectory {

    static final Class DefaultPolicy = LocalRandomPolicy.class;

    final RpcServerAddress localAddress;

    Map<String, Object> localIds = new HashMap<>();
    Map<Object, String> localIdsInverted = new HashMap<>();
    Multimap<String, Object> localInterfaces = new Multimap<>();

    Map<String, ServiceDescriptor> allIds = new ConcurrentHashMap<>();
    Multimap<String, ServiceDescriptor> allInterfaces = new Multimap<>();

    boolean running = false;

    AbstractServiceDirectory(RpcServerAddress localAddress) {
        this.localAddress = localAddress;
    }

    public Object getLocalInstance(ServiceDescriptor d) {

        if (!d.getAddress().equals(localAddress.geAddressString()))
            return null;

        if (d.getId() != null)
            return localIds.get(d.getId());
        else {
            Set<Object> ls = localInterfaces.get(d.getPath());
            if (ls.isEmpty()) return null;
            return new ArrayList<Object>(ls).get(0);
        }
    }

    ServiceDescriptor getIdLocalServiceDescriptor(String id) {
        return new ServiceDescriptor(localAddress.geAddressString(), id, null, null, id, null);
    }

    Map<String, ServiceDescriptor> getInterfaceLocalServiceDescriptors(Object service, String id) {

        Map<String, ServiceDescriptor> serviceDescriptors = new HashMap<>();
        Name name = service.getClass().getAnnotation(Name.class);
        InvocationPolicy policy = service.getClass().getAnnotation(InvocationPolicy.class);
        ClassUtils.getAllInterfaces(service.getClass()).forEach(
                c -> {
                    serviceDescriptors.put(
                            c.getCanonicalName(),
                            new ServiceDescriptor(
                                    localAddress.geAddressString(), null, c, null, c.getCanonicalName(),
                                    policy == null ? DefaultPolicy : policy.value()
                            )
                    );
                    if (name != null) {
                        serviceDescriptors.put(
                            c.getCanonicalName() + "!" + name.value(),
                            new ServiceDescriptor(
                                    localAddress.geAddressString(), null, c, name.value(),
                                    c.getCanonicalName() + "!" + name.value(),
                                    policy == null ? DefaultPolicy : policy.value()
                            )
                        );
                    }
                }
        );
        return serviceDescriptors;

    }


    private void registerLocalInstance(String id, Object service) {

        localIds.put(id, service);
        localIdsInverted.put(service, id);

        Map<String, ServiceDescriptor> ifaces = getInterfaceLocalServiceDescriptors(service, id);
        for (String i: ifaces.keySet())
            localInterfaces.put(i, service);

        allIds.put(id, getIdLocalServiceDescriptor(id));
        allInterfaces.putAll(ifaces);

    }

    private boolean unregisterLocalInstance(String id, Object service) {

        if (id != null)
            service = localIds.get(id);
        if (service != null)
            id = localIdsInverted.get(service);

        if (id == null || service == null)
            return false;

        localIds.remove(id);
        localIdsInverted.remove(service);

        Map<String, ServiceDescriptor> ifaces = getInterfaceLocalServiceDescriptors(service, id);
        for (String i: ifaces.keySet())
            localInterfaces.remove(i, service);

        allIds.remove(id);
        allInterfaces.removeAll(ifaces);
        return true;
    }


    @Override
    public synchronized ServiceDescriptor getService(String id) {
        return allIds.get(id);
    }

    @Override
    public synchronized Set<ServiceDescriptor> getServicesByType(Class type, String name) {
        if (!running) return Collections.EMPTY_SET;
        Set<ServiceDescriptor> ret = allInterfaces.get(type.getCanonicalName() + ((name == null) ? "" : ("!" + name)));
        if (ret == null || ret.size() == 0) return Collections.EMPTY_SET;
        return ret;
    }

    @Override
    public synchronized void putService(Object service) {
        putService(UUID.randomUUID().toString(), service);
    }

    @Override
    public synchronized void putService(String id, Object service) {
        registerLocalInstance(id, service);
    }

    @Override
    public synchronized boolean removeService(Object service) {
        return unregisterLocalInstance(null, service);
    }

    @Override
    public synchronized boolean removeService(String id) {
        return unregisterLocalInstance(id, null);
    }


    @Override
    public synchronized void start() throws Exception {
        running = true;
    }

    @Override
    public synchronized void stop() throws Exception {
        running = false;
    }
}
