package com.dataheaps.beanszoo.sd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.curator.test.TestingServer;
import org.junit.Test;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcClient;
import com.dataheaps.beanszoo.rpc.SocketRpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcServerAddress;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTestLeaderElection {

    public interface SampleSingleInstanceService {
        String test();
    }

    @OnRegister(LeaderElectionHandler.class) @Group("SingleInstance")
    public static class SampleSingleInstanceServiceImpl
            implements SampleSingleInstanceService, Activable {

        String id;

        public SampleSingleInstanceServiceImpl(String id) {
            this.id = id;
        }

        @Override
        public String test() {
            return id;
        }

        @Override
        public void activate() throws Exception {
            System.out.print(id + " activated\n");
        }

        @Override
        public void deactivate() throws Exception {
            System.out.print(id + " deactivated\n");
        }
    }

    @Test
    public void leaderElectInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	Map<String, RpcServer> servers = new ConcurrentHashMap<>();
            Map<String, ServiceDirectory> sdl = new ConcurrentHashMap<>();
            List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

            try {

                for (int ctr = 0; ctr < 20; ctr++) {

                    int idx = ctr;
                    threads.add(new Thread(() -> {
                        try {
                            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + idx);
                            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                                    serverAddress, server.getConnectString(), "/bztest", 500
                            );
                            serverSd.start();
                            serverSd.putService(new SampleSingleInstanceServiceImpl("service" + idx));

                            RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                            rpcServer.start();
                            servers.put("service" + idx, rpcServer);
                            sdl.put("service" + idx, serverSd);
                        } catch (Exception e) {

                        }
                    }));
                }

                for (Thread t : threads)
                    t.start();


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 40090);
                ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                        serverAddress, server.getConnectString(), "/bztest", 500
                );
                clientSd.start();


                for (int ctr = 0; ctr < 10; ctr++) {

                    Thread.sleep(1000);

                    Services clientServices = new Services(rpcClient, clientSd);
                    SampleSingleInstanceService svc = clientServices.getService(SampleSingleInstanceService.class);

                    Set<String> invoked = new HashSet<>();
                    for (int i = 0; i < 10; i++) {
                        invoked.add(svc.test());
                    }

                    assert (invoked.size() == 1 && sdl.keySet().contains(new ArrayList<>(invoked).get(0)));

                    String service = new ArrayList<>(invoked).get(0);
                    servers.remove(service).stop();
                    sdl.remove(service).stop();

                }
            }
            finally {

                for (ServiceDirectory s : sdl.values()) {
                    s.stop();
                }
                for (RpcServer s : servers.values()) {
                    s.stop();
                }
                server.stop();
            }
        }

    }

}