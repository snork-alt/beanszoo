package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import lombok.*;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by admin on 9/2/17.
 */

@RequiredArgsConstructor @NoArgsConstructor
public class SocketRpcFactory implements RpcFactory<SocketRpcServerAddress> {

    @Getter @Setter @NonNull RPCRequestCodec codec;
    @Getter @Setter @NonNull int timeout = 10000;
    @Getter @Setter @NonNull int lowestPort = -1;
    @Getter @Setter @NonNull int highestPort = -1;

    int currPort = -1;

    @Override
    public RpcServer createServer(SocketRpcServerAddress address, ServiceDirectory sd) throws Exception {
        return new SocketRpcServer(address, codec, sd);
    }

    @Override
    public RpcClient createClient() throws Exception {
        return new SocketRpcClient(codec, timeout);
    }

    @Override
    public synchronized RpcServerAddress createAddress() throws Exception {

        if (lowestPort < 0 || highestPort < 0) {
            return new AutoSocketRpcServerAddress();
        }
        else {
            if (currPort < 0) currPort = lowestPort; else currPort++;
            if (currPort > highestPort)
                throw new IllegalArgumentException("Unable to allocate port: " + currPort);
            return new AutoSocketRpcServerAddress(currPort);
        }
    }
}
