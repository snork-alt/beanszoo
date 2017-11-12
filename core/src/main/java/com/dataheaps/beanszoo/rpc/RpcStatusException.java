package com.dataheaps.beanszoo.rpc;

import lombok.Getter;

/**
 * Created by matteopelati on 29/10/15.
 */
public class RpcStatusException extends Exception {

	private static final long serialVersionUID = -4178362319146863063L;

	@Getter final int statusCode;

    public RpcStatusException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }
}
