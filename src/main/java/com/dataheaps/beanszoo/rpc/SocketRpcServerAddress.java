package com.dataheaps.beanszoo.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by admin on 29/5/16.
 */

@RequiredArgsConstructor
public class SocketRpcServerAddress implements RpcServerAddress{

    @Getter final String hostname;
    @Getter final int port;

    @Override
    public String geAddressString() {
        return String.format("%s:%d", hostname, port);
    }

}
