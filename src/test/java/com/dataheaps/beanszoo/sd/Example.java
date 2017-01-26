package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.codecs.YamlRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.*;
import com.dataheaps.beanszoo.sd.*;
import lombok.RequiredArgsConstructor;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by matteopelati on 25/10/15.
 */
public class Example {

    static CountDownLatch latch = new CountDownLatch(3);


    public static interface ISmapleService {
        public String sayHello(String name);
    }

    @RequiredArgsConstructor
    public static class SampleService implements ISmapleService {
        final String serviceName;
        @Override public String sayHello(String name) throws IllegalArgumentException {
            return "Hello " + name + " from service " + serviceName;
        }
    }

    static RpcServer createRpcServer(SocketRpcServerAddress address, ServiceDirectory sd) throws Exception {

        SocketRpcServer server = new SocketRpcServer(address, new FstRPCRequestCodec(), sd);
        return server;
    }

    static ServiceDirectory createServiceDirectory(String connString, RpcServerAddress address) throws Exception {
        ZookeeperServiceDirectory sd = new ZookeeperServiceDirectory(address, connString);
        return sd;
    }


    @RequiredArgsConstructor
    static class Client implements Runnable {

        final int client;
        final AtomicInteger counter;
        final String connString;

        @Override
        public void run() {

            try {

                ServiceDirectory sd = new ZookeeperServiceDirectory(new SocketRpcServerAddress("127.0.0.1", 3400 + client), connString);
                sd.start();

                RpcClient cli = new SocketRpcClient(new FstRPCRequestCodec(), 10000);
                Services services = new Services(cli, sd);

                ISmapleService svc = services.getService(ISmapleService.class);

                ExecutorService executorService = Executors.newFixedThreadPool(20);
                for (int ctr = 0; ctr < 10; ctr++) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 10000; i++) {
                                try {
                                    String resp = svc.sayHello("Matteo");
                                    System.out.println("Client " + client + ": " + counter.incrementAndGet() + " " + resp);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            try {
                                Thread.sleep(200);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
            catch(Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                latch.countDown();
            }

        }
    }



    public static void main(String[] args) throws Exception {


        TestingServer testServer = new TestingServer();

        List<RpcServer> servers = new ArrayList<>();
        List<ServiceDirectory> serviceDirectories = new ArrayList<>();

        for (int ctr = 0; ctr < 3; ctr++) {
            SocketRpcServerAddress addr = new SocketRpcServerAddress("127.0.0.1", 12000 + ctr);
            ServiceDirectory sd = createServiceDirectory(testServer.getConnectString(), addr);
            RpcServer server = createRpcServer(addr, sd);
            sd.putService("SampleService" + ctr, new SampleService("SampleService" + ctr));
            server.start();
            sd.start();
            serviceDirectories.add(sd);
            servers.add(server);
        }


        AtomicInteger ai = new AtomicInteger();

        ExecutorService executors = Executors.newCachedThreadPool();
        executors.submit(new Client(1, ai, testServer.getConnectString()));
        executors.submit(new Client(2, ai, testServer.getConnectString()));

        executors.submit(() -> {
            try {

                Thread.sleep(5000);
                serviceDirectories.get(0).stop();
                serviceDirectories.get(1).stop();

                Thread.sleep(2000);
                serviceDirectories.get(0).start();
                serviceDirectories.get(1).start();
                for (int ctr = 4; ctr < 10; ctr++) {
                    SocketRpcServerAddress addr = new SocketRpcServerAddress("127.0.0.1", 13000 + ctr);
                    ServiceDirectory sd = createServiceDirectory(testServer.getConnectString(), addr);
                    RpcServer server = createRpcServer(addr, sd);
                    serviceDirectories.add(sd);
                    servers.add(server);
                    sd.putService("SampleService" + ctr, new SampleService("SampleService" + ctr));
                    server.start();
                    sd.start();
                }
                Thread.sleep(2000);
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
            finally {
                latch.countDown();
            }
        });

        latch.await();

        for (RpcServer server : servers)
            server.stop();


    }

}
