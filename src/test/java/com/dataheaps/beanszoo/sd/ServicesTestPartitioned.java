package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.*;
import com.dataheaps.beanszoo.sd.policies.*;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTestPartitioned {

    public interface SamplePartitionedService {
        @Partitioned List<String> process(@PartitionKey List<String> ls);
        String nonPartitioned();
    }

    @InvocationPolicy(PartitioningPolicy.class)
    public static class SamplePartitionedServiceImpl implements SamplePartitionedService {

        String id;

        public SamplePartitionedServiceImpl(String id) {
            this.id = id;
        }

        @Override
        public List<String> process(List<String> ls) {
            List<String> res = new ArrayList();
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

        TestingServer server = new TestingServer(true);

        List<RpcServer> servers = new ArrayList<>();
        List<ServiceDirectory> sdl = new ArrayList<>();
        Set<String> serviceNames = new HashSet<>();

        for (int ctr=0;ctr<10;ctr++) {

            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + ctr);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString()
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
        for (int ctr=0;ctr<10;ctr++)
            args.add("" + ctr);

        List<String> res = svc.process(args);
        assert (res.size() == 10);

        Set<String> invokedServices = new HashSet<>();
        for (String r: res) {
            invokedServices.add(r.split(";")[0]);
        }
        assert (invokedServices.equals(serviceNames));

        for (RpcServer s : servers)
            s.stop();

    }

    @Test
    public void distributePartialInvocation() throws Exception {

        TestingServer server = new TestingServer(true);

        List<RpcServer> servers = new ArrayList<>();
        List<ServiceDirectory> sdl = new ArrayList<>();
        Set<String> serviceNames = new HashSet<>();

        for (int ctr=0;ctr<10;ctr++) {

            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + ctr);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString()
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
        for (int ctr=0;ctr<5;ctr++)
            args.add("" + ctr);

        List<String> res = svc.process(args);
        assert (res.size() == 5);

        for (String r: res) {
            serviceNames.remove(r.split(";")[0]);
        }
        assert (serviceNames.size() == 5);

        for (RpcServer s : servers)
            s.stop();

    }

    @Test
    public void nonPartitionedInvocation() throws Exception {

        TestingServer server = new TestingServer(true);

        List<RpcServer> servers = new ArrayList<>();
        List<ServiceDirectory> sdl = new ArrayList<>();
        Set<String> serviceNames = new HashSet<>();

        for (int ctr=0;ctr<10;ctr++) {

            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + ctr);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString()
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

        assert(serviceNames.contains(svc.nonPartitioned()));

        for (RpcServer s : servers)
            s.stop();

    }






}