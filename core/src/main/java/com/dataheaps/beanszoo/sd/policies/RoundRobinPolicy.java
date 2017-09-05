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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by admin on 24/1/17.
 */
public class RoundRobinPolicy implements Policy {

    AtomicInteger index = new AtomicInteger(0);

    @Override
    public Object invoke(Object proxy, Method method, Object[] args, Class klass, String name, List<ServiceDescriptor> d, RpcClient rpcClient, ServiceDirectory services) throws Throwable {

        int ctr = 0;
        while (true) {

            try {

                List<ServiceDescriptor> sdl = new ArrayList<ServiceDescriptor>(services.getServicesByType(klass, name));
                if (sdl.isEmpty())
                    throw new IllegalArgumentException("Object not found");

                if (index.get() >= sdl.size())
                    index.set(0);

                return rpcClient.invoke(sdl.get(index.get()), method.getName(), method.getParameterTypes(), args);
            } catch (RpcStatusException e) {
                if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 5)
                    throw e;
                ctr++;
            } catch (Exception e) {
                throw e;
            } finally {
                index.incrementAndGet();
            }
        }
    }

}
