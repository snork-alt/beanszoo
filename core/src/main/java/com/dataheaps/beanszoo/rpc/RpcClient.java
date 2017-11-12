package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.sd.ServiceDescriptor;

/**
 * Created by matteopelati on 26/10/15.
 */
public interface RpcClient {

    Object invoke(ServiceDescriptor d, String method, Class<?>[] argTypes, Object[] args) throws Exception;
}
