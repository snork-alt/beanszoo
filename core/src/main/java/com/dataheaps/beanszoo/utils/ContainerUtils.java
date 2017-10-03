package com.dataheaps.beanszoo.utils;

import com.dataheaps.beanszoo.lifecycle.Container;
import com.dataheaps.beanszoo.lifecycle.ContainerConfiguration;
import com.dataheaps.beanszoo.lifecycle.LifeCycle;
import com.dataheaps.beanszoo.lifecycle.RoleConfiguration;
import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcFactory;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.ServiceDirectoryFactory;
import com.dataheaps.beanszoo.sd.Services;

import java.util.List;

/**
 * Created by cs186076 on 29/9/17.
 */
public class ContainerUtils {

    public Container createContainer(
        ContainerConfiguration containerConfig, RoleConfiguration[] roles,
        RpcFactory rpcFactory, ServiceDirectoryFactory sdFactory
    ) throws Exception {

        RpcServerAddress serverAddress = rpcFactory.createAddress();

        ServiceDirectory sd = sdFactory.create(serverAddress);
        sd.start();

        RpcServer rpcServer = rpcFactory.createServer(serverAddress, sd);
        rpcServer.start();

        RpcClient rpcClient = rpcFactory.createClient();
        Services services = new Services(rpcClient, sd);

        LifeCycleUtils lcu = new LifeCycleUtils();

        List<Object> serviceConfigs = lcu.getServices(containerConfig.getRoles(), roles);
        List<LifeCycle> serviceInstances = lcu.instantiateServices(serviceConfigs);

        for (LifeCycle lc: serviceInstances) {
            lcu.injectServices(lc, services);
            lc.init(services);
            lc.start();
            sd.putService(lc);
        }

        return new Container(sd, rpcClient, rpcServer, serverAddress, services);

    }

}
