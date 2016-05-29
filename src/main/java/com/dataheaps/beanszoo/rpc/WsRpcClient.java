package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by matteopelati on 28/10/15.
 */

@RequiredArgsConstructor
public class WsRpcClient implements RpcClient {

    static class RpcSync {
        Integer status = null;
        String statusText = null;
        Object returnValue = null;
        Semaphore event = new Semaphore(0);
    }

    final RPCRequestCodec codec;
    final int timeout;
    Map<String, Session> sessions = new ConcurrentHashMap<>();
    Map<Long,RpcSync> events = new ConcurrentHashMap<>();
    AtomicLong idGen = new AtomicLong(0);

    synchronized Session getSession(final String address) throws Exception {

        Session session;

        session = sessions.get(address);
        if (session != null) return session;

        WebSocketAdapter ws = new WebSocketAdapter() {

            @Override
            public void onWebSocketClose(int statusCode, String reason) {
                super.onWebSocketClose(statusCode, reason);
                sessions.remove(address);
            }

            @Override
            public void onWebSocketError(Throwable cause) {
                super.onWebSocketError(cause);
                sessions.remove(address);
            }

            @Override
            public void onWebSocketBinary(byte[] payload, int offset, int len) {
                super.onWebSocketBinary(payload, offset, len);

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
        };

        WebSocketClient client = new WebSocketClient();
        client.start();
        Future<Session> fut = client.connect(ws, new URI(address));
        session = fut.get();
        sessions.put(address, session);
        return session;
    }



    @Override
    public synchronized Object invoke(String address, String id, String method, Object[] args) throws Exception {


        long messageId = idGen.incrementAndGet();
        RpcSync sync = new RpcSync();
        events.put(messageId, sync);

        Session session = getSession(address + "/events");

        byte[] buffer = StreamUtils.toByteArray(new Object[]{
                messageId, codec.getContentType(), id, method, codec.serialize(args)
        });
        session.getRemote().sendBytes(ByteBuffer.wrap(buffer));

        if (!sync.event.tryAcquire(timeout, TimeUnit.MILLISECONDS))
            throw new TimeoutException();

        RpcSync e = events.remove(messageId);
        if (e.status == RpcConstants.STATUS_OK)
            return e.returnValue;
        else throw new RpcStatusException(e.status, e.statusText);

    }


}
