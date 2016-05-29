package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;

import java.net.SocketAddress;
import java.util.Map;

/**
 * Created by matteopelati on 24/10/15.
 */
public interface RpcServer {

    void start() throws Exception;
    void stop() throws Exception;

}
