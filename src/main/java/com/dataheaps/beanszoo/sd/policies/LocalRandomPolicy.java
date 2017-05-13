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
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by admin on 24/1/17.
 */
public class LocalRandomPolicy implements Policy {

    AtomicReference localInstance = new AtomicReference();
    Random rand = new Random();

    @Override
    public Object invoke(Object proxy, Method method, Object[] args, Class klass, String name, List<ServiceDescriptor> d, RpcClient client, ServiceDirectory services) throws Throwable {

        if (localInstance.get() == null) {
            for (ServiceDescriptor sd : d) {
                Object i = services.getLocalInstance(sd);
                if (i != null) localInstance.set(i);
            }
        }

        Object target = localInstance.get();
        if (target != null)
            return method.invoke(target, args);

        int ctr = 0;
        while (true) {

            try {

                List<ServiceDescriptor> sdl = new ArrayList<ServiceDescriptor>(services.getServicesByType(klass, name));
                if (sdl.isEmpty())
                    throw new IllegalArgumentException("Object not found");

                return client.invoke(sdl.get(Math.abs(rand.nextInt()) % sdl.size()), method.getName(), method.getParameterTypes(), args);
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

    }

}
