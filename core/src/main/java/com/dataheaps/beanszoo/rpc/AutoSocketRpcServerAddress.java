package com.dataheaps.beanszoo.rpc;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by admin on 19/1/17.
 */
public class AutoSocketRpcServerAddress extends SocketRpcServerAddress {

    private static final int MIN_PORT_NUMBER = 49152;
    private static final int MAX_PORT_NUMBER = 65534;


    public AutoSocketRpcServerAddress() throws UnknownHostException {
        super(getLocalHostname(), getNextAvailablePortNumber());
    }

    public AutoSocketRpcServerAddress(int port) throws UnknownHostException {
        super(getLocalHostname(), port);
    }

    @Override
    public String geAddressString() {
        return String.format("%s:%d", hostname, port);
    }

    static int getNextAvailablePortNumber() {

        for (int ctr=0;ctr<500;ctr++) {
            int port = ThreadLocalRandom.current().nextInt(MIN_PORT_NUMBER, MAX_PORT_NUMBER);
            if (isPortAvailable(port))
                return port;
        }
        throw new IllegalArgumentException("Unable to allocate an available port");
    }

    static String getLocalHostname() throws UnknownHostException {
        return InetAddress.getLocalHost().getHostName();
    }

    static boolean isPortAvailable(int port) {
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                /* should not be thrown */
                }
            }
        }

        return false;
    }

}
