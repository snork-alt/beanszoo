package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by admin on 24/1/17.
 */
public interface Policy {

    Object invoke(
            Object proxy, Method method, Object[] args,
            Class klass, String name,
            List<ServiceDescriptor> d,
            RpcClient rpcClient,
            ServiceDirectory services
    ) throws Throwable;
}
