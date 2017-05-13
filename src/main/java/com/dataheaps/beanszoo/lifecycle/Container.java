package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.Services;
import lombok.AllArgsConstructor;

/**
 * Created by admin on 9/2/17.
 */

@AllArgsConstructor
public class Container {
    ServiceDirectory sd;
    RpcClient rpcClient;
    RpcServer rpcServer;
    RpcServerAddress rpcServerAddress;
    Services services;
}
