package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by admin on 24/1/17.
 */
public class RemoteInstancePolicyManager implements PolicyManager {

    @Override
    public Object getServiceInstance(Class klass, String name, List<ServiceDescriptor> d, RpcClient rpcClient, ServiceDirectory services) {
        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{klass}, (proxy, method, args) -> {

            int ctr = 0;
            while (true) {

                try {
                    return rpcClient.invoke(d.get(0), method.getName(), args);
                }
                catch(RpcStatusException e) {
                    if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 5)
                        throw e;
                    ctr++;
                }
                catch (Exception e) {
                    throw e;
                }

            }
        });
    }
}
