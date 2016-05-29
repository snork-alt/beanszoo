package com.dataheaps.beanszoo.rpc;

import lombok.Getter;

/**
 * Created by matteopelati on 29/10/15.
 */
public class RpcStatusException extends Exception {

    @Getter final int statusCode;

    public RpcStatusException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
