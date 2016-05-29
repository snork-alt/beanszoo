package com.dataheaps.beanszoo.rpc;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Created by matteopelati on 28/10/15.
 */

@RequiredArgsConstructor
public class WsRpcServerAddress implements RpcServerAddress{

    @Getter final String hostname;
    @Getter final int port;

    @Override
    public String geAddressString() {
        return String.format("ws://%s:%d", hostname, port);
    }

}
