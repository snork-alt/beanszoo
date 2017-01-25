package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.AutoSocketRpcServerAddress;
import com.dataheaps.beanszoo.rpc.SocketRpcServerAddress;
import org.apache.curator.test.TestingServer;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;

/**
 * Created by admin on 24/1/17.
 */
public class ZookeeperServiceDirectoryTest {

    public interface SampleService {
        String test();
    }

    public static class SampleServiceImpl1 implements SampleService {
        @Override
        public String test() {
            return this.getClass().getCanonicalName();
        }
    }

    @Name("impl2")
    public static class SampleServiceImpl2 implements SampleService {
        @Override
        public String test() {
            return this.getClass().getCanonicalName();
        }
    }


    @Test
    public void testServiceRegistrationOnDifferentServersAndStartStop() throws Exception {

        TestingServer server = new TestingServer(true);
        List<ServiceDirectory> sds = new ArrayList<>();


        for (int ctr=0;ctr<10;ctr++) {

            ZookeeperServiceDirectory sd = new ZookeeperServiceDirectory(
                    new SocketRpcServerAddress("localhost", 9090 + ctr), server.getConnectString()
            );

            String id = "SIMPLE" + ctr;
            sd.putService(id, new SampleServiceImpl1());
            String id2 = "NAMED" + ctr;
            sd.putService(id2, new SampleServiceImpl2());

            sds.add(sd);

        }

        int started = 0;
        for (ServiceDirectory sd : sds) {

            sd.start();
            started++;
            Thread.sleep(1000);
            Set<ServiceDescriptor> allServices = sd.getServicesByType(SampleService.class, null);
            Set<ServiceDescriptor> allNamedServices = sd.getServicesByType(SampleService.class, "impl2");
            assert (allServices.size() == started);
            assert (allNamedServices.size() == started);

        }

        ZookeeperServiceDirectory sdLookup = new ZookeeperServiceDirectory(
                new SocketRpcServerAddress("localhost", 9200), server.getConnectString()
        );
        sdLookup.start();

        int stopped = 0;
        for (ServiceDirectory sd : sds) {
            sd.stop();
            stopped++;
            Thread.sleep(1000);
            Set<ServiceDescriptor> allServices = sdLookup.getServicesByType(SampleService.class, null);
            Set<ServiceDescriptor> allNamedServices = sdLookup.getServicesByType(SampleService.class, "impl2");
            assert (allServices.size() == (started - stopped));
            assert (allNamedServices.size() == (started - stopped));

        }

    }

    @Test
    public void testServiceRegistrationOnDifferentServersWithIncrStart() throws Exception {

        TestingServer server = new TestingServer(true);

        for (int ctr=0;ctr<10;ctr++) {

            ZookeeperServiceDirectory sd = new ZookeeperServiceDirectory(
                    new SocketRpcServerAddress("localhost", 9090 + ctr), server.getConnectString()
            );
            sd.start();

            String id = "SIMPLE" + ctr;
            sd.putService(id, new SampleServiceImpl1());
            String id2 = "NAMED" + ctr;
            sd.putService(id2, new SampleServiceImpl2());

            Thread.sleep(1000);

            Set<ServiceDescriptor> allServices = sd.getServicesByType(SampleService.class, null);
            Set<ServiceDescriptor> allNamedServices = sd.getServicesByType(SampleService.class, "impl2");

            assert (allServices.size() == ctr+1);
            assert (allNamedServices.size() == ctr+1);

        }

    }


    @Test
    public void testServiceRegistrationOnDifferentServersAndStartStopParallel() throws Exception {

        TestingServer server = new TestingServer(true);
        List<ServiceDirectory> sds = Collections.synchronizedList(new ArrayList<>());
        ExecutorService executorService = Executors.newCachedThreadPool();

        ZookeeperServiceDirectory sdLookup = new ZookeeperServiceDirectory(
                new SocketRpcServerAddress("localhost", 12000), server.getConnectString()
        );
        sdLookup.start();

        int sdCount = 50;

        CountDownLatch latch = new CountDownLatch(sdCount);
        for (int ctr=0;ctr<sdCount;ctr++) {

            final int idx = ctr;
            executorService.submit(() -> {
                try {
                    ZookeeperServiceDirectory sd = new ZookeeperServiceDirectory(
                            new SocketRpcServerAddress("localhost", 9090 + idx), server.getConnectString()
                    );

                    if (idx % 2 == 0)
                        sd.start();

                    String id = "SIMPLE" + idx;
                    sd.putService(id, new SampleServiceImpl1());
                    String id2 = "NAMED" + idx;
                    sd.putService(id2, new SampleServiceImpl2());

                    if (idx % 2 != 0)
                        sd.start();

                    sds.add(sd);
                    latch.countDown();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        if (!latch.await(30, TimeUnit.SECONDS))
            throw new TimeoutException();
        Thread.sleep(1000);

        Set<ServiceDescriptor> allServices = sdLookup.getServicesByType(SampleService.class, null);
        Set<ServiceDescriptor> allNamedServices = sdLookup.getServicesByType(SampleService.class, "impl2");
        assert (allServices.size() == sdCount);
        assert (allNamedServices.size() == sdCount);


        CountDownLatch stopLatch = new CountDownLatch(sdCount);
        for (int ctr=0;ctr<sdCount;ctr++) {

            final ServiceDirectory sd = sds.get(ctr);

            executorService.submit(() -> {
                try {
                    sd.stop();
                    stopLatch.countDown();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        if (!stopLatch.await(30, TimeUnit.SECONDS))
            throw new TimeoutException();
        Thread.sleep(1000);

        allServices = sdLookup.getServicesByType(SampleService.class, null);
        allNamedServices = sdLookup.getServicesByType(SampleService.class, "impl2");
        assert (allServices.size() == 0);
        assert (allNamedServices.size() == 0);


    }

}