package com.dataheaps.beanszoo.rpc;

import java.io.Serializable;
import java.util.List;

import com.dataheaps.beanszoo.sd.ServiceDescriptor;

/**
 * Created by admin on 24/1/17.
 */

public class RpcMessage implements Serializable {

	private static final long serialVersionUID = 610414194024218274L;

	public enum Type {
        Request, Response
    }

    Type type;
    long id;
    ServiceDescriptor service;
    String method;
    Class<?>[] argTypes;
    List<Object> args;
    Object returnValue;
    int status;
    String statusText;

    public RpcMessage(long id, ServiceDescriptor service, String method, Class<?>[] argTypes, List<Object> args) {
        this.type = Type.Request;
        this.id = id;
        this.service = service;
        this.method = method;
        this.args = args;
        this.argTypes = argTypes;
    }

    public RpcMessage(long id, ServiceDescriptor service, String method, Object returnValue, int status, String statusText) {
        this.type = Type.Response;
        this.id = id;
        this.service = service;
        this.method = method;
        this.returnValue = returnValue;
        this.status = status;
        this.statusText = statusText;
    }
}
