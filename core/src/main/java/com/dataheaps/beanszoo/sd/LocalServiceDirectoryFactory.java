package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;

/**
 * Created by admin on 17/9/17.
 */
public class LocalServiceDirectoryFactory implements ServiceDirectoryFactory {

    @Override
    public ServiceDirectory create(RpcServerAddress address) throws Exception {
        return new LocalServiceDirectory();
    }
}
