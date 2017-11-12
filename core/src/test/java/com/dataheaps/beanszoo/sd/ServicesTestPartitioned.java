package com.dataheaps.beanszoo.sd;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.curator.test.TestingServer;
import org.junit.Test;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcClient;
import com.dataheaps.beanszoo.rpc.SocketRpcServer;
import com.dataheaps.beanszoo.rpc.SocketRpcServerAddress;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.dataheaps.beanszoo.sd.policies.PartitionKey;
import com.dataheaps.beanszoo.sd.policies.Partitioned;
import com.dataheaps.beanszoo.sd.policies.PartitioningPolicy;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTestPartitioned {

    public interface SamplePartitionedService {
        @InvocationPolicy(PartitioningPolicy.class)
        @Partitioned List<String> process(@PartitionKey List<String> ls);

        String nonPartitioned();
    }


    public static class SamplePartitionedServiceImpl implements SamplePartitionedService {

        String id;

        public SamplePartitionedServiceImpl(String id) {
            this.id = id;
        }

        @Override
        public List<String> process(List<String> ls) {
            List<String> res = new ArrayList<>();
            for (String s : ls) {
                res.add(id + ";" + s);
            }
            return res;
        }

        @Override
        public String nonPartitioned() {
            return id;
        }
    }

    @Test
    public void distributeAllInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sdl = new ArrayList<>();
            Set<String> serviceNames = new HashSet<>();

            try {

                for (int ctr = 0; ctr < 10; ctr++) {

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SamplePartitionedServiceImpl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.add(rpcServer);
                    sdl.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, sdl.get(0));
                SamplePartitionedService svc = clientServices.getService(SamplePartitionedService.class);

                List<String> args = new ArrayList<>();
                for (int ctr = 0; ctr < 10; ctr++)
                    args.add("" + ctr);

                List<String> res = svc.process(args);
                assert (res.size() == 10);

                Set<String> invokedServices = new HashSet<>();
                for (String r : res) {
                    invokedServices.add(r.split(";")[0]);
                }
                assert (invokedServices.equals(serviceNames));
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

    @Test
    public void distributePartialInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sdl = new ArrayList<>();
            Set<String> serviceNames = new HashSet<>();

            try {

                for (int ctr = 0; ctr < 10; ctr++) {

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SamplePartitionedServiceImpl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.add(rpcServer);
                    sdl.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, sdl.get(0));
                SamplePartitionedService svc = clientServices.getService(SamplePartitionedService.class);

                List<String> args = new ArrayList<>();
                for (int ctr = 0; ctr < 5; ctr++)
                    args.add("" + ctr);

                List<String> res = svc.process(args);
                assert (res.size() == 5);

                for (String r : res) {
                    serviceNames.remove(r.split(";")[0]);
                }
                assert (serviceNames.size() == 5);
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

    @Test
    public void nonPartitionedInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sdl = new ArrayList<>();
            Set<String> serviceNames = new HashSet<>();

            try {

                for (int ctr = 0; ctr < 10; ctr++) {

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SamplePartitionedServiceImpl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.add(rpcServer);
                    sdl.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, sdl.get(0));
                SamplePartitionedService svc = clientServices.getService(SamplePartitionedService.class);

                assert (serviceNames.contains(svc.nonPartitioned()));

            }
            finally {

                for (RpcServer s : servers)
                    s.stop();
                for (ServiceDirectory s : sdl)
                    s.stop();
                server.stop();
            }

            Thread.sleep(1000);

        }
        
    }

}