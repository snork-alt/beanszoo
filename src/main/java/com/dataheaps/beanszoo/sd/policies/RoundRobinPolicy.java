package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.policies.Policy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 24/1/17.
 */
public class RoundRobinPolicy implements Policy {

    @Override
    public Object getServiceInstance(Class klass, String name, List<ServiceDescriptor> d, RpcClient client, ServiceDirectory services) {

        return Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class<?>[]{klass}, new InvocationHandler() {

            int index = 0;

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                int ctr = 0;
                while (true) {

                    try {

                        List<ServiceDescriptor> sdl = new ArrayList<ServiceDescriptor>(services.getServicesByType(klass, name));
                        if (sdl.isEmpty())
                            throw new IllegalArgumentException("Object not found");

                        if (index >= sdl.size())
                            index = 0;

                        return client.invoke(sdl.get(index), method.getName(), method.getParameterTypes(), args);
                    }
                    catch(RpcStatusException e) {
                        if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 5)
                            throw e;
                        ctr++;
                    }
                    catch (Exception e) {
                        throw e;
                    }
                    finally {
                        index++;
                    }

                }
            }
        });


    }
}
