package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.sd.ServiceDirectory;

/**
 * Created by admin on 9/2/17.
 */
public interface RpcFactory<T extends RpcServerAddress> {
    RpcServer createServer(T address, ServiceDirectory sd) throws Exception;
    RpcClient createClient() throws Exception;
    RpcServerAddress createAddress() throws Exception;
}
