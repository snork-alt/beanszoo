package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcConstants;
import com.dataheaps.beanszoo.rpc.RpcStatusException;
import com.dataheaps.beanszoo.sd.ServiceDescriptor;
import com.dataheaps.beanszoo.sd.ServiceDirectory;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Created by admin on 25/1/17.
 */
public class PartitioningPolicy implements Policy {

    static ExecutorService executors = Executors.newCachedThreadPool();
    Random rand = new Random();
    ConcurrentHashMap<Method, Integer> partitionKeyIndexes = new ConcurrentHashMap<>();

    int getPartitionKeyIndex(Method method) {

        Annotation[][] annotations = method.getParameterAnnotations();
        for (int ctr=0;ctr<method.getParameterCount();ctr++) {
            Annotation[] paramAnnotations = annotations[ctr];
            for (int pctr=0;pctr<paramAnnotations.length;pctr++) {
                if (paramAnnotations[pctr] instanceof PartitionKey) {
                    return ctr;
                }
            }
        }
        return -1;
    }


    Object invokeMultipleInstances(RpcClient client, Partitioner p, List<ServiceDescriptor> d, Method m, List<Object[]> args) throws Exception {

        final CountDownLatch latch = new CountDownLatch(d.size());
        final List<Object> results = Collections.synchronizedList(new ArrayList<>(d.stream().map(e -> null).collect(Collectors.toList())));
        final AtomicReference<Exception> exception = new AtomicReference<Exception>(null);

        for (int ctr=0;ctr<d.size();ctr++) {

            if (args.get(ctr) == null) {
                latch.countDown();
                continue;
            }

            final int index = ctr;
            executors.submit(() -> {
                try {
                    Object res = invokeSingleInstance(client, d.get(index), m, args.get(index));
                    results.set(index, res);
                }
                catch (Exception e) {
                    exception.set(e);
                }
                finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        if (exception.get() != null)
            throw exception.get();
        return p.union(results, d);
    }

    Object invokeSingleInstance(RpcClient client, ServiceDescriptor d, Method m, Object[] args) throws Exception {

        int ctr = 0;
        while (true) {

            try {
                return client.invoke(d, m.getName(), m.getParameterTypes(), args);
            }
            catch(RpcStatusException e) {
                if (e.getStatusCode() != RpcConstants.STATUS_SERVER_EXCEPTION || ctr > 5)
                    throw e;
                ctr++;
            }
            catch (Exception e) {
                throw e;
            }
        }

    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args, Class klass, String name, List<ServiceDescriptor> d, RpcClient rpcClient, ServiceDirectory services) throws Throwable {

        List<ServiceDescriptor> sdl = new ArrayList<>(services.getServicesByType(klass, name));
        if (sdl.isEmpty())
            throw new IllegalArgumentException("Object not found");

        Partitioned partitioned = method.getAnnotation(Partitioned.class);

        if (partitioned == null) {
            return invokeSingleInstance(rpcClient, sdl.get(Math.abs(rand.nextInt()) % sdl.size()), method, args);
        }
        else {

            Partitioner p = (Partitioner) partitioned.partitioner().newInstance();

            int partitionKeyIndex = partitionKeyIndexes.computeIfAbsent(method, m -> getPartitionKeyIndex(m));
            if (partitionKeyIndex < 0) {
                return invokeMultipleInstances(
                        rpcClient, p, sdl, method, sdl.stream().map(e -> args).collect(Collectors.toList())
                );
            }
            else {
                List<List> keyPartitioned = p.partition((List) args[partitionKeyIndex], sdl);
                return invokeMultipleInstances(
                        rpcClient,
                        p, sdl, method, keyPartitioned.stream().map(l -> {
                            if (l.isEmpty()) return null;
                            Object[] cloned = Arrays.copyOf(args, args.length);
                            cloned[partitionKeyIndex] = l;
                            return cloned;
                        }).collect(Collectors.toList())
                );
            }

        }

    }

}
