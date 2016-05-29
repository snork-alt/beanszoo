package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 29/5/16.
 */
public class SocketRpcClient extends AbstractRpcClient<SocketRpcController> {

    public SocketRpcClient(RPCRequestCodec codec, int timeout) {
        super(codec, timeout);
    }

    Map<String, SocketRpcController> sessions = new ConcurrentHashMap<>();

    @Override
    synchronized SocketRpcController getSession(String address) throws IOException {
        SocketRpcController c = sessions.get(address);
        if (c == null) {
            Socket socket = new Socket(address.split(":")[0], Integer.parseInt(address.split(":")[1]));
            c = new SocketRpcController(socket, new SocketRpcController.MessageHandler() {
                @Override
                public void handleMessage(Socket socket, byte[] buffer) {
                    handleResponse(buffer);
                }

                @Override
                public void socketError(Socket socket) {
                    System.out.print("err");
                }
            });
            sessions.put(address, c);
            c.start();
        }
        return c;
    }

    @Override
    void sendMessage(SocketRpcController session, byte[] payload) throws IOException {
        session.sendMessage(payload);
    }
}
