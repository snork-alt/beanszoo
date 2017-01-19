package com.dataheaps.beanszoo;

import com.dataheaps.beanszoo.codecs.FstRPCRequestCodec;
import com.dataheaps.beanszoo.codecs.NativeRPCRequestCodec;
import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import com.dataheaps.beanszoo.codecs.YamlRPCRequestCodec;
import com.dataheaps.beanszoo.rpc.*;
import com.dataheaps.beanszoo.sd.*;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by matteopelati on 25/10/15.
 */
public class Test {

    static CountDownLatch latch = new CountDownLatch(3);

    public static interface ISmapleService {
        public String sayHello(String name);
    }

    @RequiredArgsConstructor @Name("example")
    public static class SampleService implements ISmapleService {
        final String serviceName;
        @Override public String sayHello(String name) throws IllegalArgumentException {
            return "Hello " + name + " from service " + serviceName;
        }
    }

    static RpcServer createRpcServer(SocketRpcServerAddress address, ServiceDirectory sd) throws Exception {
        List<RPCRequestCodec> codes = new ArrayList<>();
        codes.add(new YamlRPCRequestCodec());
        codes.add(new FstRPCRequestCodec());
        SocketRpcServer server = new SocketRpcServer(
                address,
                codes,
                new ServiceDirectoryRPCRequestHandler(sd)
        );
       // server.start();
        return server;
    }

    static RemoteServiceDirectory createServiceDirectory(String connString, RpcServerAddress address) throws Exception {
        ZookeperServiceDirectory sd = new ZookeperServiceDirectory(connString, address);
    //    sd.start();
        return sd;
    }


    @RequiredArgsConstructor
    static class Client implements Runnable {

        final int client;
        final AtomicInteger counter;

        @Override
        public void run() {

            try {

                RemoteServiceDirectory sd = new ZookeperServiceDirectory("localhost:2181");
                sd.start();

                RpcClient cli = new SocketRpcClient(new FstRPCRequestCodec(), 10000);
                Services services = new Services(cli, sd);

                ISmapleService svc = (ISmapleService) services.getServiceByType(ISmapleService.class, "example");

                ExecutorService executorService = Executors.newFixedThreadPool(20);
                for (int ctr = 0; ctr < 10; ctr++) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < 10000; i++) {
                                try {
                                    System.out.println("Client " + client + ": " + counter.incrementAndGet() + " " + svc.sayHello("Matteo"));
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


        List<RpcServer> servers = new ArrayList<>();
        List<RemoteServiceDirectory> serviceDirectories = new ArrayList<>();

        for (int ctr = 0; ctr < 3; ctr++) {
            SocketRpcServerAddress addr = new AutoSocketRpcServerAddress();
            RemoteServiceDirectory sd = createServiceDirectory("localhost:2181", addr);
            RpcServer server = createRpcServer(addr, sd);
            serviceDirectories.add(sd);
            servers.add(server);
            sd.putService("SampleService" + ctr, new SampleService("SampleService" + ctr));
            server.start();
            sd.start();
        }


        AtomicInteger ai = new AtomicInteger();

        ExecutorService executors = Executors.newCachedThreadPool();
        executors.submit(new Client(1, ai));
        executors.submit(new Client(2, ai));

        executors.submit(() -> {
            try {

                Thread.sleep(5000);
                serviceDirectories.get(0).stop();
                serviceDirectories.get(1).stop();

                Thread.sleep(2000);
                serviceDirectories.get(0).start();
                serviceDirectories.get(1).start();
                for (int ctr = 4; ctr < 7; ctr++) {
                    SocketRpcServerAddress addr = new AutoSocketRpcServerAddress();
                    RemoteServiceDirectory sd = createServiceDirectory("localhost:2181", addr);
                    RpcServer server = createRpcServer(addr, sd);
                    serviceDirectories.add(sd);
                    servers.add(server);
                    sd.putService("SampleService" + ctr, new SampleService("SampleService" + ctr));
                    server.start();
                    sd.start();
                }
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
