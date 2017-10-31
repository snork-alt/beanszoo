package com.dataheaps.beanszoo.sd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.curator.test.TestingServer;
import org.junit.Test;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcClient;
import com.dataheaps.beanszoo.rpc.SocketRpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcServerAddress;
import com.dataheaps.beanszoo.sd.policies.BroadcastPolicy;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.google.common.collect.ImmutableList;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTestBroadcast {

    public interface SampleBroadcastService {
        @InvocationPolicy(BroadcastPolicy.class)
        void process(String id);

    }


    public static class SampleBroadcastServiceImpl implements SampleBroadcastService {

        List<String> ls;

        public SampleBroadcastServiceImpl(List<String> ls) {
            this.ls = ls;
        }

        @Override
        public void process(String id) {
            ls.add(id);
        }
    }

    @Test
    public void invokeBcast() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sdl = new ArrayList<>();
            List<List<String>> results = new ArrayList<>();

            try {

                for (int ctr = 0; ctr < 10; ctr++) {

                    List<String> res = new CopyOnWriteArrayList<>();
                    results.add(res);

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SampleBroadcastServiceImpl(res));

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.add(rpcServer);
                    sdl.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(500);

                Services clientServices = new Services(rpcClient, sdl.get(0));
                SampleBroadcastService svc = clientServices.getService(SampleBroadcastService.class);

                for (int ctr = 0; ctr < 5; ctr++) {
                    svc.process("test" + ctr);
                }

                assert (results.stream().allMatch(e -> e.size() == 5));
                assert (results.stream().allMatch(e -> e.equals(ImmutableList.of("test0", "test1", "test2", "test3", "test4"))));

            }
            finally {

                for (RpcServer s : servers)
                    s.stop();
                for (ServiceDirectory s : sdl)
                    s.stop();

                server.stop();
            }
        }

    }

}