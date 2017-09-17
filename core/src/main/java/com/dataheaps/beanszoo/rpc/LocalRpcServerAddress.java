package com.dataheaps.beanszoo.rpc;

/**
 * Created by admin on 17/9/17.
 */
public class LocalRpcServerAddress implements RpcServerAddress {

    @Override
    public String geAddressString() {
        return "local";
    }
}
