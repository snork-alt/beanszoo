package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Created by admin on 17/9/17.
 */
public class LocalRpcClient implements RpcClient {

    @Override
    public Object invoke(ServiceDescriptor d, String method, Class[] argTypes, Object[] args) throws Exception {
        throw new NotImplementedException();
    }
}
