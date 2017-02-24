package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.sd.policies.LocalRandomPolicy;
import com.dataheaps.beanszoo.sd.policies.Policy;
import com.dataheaps.beanszoo.sd.policies.InstanceIdPolicy;
import com.dataheaps.beanszoo.sd.policies.RoundRobinPolicy;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by matteopelati on 26/10/15.
 */

public class Services {

    static final InstanceIdPolicy remoteInstancePolicyManager = new InstanceIdPolicy();

    final RpcClient rpcClient;
    final ServiceDirectory services;

    public Services(RpcClient rpcClient, ServiceDirectory services) {
        this.rpcClient = rpcClient;
        this.services = services;
    }

    public ServiceDirectory getServiceDirectory() {
        return services;
    }

    public <T> T getService(String id, Class<T> klass) {

        ServiceDescriptor d = services.getService(id);
        if (d == null) return null;

        T instance = (T) services.getLocalInstance(d);
        if (instance != null) return instance;

        return (T) remoteInstancePolicyManager.getServiceInstance(
                klass, null, Arrays.asList(new ServiceDescriptor[] {d}),
                rpcClient, services
        );

    }

    Policy getServicePolicy(Set<ServiceDescriptor> d)  {

        try {
            Class policy = null;
            ArrayList<ServiceDescriptor> dList = new ArrayList<>(d);
            for (ServiceDescriptor sd : dList) {
                if (policy == null) policy = sd.getPolicy();
                else if (!policy.equals(sd.getPolicy()))
                    throw new IllegalArgumentException("The same service specifies different policies on different servers");
            }

            return (Policy) policy.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getService(Class<T> klass) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, null);
        if (d.isEmpty()) return null;

        Policy pm = getServicePolicy(d);
        return (T) pm.getServiceInstance(
                klass, null, new ArrayList<>(d), rpcClient, services
        );

    }

    public <T> T getService(Class<T> klass, String name) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, name);
        if (d.isEmpty()) return null;

        Policy pm = getServicePolicy(d);
        return (T) pm.getServiceInstance(
                klass, null, new ArrayList<>(d), rpcClient, services
        );

    }

    public Set<Object> getServicesMetadata(Class<?> klass, String name) {
        Set<ServiceDescriptor> d = services.getServicesByType(klass, name);
        if (d.isEmpty()) return Collections.EMPTY_SET;
        return d.stream().flatMap(
                e -> (e.getMetadata() == null) ? Stream.empty() : Stream.of(e.getMetadata())).collect(Collectors.toSet()
        );
    }

    public Set<Object> getServicesMetadata(Class<?> klass) {
        return getServicesMetadata(klass, null);
    }

    public <T> T getServiceMetadata(String id, Class<T> metadataType) {

        ServiceDescriptor d = services.getService(id);
        if (d == null) return null;
        return (T) d.getMetadata();

    }

}
