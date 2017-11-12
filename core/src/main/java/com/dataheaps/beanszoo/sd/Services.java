package com.dataheaps.beanszoo.sd;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.dataheaps.beanszoo.sd.policies.LocalRandomPolicy;
import com.dataheaps.beanszoo.sd.policies.Policy;
import com.dataheaps.beanszoo.sd.policies.SingleInstancePolicy;

/**
 * Created by matteopelati on 26/10/15.
 */

/**
 *  Class used to register, unregister and query services
 */
public class Services {

    static final SingleInstancePolicy singleInstancePolicy = new SingleInstancePolicy();

    final RpcClient rpcClient;
    final ServiceDirectory services;

    /**
     * Initialises a new service manager that can be used for service registration and querying
     *
     * @param rpcClient An implementation of an RPC client that will be used when invoking remote services
     * @param services A service directory implementation which keeps track of the registered services (local and remote)
     */
    public Services(RpcClient rpcClient, ServiceDirectory services) {
        this.rpcClient = rpcClient;
        this.services = services;
    }

    /**
     * Returns teh underlying service directory used by the service manager
     * @return the service directory implementation
     */
    public ServiceDirectory getServiceDirectory() {
        return services;
    }

    /**
     * Obtains a reference to a service implementation (local or remote)
     *
     * @param id The unique identifier of teh service
     * @param klass The interface implemented by teh requested service
     * @return Reference to the service implementation, which can me local or remote
     */
    @SuppressWarnings("unchecked")
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

    Object getServiceInstance(Class<?> klass, String name, List<ServiceDescriptor> d, RpcClient client, ServiceDirectory services)  {

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

    /**
     * Obtains a reference to a service implementation (local or remote)
     *
     * @param klass The interface implemented by teh requested service
     * @return Reference to the service implementation, which can me local or remote
     */
    @SuppressWarnings("unchecked")
	public <T> T getService(Class<T> klass) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, null);
        if (d.isEmpty()) return null;
        return (T) getServiceInstance(klass, null, new ArrayList<>(d), rpcClient, services);

    }

    /**
     * Obtains a reference to a service implementation (local or remote)
     *
     * @param klass The interface implemented by teh requested service
     * @param name The name of the service. Services can be named using the Name annotation
     * @return Reference to the service implementation, which can me local or remote
     */
    @SuppressWarnings("unchecked")
	public <T> T getService(Class<T> klass, String name) {

        Set<ServiceDescriptor> d = services.getServicesByType(klass, name);
        if (d.isEmpty()) return null;
        return (T) getServiceInstance(klass, null, new ArrayList<>(d), rpcClient, services);

    }

    public Set<Object> getServicesMetadata(Class<?> klass, String name) {
        Set<ServiceDescriptor> d = services.getServicesByType(klass, name);
        if (d.isEmpty()) return Collections.emptySet();
        return d.stream().flatMap(
                e -> (e.getMetadata() == null) ? Stream.empty() : Stream.of(e.getMetadata())).collect(Collectors.toSet()
        );
    }

    public Set<Object> getServicesMetadata(Class<?> klass) {
        return getServicesMetadata(klass, null);
    }

    @SuppressWarnings("unchecked")
	public <T> T getServiceMetadata(String id, Class<T> metadataType) {

        ServiceDescriptor d = services.getService(id);
        if (d == null) return null;
        return (T) d.getMetadata();

    }

}
