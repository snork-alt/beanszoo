package com.dataheaps.beanszoo.utils;

import com.google.common.base.Optional;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;

/**
 * Created by cs186076 on 3/10/17.
 */
public class CommonUtils {

    static final int MAX_PORT_ASSIGNMENT_ATTEMPTS = 5;

    /**
     * Assigns a port dynamically if {@code requestedPort} is absent and updates the {@code assignedPorts} set.
     * Attempts up to {@code MAX_PORT_ASSIGNMENT_ATTEMPTS} to calculate an assigned port.  Visible for testing.
     */
    public static int getAvailablePort(Optional<Integer> requestedPort) {

        if (requestedPort.isPresent()) {
            int port = requestedPort.get();
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            }
            catch (BindException be) {
                throw new RuntimeException(String.format("port %d is already bound", port));
            }
            catch (IOException ioe) {
                throw new RuntimeException(String.format("Could not determine whether port %d is bound", port), ioe);
            }
        }
        else {
            int attemptsRemaining = MAX_PORT_ASSIGNMENT_ATTEMPTS;
            while (attemptsRemaining > 0) {
                attemptsRemaining--;
                try (ServerSocket socket = new ServerSocket(0)) {
                    if (socket.isBound()) {
                        return socket.getLocalPort();
                    }
                }
                catch (BindException be) {
                    // port is already bound, try again
                    continue;
                }
                catch (IOException ioe) {
                    throw new RuntimeException(String.format("Error while attempting to retrieve an available port"), ioe);
                }
            }
            throw new RuntimeException(String.format("Could not find an available port after %d attempts",
                                                     MAX_PORT_ASSIGNMENT_ATTEMPTS));
        }
    }


}
