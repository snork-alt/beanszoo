package com.dataheaps.beanszoo.rpc;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.dataheaps.beanszoo.codecs.RPCRequestCodec;

/**
 * Created by admin on 29/5/16.
 */
public class SocketRpcClient extends AbstractRpcClient<SocketRpcController> {

    public SocketRpcClient(RPCRequestCodec codec, int timeout) {
        super(codec, timeout);
    }

    private ReadWriteLock lock = new ReentrantReadWriteLock();
    private Map<String, SocketRpcController> sessions = new HashMap<>();
    private Map<Socket, SocketRpcController> sockets = new HashMap<>();
    private Map<Socket, String> addresses = new HashMap<>();

    @Override
    SocketRpcController getSession(String address) throws IOException {

        lock.writeLock().lock();
        try {
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
                        lock.writeLock().lock();
                        try {
                            String address = addresses.get(socket);
                            addresses.remove(socket);
                            sockets.remove(socket);
                            if (address != null)
                                sessions.remove(address);
                        }
                        finally {
                            lock.writeLock().unlock();
                        }
                    }
                });
                sessions.put(address, c);
                sockets.put(c.socket, c);
                addresses.put(c.socket, address);
                c.start();
            }
            return c;
        }
        finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    void sendMessage(SocketRpcController session, byte[] payload) throws IOException {
        session.sendMessage(payload);
    }
}
