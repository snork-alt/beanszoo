package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;

/**
 * Created by admin on 9/2/17.
 */
public interface ServiceDirectoryFactory {
    ServiceDirectory create(RpcServerAddress address) throws Exception;
}
