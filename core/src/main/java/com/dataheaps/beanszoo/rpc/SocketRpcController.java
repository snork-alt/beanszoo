package com.dataheaps.beanszoo.rpc;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

/**
 * Created by admin on 29/5/16.
 */

public class SocketRpcController extends Thread {

    public static interface MessageHandler {
        void handleMessage(Socket socket, byte[] buffer);
        void socketError(Socket socket);
    }

    final Socket socket;
    final MessageHandler handler;

    public SocketRpcController(Socket socket, MessageHandler handler) throws SocketException {
        this.socket = socket;
        this.handler = handler;
        this.socket.setKeepAlive(true);
        this.socket.setReceiveBufferSize(1024*1024*1024);
        this.socket.setSendBufferSize(1024*1024*1024);
    }

    int fromByteArray(byte[] bytes) {
        return bytes[0] << 24 | (bytes[1] & 0xFF) << 16 | (bytes[2] & 0xFF) << 8 | (bytes[3] & 0xFF);
    }

    byte[] toByteArray(int value) {
        return new byte[] {
                (byte)(value >> 24),
                (byte)(value >> 16),
                (byte)(value >> 8),
                (byte)value };
    }

    @Override
    public void run() {
        try {

            while (!isInterrupted()) {
                byte[] header = new byte[4];
                socket.getInputStream().read(header);
                byte[] payload = new byte[fromByteArray(header)];
                socket.getInputStream().read(payload);
                handler.handleMessage(socket, payload);
            }

        }catch(IOException e) {
            handler.socketError(socket);
        }
    }

    public synchronized void sendMessage(byte[] payload) throws IOException {
        socket.getOutputStream().write(toByteArray(payload.length));
        socket.getOutputStream().write(payload);
        socket.getOutputStream().flush();
    }
}
