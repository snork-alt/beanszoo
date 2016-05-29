package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;

/**
 * Created by matteopelati on 24/10/15.
 */
public interface RpcRequestHandler {

    byte[] handleRPCRequest(RPCRequestCodec codec, byte[] data, String objectPath, String method) throws Exception;
}
