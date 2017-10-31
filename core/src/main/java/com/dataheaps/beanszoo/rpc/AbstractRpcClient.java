package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matteopelati on 28/10/15.
 */

@RequiredArgsConstructor
public abstract class AbstractRpcClient<S> implements RpcClient {

    static class RpcSync {
        Integer status = null;
        String statusText = null;
        Object returnValue = null;
        Semaphore event = new Semaphore(0);
    }

    final RPCRequestCodec codec;
    final int timeout;
    Map<Long,RpcSync> events = new ConcurrentHashMap<>();
    AtomicLong idGen = new AtomicLong(0);


    abstract S getSession(String address) throws IOException;
    abstract void sendMessage(S session, byte[] payload) throws IOException;

    void handleResponse(byte[] payload) {

        RpcMessage msg = null;

        try {

            msg = (RpcMessage) codec.deserialize(payload);

            RpcSync e = events.get(msg.id);
            if (e != null) {
                e.status = msg.status;
                e.statusText = msg.statusText;
                e.returnValue = msg.returnValue;
                e.event.release();
            }
        }
        catch (Exception ex) {

            if (msg != null) {
                RpcSync e = events.get(msg.id);
                if (e != null) {
                    e.status = RpcConstants.STATUS_CLIENT_EXCEPTION;
                    e.statusText = ex.getMessage();
                    e.event.release();
                }
            }
        }
    }

    @Override
    public synchronized Object invoke(ServiceDescriptor d, String method, Class<?>[] argTypes, Object[] args) throws Exception {

        long messageId = idGen.incrementAndGet();
        RpcSync sync = new RpcSync();
        events.put(messageId, sync);

        S session = getSession(d.getAddress());
        RpcMessage msg = new RpcMessage(
                messageId, d, method,
                argTypes,
                (args == null) ? new ArrayList<>() : Arrays.asList(args)
        );
        sendMessage(session, codec.serialize(msg));

        if (!sync.event.tryAcquire(timeout, TimeUnit.MILLISECONDS))
            throw new TimeoutException();

        RpcSync e = events.remove(messageId);
        if (e.status == RpcConstants.STATUS_OK)
            return e.returnValue;
        else throw new RpcStatusException(e.status, e.statusText);

    }


}

