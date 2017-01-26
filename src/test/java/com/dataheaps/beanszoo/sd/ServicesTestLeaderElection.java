package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.*;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.dataheaps.beanszoo.sd.policies.PartitionKey;
import com.dataheaps.beanszoo.sd.policies.Partitioned;
import com.dataheaps.beanszoo.sd.policies.PartitioningPolicy;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTestLeaderElection {

    public interface SampleSingleInstanceService {
        String test();
    }

    @OnRegister(LeaderElectionHandler.class) @Name("SingleInstance")
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

        TestingServer server = new TestingServer(true);

        Map<String, RpcServer> servers = new ConcurrentHashMap<>();
        Map<String, ServiceDirectory> sdl = new ConcurrentHashMap<>();
        List<Thread> threads = Collections.synchronizedList(new ArrayList<>());

        for (int ctr=0;ctr<20;ctr++) {

            int idx = ctr;
            threads.add(new Thread(() -> {
                try {
                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + idx);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), 500
                    );
                    serverSd.start();
                    serverSd.putService(new SampleSingleInstanceServiceImpl("service" + idx));

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.put("service" + idx, rpcServer);
                    sdl.put("service" + idx, serverSd);
                }
                catch (Exception e) {

                }
            }));
        }

        for (Thread t: threads)
            t.start();


        RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
        SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 10090);
        ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                serverAddress, server.getConnectString(),500
        );
        clientSd.start();


        for (int ctr=0;ctr<10;ctr++) {

            Thread.sleep(1000);

            Services clientServices = new Services(rpcClient, clientSd);
            SampleSingleInstanceService svc = clientServices.getService(SampleSingleInstanceService.class);

            Set<String> invoked = new HashSet<>();
            for (int i=0;i<10;i++) {
                invoked.add(svc.test());
            }

            assert (invoked.size() == 1 && sdl.keySet().contains(new ArrayList<>(invoked).get(0)));

            String service = new ArrayList<>(invoked).get(0);
            servers.remove(service).stop();
            sdl.remove(service).stop();

        }


        for (ServiceDirectory s : sdl.values()) {
            s.stop();
        }
        for (RpcServer s : servers.values()) {
            s.stop();
        }

    }






}