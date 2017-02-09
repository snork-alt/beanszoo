package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcFactory;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.ServiceDirectoryFactory;
import com.dataheaps.beanszoo.sd.Services;
import org.apache.commons.beanutils.BeanMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by admin on 8/2/17.
 */
public class LocalLifeCycleManager extends AbstractLifeCycleManager {

    List<Container> containers = new ArrayList<>();

    public LocalLifeCycleManager(Configuration config) {
        super(config);
    }

    @Override
    public void start() throws Exception {

        for (ContainerConfiguration c : config.getContainers()) {
            for (int ctr=0;ctr<c.getInstances();ctr++)
                containers.add(createContainer(c, config.getRoles(), config.getRpcFactory(), config.getSdFactory()));
        }

    }

    @Override
    public void stop() throws Exception {

    }

    List<ServiceConfiguration> getServices(String[] roles, RoleConfiguration[] allRoles)  {

        Map<String, RoleConfiguration> rolesMap =
                Arrays.stream(allRoles).collect(Collectors.toMap(RoleConfiguration::getId, Function.identity()));

        List<ServiceConfiguration> services = new ArrayList<>();
        for (String role: roles) {
            RoleConfiguration roleConfig = rolesMap.get(role);
            if (roleConfig == null)
                throw new IllegalArgumentException("Role " + role + " does not exist");
            services.addAll(Arrays.asList(roleConfig.getServices()));
        }

        return services;
    }

    List<LifeCycle> instantiateServices(List<ServiceConfiguration> serviceConfigs) throws Exception {

        List<LifeCycle> services = new ArrayList<>();
        for (ServiceConfiguration cfg: serviceConfigs) {
            LifeCycle instance = (LifeCycle) cfg.getType().newInstance();
            new BeanMap(instance).putAll(cfg.getConfiguration());
            services.add(instance);
        }

        return services;
    }


    Container createContainer(
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

        List<ServiceConfiguration> serviceConfigs = getServices(containerConfig.getRoles(), roles);
        List<LifeCycle> serviceInstances = instantiateServices(serviceConfigs);

        for (LifeCycle lc: serviceInstances)
            lc.init(services);
        for (LifeCycle lc: serviceInstances)
            lc.start();
        for (LifeCycle lc: serviceInstances)
            sd.putService(lc);

        return new Container(sd, rpcClient, rpcServer, serverAddress, services);

    }
}



