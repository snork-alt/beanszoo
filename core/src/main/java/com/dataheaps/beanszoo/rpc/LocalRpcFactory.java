package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.sd.ServiceDirectory;

/**
 * Created by admin on 17/9/17.
 */
public class LocalRpcFactory implements RpcFactory<LocalRpcServerAddress> {

    @Override
    public RpcServer createServer(LocalRpcServerAddress address, ServiceDirectory sd) throws Exception {
        return new LocalRpcServer();
    }

    @Override
    public RpcClient createClient() throws Exception {
        return new LocalRpcClient();
    }

    @Override
    public RpcServerAddress createAddress() throws Exception {
        return new LocalRpcServerAddress();
    }
}
