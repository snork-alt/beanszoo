package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.*;
import com.dataheaps.beanszoo.sd.policies.InvocationPolicy;
import com.dataheaps.beanszoo.sd.policies.RoundRobinPolicy;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Created by admin on 24/1/17.
 */
public class ServicesTest {

    public interface SampleService {
        int test();
    }

    @Name({"test", "test2"})
    public static class SampleServiceImpl1 implements SampleService {

        int ctr = 0;

        @Override
        public int test() {
            ctr++;
            return ctr;
        }
    }

    @Name({"test3", "test4"})
    public static class SampleServiceImpl2 implements SampleService {

        int ctr = 0;

        @Override
        public int test() {
            ctr++;
            return ctr;
        }
    }

    public static class SampleNamedServiceImpl1 implements SampleService, Named {

        String name;
        int ctr = 0;

        public SampleNamedServiceImpl1(String name) {
            this.name = name;
        }

        @Override
        public int test() {
            ctr++;
            return ctr;
        }

        @Override
        public String[] getNames() {
            return new String[] {name};
        }
    }

    @Test
    public void basicLocalInstanceInvocation() throws Exception {

        LocalServiceDirectory serverSd = new LocalServiceDirectory();
        serverSd.start();
        serverSd.putService("id1", new SampleServiceImpl1());

        RpcClient localRpcClient = new LocalRpcClient();
        Services services = new Services(localRpcClient, serverSd);

        SampleService remote = services.getService("id1", SampleService.class);
        assert(remote.test() == 1);

    }

    @Test
    public void basicInstanceInvocation() throws Exception {

    	try(TestingServer server = new TestingServer(true)) {
    		SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString(), "/bztest"
            );
            serverSd.start();
            serverSd.putService("id1", new SampleServiceImpl1());

            RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
            rpcServer.start();

            RpcClient localRpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            Services serverServices = new Services(localRpcClient, serverSd);
            SampleService local = serverServices.getService("id1", SampleService.class);
            assert (local.test() == 1);

            RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            SocketRpcServerAddress clientAddress = new SocketRpcServerAddress("localhost", 9091);
            ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                    clientAddress, server.getConnectString(), "/bztest"
            );
            clientSd.start();
            Thread.sleep(1000);

            Services clientServices = new Services(rpcClient, clientSd);
            SampleService remote = clientServices.getService("id1", SampleService.class);
            assert(remote.test() == 2);

            serverSd.stop();
            clientSd.stop();
            rpcServer.stop();
        }

    }

    @Test
    public void basicInstanceInvocationNamed() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString(), "/bztest"
            );
            serverSd.start();
            serverSd.putService("id1", new SampleNamedServiceImpl1("test0"));
            serverSd.putService("id2", new SampleNamedServiceImpl1("test1"));

            RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
            rpcServer.start();

            RpcClient localRpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            Services serverServices = new Services(localRpcClient, serverSd);
            SampleService local = serverServices.getService(SampleService.class, "test0");
            assert (local.test() == 1);
            SampleService local2 = serverServices.getService(SampleService.class, "test1");
            assert (local2.test() == 1);

            serverServices.getServicesMetadata(SampleService.class);

            RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            SocketRpcServerAddress clientAddress = new SocketRpcServerAddress("localhost", 9091);
            ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                    clientAddress, server.getConnectString(), "/bztest"
            );
            clientSd.start();
            Thread.sleep(1000);

            Services clientServices = new Services(rpcClient, clientSd);
            SampleService remote = clientServices.getService(SampleService.class, "test0");
            assert(remote.test() == 2);
            SampleService remote2 = clientServices.getService(SampleService.class, "test1");
            assert(remote2.test() == 2);

            clientServices.getServicesMetadata(SampleService.class);

            serverSd.stop();
            clientSd.stop();
            rpcServer.stop();
        }

    }

    @Test
    public void basicInstanceInvocationNamed2() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
            SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090);
            ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                    serverAddress, server.getConnectString(), "/bztest"
            );
            serverSd.start();
            serverSd.putService("id1", new SampleServiceImpl1());
            serverSd.putService("id2", new SampleServiceImpl2());

            RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
            rpcServer.start();

            RpcClient localRpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            Services serverServices = new Services(localRpcClient, serverSd);
            SampleService local = serverServices.getService(SampleService.class, "test");
            assert (local.test() == 1);
            SampleService local2 = serverServices.getService(SampleService.class, "test4");
            assert (local2.test() == 1);

            RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            SocketRpcServerAddress clientAddress = new SocketRpcServerAddress("localhost", 9091);
            ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                    clientAddress, server.getConnectString(), "/bztest"
            );
            clientSd.start();
            Thread.sleep(1000);

            Services clientServices = new Services(rpcClient, clientSd);
            SampleService remote = clientServices.getService(SampleService.class, "test");
            assert(remote.test() == 2);
            SampleService remote2 = clientServices.getService(SampleService.class, "test4");
            assert(remote2.test() == 2);

            serverSd.stop();
            clientSd.stop();
            rpcServer.stop();
        }

    }

    public interface SampleService2 {
        String test();
    }

    public interface SampleService2RR {
        @InvocationPolicy(RoundRobinPolicy.class)
        String test();
    }

    public class SampleService2Impl implements SampleService2 {

        String name;

        public SampleService2Impl(String name) {
            this.name = name;
        }

        @Override
        public String test() {
            return name;
        }
    }

    @Name("test")
    public class SampleRRService2Impl implements SampleService2RR {

        String name;

        public SampleRRService2Impl(String name) {
            this.name = name;
        }

        @Override
        public String test() {
            return name;
        }
    }

    @Test
    public void localInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sdl = new ArrayList<>();
            Set<String> serviceNames = new HashSet<>();

            for (int ctr=0;ctr<5;ctr++) {

                SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 9090 + ctr);
                ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                        serverAddress, server.getConnectString(), "/bztest"
                );
                serverSd.start();
                serverSd.putService(new SampleService2Impl("service" + ctr));
                serviceNames.add("service" + ctr);

                RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                rpcServer.start();
                servers.add(rpcServer);
                sdl.add(serverSd);
            }
            RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
            Thread.sleep(1000);

            Services clientServices = new Services(rpcClient, sdl.get(0));
            SampleService2 svc = clientServices.getService(SampleService2.class);

            Set<String> res = new HashSet<>();
            for (int ctr=0;ctr<50;ctr++) {
                res.add(svc.test());
            }

            assertEquals(res, new HashSet<String>(Arrays.asList(new String[]{"service0"})));

            for (RpcServer s : servers)
                s.stop();
            for (ServiceDirectory s : sdl)
                s.stop();
            
            server.stop();
        }

    }


    @Test
    public void localOrRandomInvocation() throws Exception {

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
                    serverSd.putService(new SampleService2Impl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();
                    servers.add(rpcServer);
                    sdl.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                SocketRpcServerAddress clientAddress = new SocketRpcServerAddress("localhost", 40091);
                ZookeeperServiceDirectory clientSd = new ZookeeperServiceDirectory(
                        clientAddress, server.getConnectString(), "/bztest"
                );
                clientSd.start();
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, clientSd);
                SampleService2 svc = clientServices.getService(SampleService2.class);

                Set<String> res = new HashSet<>();
                for (int ctr = 0; ctr < 1000; ctr++) {
                    res.add(svc.test());
                }

                assertEquals(serviceNames, res);
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
    public void roundRobinInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sds = new ArrayList<>();
            List<String> serviceNames = new ArrayList<>();

            try {
                for (int ctr = 0; ctr < 30; ctr++) {

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SampleRRService2Impl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();

                    servers.add(rpcServer);
                    sds.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, sds.get(0));
                SampleService2RR svc = clientServices.getService(SampleService2RR.class);

                List<String> res = new ArrayList<>();
                for (int ctr = 0; ctr < 30; ctr++) {
                    res.add(svc.test());
                }

                Collections.sort(res);
                Collections.sort(serviceNames);

                assertEquals(res, serviceNames);

            }
            finally {

                for (RpcServer s : servers)
                    s.stop();
                for (ServiceDirectory s : sds)
                    s.stop();

                server.stop();
            }
        }

    }

    @Test
    public void roundRobinNamedInvocation() throws Exception {

        try(TestingServer server = new TestingServer(true)) {
        	List<RpcServer> servers = new ArrayList<>();
            List<ServiceDirectory> sds = new ArrayList<>();
            List<String> serviceNames = new ArrayList<>();

            try {

                for (int ctr = 0; ctr < 30; ctr++) {

                    SocketRpcServerAddress serverAddress = new SocketRpcServerAddress("localhost", 30090 + ctr);
                    ZookeeperServiceDirectory serverSd = new ZookeeperServiceDirectory(
                            serverAddress, server.getConnectString(), "/bztest"
                    );
                    serverSd.start();
                    serverSd.putService(new SampleRRService2Impl("service" + ctr));
                    serviceNames.add("service" + ctr);

                    RpcServer rpcServer = new SocketRpcServer(serverAddress, new FstRPCRequestCodec(), serverSd);
                    rpcServer.start();

                    servers.add(rpcServer);
                    sds.add(serverSd);
                }


                RpcClient rpcClient = new SocketRpcClient(new FstRPCRequestCodec(), 5000);
                Thread.sleep(1000);

                Services clientServices = new Services(rpcClient, sds.get(0));
                SampleService2RR svc = clientServices.getService(SampleService2RR.class, "test");

                List<String> res = new ArrayList<>();
                for (int ctr = 0; ctr < 30; ctr++) {
                    res.add(svc.test());
                }

                Collections.sort(res);
                Collections.sort(serviceNames);

                assertEquals(res, serviceNames);
            }
            finally {

                for (RpcServer s : servers)
                    s.stop();
                for (ServiceDirectory s : sds)
                    s.stop();

                server.stop();
            }	
        }

    }

}