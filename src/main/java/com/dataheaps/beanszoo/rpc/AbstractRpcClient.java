package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
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

        Long messageId = null;

        try {

            Object[] decoded = StreamUtils.fromByteArray(
                    new Object[]{Long.class, Integer.class, String.class, new byte[0]},
                    payload
            );
            messageId = (Long) decoded[0];

            RpcSync e = events.get(messageId);
            if (e != null) {
                e.status = (Integer) decoded[1];
                e.statusText = (String) decoded[2];
                e.returnValue = codec.deserialize((byte[])decoded[3]);
                e.event.release();
            }
        }
        catch (Exception ex) {

            RpcSync e = events.get(messageId);
            if (messageId != null && e != null) {
                e.status = RpcConstants.STATUS_CLIENT_EXCEPTION;
                e.statusText = ex.getMessage();
                e.event.release();
            }
        }
    }

    @Override
    public synchronized Object invoke(String address, String id, String method, Object[] args) throws Exception {


        long messageId = idGen.incrementAndGet();
        RpcSync sync = new RpcSync();
        events.put(messageId, sync);

        S session = getSession(address);

        byte[] buffer = StreamUtils.toByteArray(new Object[]{
                messageId, codec.getContentType(), id, method, codec.serialize(args)
        });
        sendMessage(session, buffer);

        if (!sync.event.tryAcquire(timeout, TimeUnit.MILLISECONDS))
            throw new TimeoutException();

        RpcSync e = events.remove(messageId);
        if (e.status == RpcConstants.STATUS_OK)
            return e.returnValue;
        else throw new RpcStatusException(e.status, e.statusText);

    }


}

