package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by matteopelati on 26/10/15.
 */

@RequiredArgsConstructor
public class Services {

    final RpcClient rpcClient;
    final ServiceDirectory services;

    Object createServiceProxy(final String id, final Class type, String name) {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{type}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                int ctr = 0;
                while (true) {

                    try {
                        RemoteServiceDirectory.RemoteService rs = id != null ?
                                ((RemoteServiceDirectory) services).getRemoteService(id, type) :
                                ((RemoteServiceDirectory) services).getRemoteServiceByType(type, name);

                        if (rs == null)
                            throw new IllegalArgumentException("Object note found");

                        return rpcClient.invoke(rs.getAddress(), rs.getId(), method.getName(), args);
                    }
                    catch(RpcStatusException e) {
                        if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 3)
                            throw e;
                        ctr++;
                    }
                    catch (Exception e) {
                        throw e;
                    }

                }
            }
        });
    }

    Object createTargetedServiceProxy(final RemoteServiceDirectory.RemoteService rs) {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{rs.getType()}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                int ctr = 0;
                while (true) {

                    try {
                        return rpcClient.invoke(rs.getAddress(), rs.getId(), method.getName(), args);
                    }
                    catch(RpcStatusException e) {
                        if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 3)
                            throw e;
                        ctr++;
                    }
                    catch (Exception e) {
                        throw e;
                    }

                }
            }
        });
    }

    public Object getServiceByType(Class type, String name) {
        Object service = services.getServiceByType(type, name);
        if (service != null || (service == null && (!(services instanceof RemoteServiceDirectory))))
            return service;
        return createServiceProxy(null, type, name);
    }

    public Object getServiceById(String id, Class type) {
        Object service = services.getService(id);
        if (service != null || (service == null && (!(services instanceof RemoteServiceDirectory))))
            return service;
        return createServiceProxy(id, type, null);
    }

    public Set getServicesByType(Class type) {

        Set allServices = new HashSet<>();

        Set local = services.getServicesByType(type);
        if (local != null) allServices.addAll(local);

        if (services instanceof RemoteServiceDirectory) {
            Set<RemoteServiceDirectory.RemoteService> remote = ((RemoteServiceDirectory) services).getRemoteServicesByType(type);
            if (remote != null)
                for (RemoteServiceDirectory.RemoteService s : remote)
                    allServices.add(createTargetedServiceProxy(s));
        }

        return allServices;
    }

}
