package com.dataheaps.beanszoo.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;

/**
 * Created by matteopelati on 28/10/15.
 */

public abstract class AbstractRpcServer<E> implements RpcServer {

    static final Logger logger = LoggerFactory.getLogger(AbstractRpcServer.class);

    final RPCRequestCodec codec;
    final RpcServerAddress bindings;
    final ServiceDirectory sd;
    ExecutorService executors = Executors.newCachedThreadPool();

    public AbstractRpcServer(RpcServerAddress bindings, RPCRequestCodec codec, ServiceDirectory sd) {
        this.codec = codec;
        this.sd = sd;
        this.bindings = bindings;
    }

    synchronized Object handleRpcRequest(ServiceDescriptor d, String method, Class[] argTypes, List<Object> args)
            throws RpcStatusException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {

        Object service = sd.getLocalInstance(d);
        if (service == null)
            throw new RpcStatusException(RpcConstants.STATUS_SERVER_EXCEPTION, d.getPath());

        Method m = service.getClass().getMethod(method, argTypes);
        if (m == null)
            throw new IllegalArgumentException("No method " + method);

        m.setAccessible(true);
        return m.invoke(service, args.toArray());
    }

    public void handleRequest(E endpoint, byte[] payload) {

        executors.submit(() -> {

            RpcMessage msg = null;

            try {

                msg = (RpcMessage) codec.deserialize(payload);

                try {

                    Object result = handleRpcRequest(msg.service, msg.method, msg.argTypes, msg.args);
                    RpcMessage retMsg = new RpcMessage(
                            msg.id, msg.service, msg.method, result,
                            RpcConstants.STATUS_OK, null
                    );
                    sendMessage(endpoint, codec.serialize(retMsg));

                }
                catch(RpcStatusException e) {

                    RpcMessage retMsg = new RpcMessage(
                            msg.id, msg.service, msg.method, null, e.statusCode, e.getMessage()
                    );
                    sendMessage(endpoint, codec.serialize(retMsg));
                }
                catch(InvocationTargetException e) {

                    RpcMessage retMsg = new RpcMessage(
                            msg.id, msg.service, msg.method, null, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getTargetException().getMessage()
                    );
                    sendMessage(endpoint, codec.serialize(retMsg));
                }
                catch(Exception e) {

                    RpcMessage retMsg = new RpcMessage(
                            msg.id, msg.service, msg.method, null, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getMessage()
                    );
                    sendMessage(endpoint, codec.serialize(retMsg));
                }

            }
            catch (Exception e) {

                try {

                    if (msg != null) {
                        RpcMessage retMsg = new RpcMessage(
                                msg.id, msg.service, msg.method, null, RpcConstants.STATUS_SERVER_EXCEPTION, e.getMessage()
                        );
                        sendMessage(endpoint, codec.serialize(retMsg));
                    }
                }
                catch (Exception ex) {

                }
            }
        });

    }


    abstract void sendMessage(E endpoint, byte[] buffer) throws Exception;


}
