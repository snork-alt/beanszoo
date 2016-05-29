package com.dataheaps.beanszoo.rpc;

/**
 * Created by matteopelati on 26/10/15.
 */
public interface RpcClient {

    Object invoke(String address, String id, String method, Object[] args) throws Exception;
}
