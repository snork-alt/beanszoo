package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.sd.policies.*;
import com.sun.javafx.collections.ImmutableObservableList;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by matteopelati on 26/10/15.
 */

public class Services {

    static final SingleInstancePolicy singleInstancePolicy = new SingleInstancePolicy();

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

        List<ServiceDescriptor> sdl = new ArrayList<>();
        sdl.add(d);

        return (T) Proxy.newProxyInstance(
                this.getClass().getClassLoader(), new Class[]{klass},
                (proxy, method, args) -> singleInstancePolicy.invoke(proxy, method, args, klass, null, sdl, rpcClient, services)
        );

    }

    Object getServiceInstance(Class klass, String name, List<ServiceDescriptor> d, RpcClient client, ServiceDirectory services)  {

        return Proxy.newProxyInstance(
                this.getClass().getClassLoader(), new Class<?>[]{klass},
                new InvocationHandler() {

                    ConcurrentHashMap<Method, Policy> policies = new ConcurrentHashMap<>();

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                        Policy policy = policies.computeIfAbsent(method, (k) -> {
                            try {
                                InvocationPolicy ann = k.getAnnotation(InvocationPolicy.class);
                                return ann == null ? new LocalRandomPolicy() : (Policy) ann.value().newInstance();
                            }
                            catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                        return policy.invoke(proxy, method, args, klass, name, d, client, services);

                    }
                }
        );
    }


    public <T> T getService(Class<T> klass) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, null);
        if (d.isEmpty()) return null;
        return (T) getServiceInstance(klass, null, new ArrayList<>(d), rpcClient, services);

    }

    public <T> T getService(Class<T> klass, String name) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, name);
        if (d.isEmpty()) return null;
        return (T) getServiceInstance(klass, null, new ArrayList<>(d), rpcClient, services);

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
