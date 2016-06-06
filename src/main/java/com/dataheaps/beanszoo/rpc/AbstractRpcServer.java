package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by matteopelati on 28/10/15.
 */

public abstract class AbstractRpcServer<E> implements RpcServer {

    static final Logger logger = LoggerFactory.getLogger(AbstractRpcServer.class);

    final Map<String,RPCRequestCodec> codecs = new ConcurrentHashMap<>();
    final RpcRequestHandler requestHandler;
    final RpcServerAddress bindings;
    ExecutorService executors = Executors.newCachedThreadPool();

    public AbstractRpcServer(RpcServerAddress bindings, List<RPCRequestCodec> codecs, RpcRequestHandler requestHandler) {
        for (RPCRequestCodec codec : codecs)
            this.codecs.put(codec.getContentType(), codec);
        this.requestHandler = requestHandler;
        this.bindings = bindings;
    }

    public void handleRequest(E endpoint, byte[] payload) {

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
                    sendMessage(endpoint, res);

                }
                catch(RpcStatusException e) {
                    sendException(endpoint, messageId, e.statusCode, e.getMessage());
                }
                catch(InvocationTargetException e) {
                    sendException(endpoint, messageId, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getTargetException().getMessage());
                }
                catch(Exception e) {
                    sendException(endpoint, messageId, RpcConstants.STATUS_SERVICE_EXCEPTION, e.getMessage());
                }

            }
            catch (Exception e) {

                if (messageId != null)
                    sendException(endpoint, messageId, RpcConstants.STATUS_SERVER_EXCEPTION, e.getMessage());
            }
        });

    }


    void sendException(E endpoint, Long messageId, int status, String statusText) {

        try {

            byte[] res = StreamUtils.toByteArray(
                    new Object[] {
                            messageId,
                            status,
                            statusText != null ? statusText : StringUtils.EMPTY,
                            new byte[0]
                    }
            );
            sendMessage(endpoint, res);
        }
        catch(Exception ex) {

        }
    }

    abstract void sendMessage(E endpoint, byte[] buffer) throws Exception;


}
