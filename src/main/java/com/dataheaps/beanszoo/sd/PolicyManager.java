package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcClient;

import java.util.List;

/**
 * Created by admin on 24/1/17.
 */
public interface PolicyManager {

    Object getServiceInstance(Class klass, String name, List<ServiceDescriptor> d, RpcClient client, ServiceDirectory services);

}
