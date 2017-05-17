package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.policies.Policy;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Created by admin on 24/1/17.
 */
public class SingleInstancePolicy implements Policy {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args, Class klass, String name, List<ServiceDescriptor> d, RpcClient rpcClient, ServiceDirectory services) throws Throwable {

        int ctr = 0;
        while (true) {

            try {
                return rpcClient.invoke(d.get(0), method.getName(), method.getParameterTypes(), args);
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
