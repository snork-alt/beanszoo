package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by matteopelati on 28/10/15.
 */

@RequiredArgsConstructor
public class WsRpcServerHandler extends WebSocketAdapter {

    static final Logger logger = LoggerFactory.getLogger(WsRpcServerHandler.class);

    final Map<String,RPCRequestCodec> codecs;
    final RpcRequestHandler requestHandler;
    ExecutorService executors = Executors.newCachedThreadPool();
    Session session;

    @Override
    public void onWebSocketBinary(byte[] payload, int offset, int len) {

        super.onWebSocketBinary(payload, offset, len);
        executors.submit(() -> {

            Long messageId = null;

            try {

                Object[] decoded = StreamUtils.fromByteArray(
                        new Object[]{ Long.class, String.class, String.class, String.class, new byte[0]},
                        payload
                );
                messageId = (Long) decoded[0];

                RPCRequestCodec codec = codecs.get(decoded[1]);
                if (codec == null)
                    throw new Exception("Ivalid data format: " + decoded[1]);

                try {

                    Object result = requestHandler.handleRPCRequest(codec, (byte[]) decoded[4], (String) decoded[2], (String) decoded[3]);
                    byte[] res = StreamUtils.toByteArray(
                            new Object[] {messageId, RpcConstants.STATUS_OK, StringUtils.EMPTY, result}
                    );
                    session.getRemote().sendBytes(ByteBuffer.wrap(res));

                }
                catch(RpcStatusException e) {
                    sendException(messageId, e.statusCode, e.getMessage());
                }
                catch(InvocationTargetException e) {
                    sendException(messageId, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getTargetException().getMessage());
                }
                catch(Exception e) {
                    sendException(messageId, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getMessage());
                }

            }
            catch (Exception e) {

                if (messageId != null)
                    sendException(messageId, RpcConstants.STATUS_SERVER_EXCEPTION, e.getMessage());
            }
        });

    }

    void sendException(Long messageId, int status, String statusText) {

        try {

            byte[] res = StreamUtils.toByteArray(
                    new Object[] {
                            messageId,
                            status,
                            statusText != null ? statusText : StringUtils.EMPTY,
                            new byte[0]
                    }
            );
            session.getRemote().sendBytes(ByteBuffer.wrap(res));
        }
        catch(Exception ex) {

        }
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        session = null;
    }

    @Override
    public void onWebSocketConnect(Session sess) {
        super.onWebSocketConnect(sess);
        session = sess;
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        session = null;
    }

    @Override
    public void onWebSocketText(String message) {

    }
}
