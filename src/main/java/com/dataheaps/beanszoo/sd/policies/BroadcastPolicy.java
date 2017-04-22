package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 22/4/17.
 */
public class BroadcastPolicy implements Policy {


    @Override
    public Object invoke(Object proxy, Method method, Object[] args, Class klass, String name, List<ServiceDescriptor> d, RpcClient rpcClient, ServiceDirectory services) throws Throwable {

        List<ServiceDescriptor> sdl = new ArrayList<ServiceDescriptor>(services.getServicesByType(klass, name));
        for (ServiceDescriptor sd: sdl) {
            try {
                rpcClient.invoke(sd, method.getName(), method.getParameterTypes(), args);
            }
            catch (Throwable e) {

            }
        }
        return null;

    }
}
