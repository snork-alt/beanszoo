package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;

import java.util.*;

/**
 * Created by matteopelati on 26/10/15.
 */

public class Services {

    static final RemoteInstancePolicyManager remoteInstancePolicyManager = new RemoteInstancePolicyManager();

    final RpcClient rpcClient;
    final ServiceDirectory services;
    final Map<String, PolicyManager> policies;

    public Services(RpcClient rpcClient, ServiceDirectory services, Map<String, PolicyManager> policies) {
        this.rpcClient = rpcClient;
        this.services = services;
        this.policies = createPolicies(policies);
    }

    public Services(RpcClient rpcClient, ServiceDirectory services) {
        this.rpcClient = rpcClient;
        this.services = services;
        this.policies = createPolicies(null);
    }

    Map<String, PolicyManager> createPolicies(Map<String, PolicyManager> userPolicies) {

        Map<String, PolicyManager> policies = new HashMap<>();
        policies.put(StandardPolicies.LocalElseRandom, new LocalRandomPolicyManager());
        policies.put(StandardPolicies.RoundRobin, new RoundRobinPolicyManager());
        if (userPolicies != null)
            policies.putAll(userPolicies);
        return policies;
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

    PolicyManager getServicePolicy(Set<ServiceDescriptor> d) {

        String policy = null;
        ArrayList<ServiceDescriptor> dList = new ArrayList<>(d);
        for (ServiceDescriptor sd: dList) {
            if (policy == null) policy = sd.getPolicy();
            else if (!policy.equals(sd.getPolicy()))
                throw new IllegalArgumentException("The same service specifies different policies on different servers");
        }

        PolicyManager pm = policies.get(policy);
        if (pm == null)
            throw new IllegalArgumentException("Unable to handle policy " + policy);
        return pm;

    }

    public <T> T getService(Class<T> klass) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, null);
        if (d.isEmpty()) return null;

        PolicyManager pm = getServicePolicy(d);
        return (T) pm.getServiceInstance(
                klass, null, new ArrayList<>(d), rpcClient, services
        );

    }



    //
//    Object createServiceProxy(final String id, final Class type, String name) {
//        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{type}, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//
//                int ctr = 0;
//                while (true) {
//
//                    try {
//                        ServiceDescriptor rs = id != null ?
//                                ((RemoteServiceDirectory) services).getRemoteService(id, type) :
//                                ((RemoteServiceDirectory) services).getRemoteServiceByType(type, name);
//
//                        if (rs == null)
//                            throw new IllegalArgumentException("Object note found");
//
//                        return rpcClient.invoke(rs.getAddress(), rs.getId(), method.getName(), args);
//                    }
//                    catch(RpcStatusException e) {
//                        if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 3)
//                            throw e;
//                        ctr++;
//                    }
//                    catch (Exception e) {
//                        throw e;
//                    }
//
//                }
//            }
//        });
//    }
//
//    Object createTargetedServiceProxy(final ServiceDescriptor rs) {
//        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{rs.getType()}, new InvocationHandler() {
//            @Override
//            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
//
//                int ctr = 0;
//                while (true) {
//
//                    try {
//                        return rpcClient.invoke(rs.getAddress(), rs.getId(), method.getName(), args);
//                    }
//                    catch(RpcStatusException e) {
//                        if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 3)
//                            throw e;
//                        ctr++;
//                    }
//                    catch (Exception e) {
//                        throw e;
//                    }
//
//                }
//            }
//        });
//    }
//
//    public Object getServiceByType(Class type, String name) {
//        Object service = services.getServiceByType(type, name);
//        if (service != null || (service == null && (!(services instanceof RemoteServiceDirectory))))
//            return service;
//        return createServiceProxy(null, type, name);
//    }
//
//    public Object getServiceById(String id, Class type) {
//        Object service = services.getService(id);
//        if (service != null || (service == null && (!(services instanceof RemoteServiceDirectory))))
//            return service;
//        return createServiceProxy(id, type, null);
//    }
//
//    public Set getServicesByType(Class type) {
//
//        Set allServices = new HashSet<>();
//
//        Set local = services.getServicesByType(type);
//        if (local != null) allServices.addAll(local);
//
//        if (services instanceof RemoteServiceDirectory) {
//            Set<ServiceDescriptor> remote = ((RemoteServiceDirectory) services).getRemoteServicesByType(type);
//            if (remote != null)
//                for (ServiceDescriptor s : remote)
//                    allServices.add(createTargetedServiceProxy(s));
//        }
//
//        return allServices;
//    }

}
