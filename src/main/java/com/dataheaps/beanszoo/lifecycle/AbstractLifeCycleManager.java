package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.rpc.RpcClient;
import com.dataheaps.beanszoo.rpc.RpcFactory;
import com.dataheaps.beanszoo.rpc.RpcServer;
import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.sd.ServiceDirectoryFactory;
import com.dataheaps.beanszoo.sd.Services;
import lombok.AllArgsConstructor;
import org.apache.commons.beanutils.BeanMap;
import org.codehaus.plexus.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by admin on 8/2/17.
 */

@AllArgsConstructor
public abstract class AbstractLifeCycleManager {

    final Configuration config;

    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;


    List<Object> getServices(String[] roles, RoleConfiguration[] allRoles)  {

        Map<String, RoleConfiguration> rolesMap =
                Arrays.stream(allRoles).collect(Collectors.toMap(RoleConfiguration::getId, Function.identity()));

        List<Object> services = new ArrayList<>();
        for (String role: roles) {
            RoleConfiguration roleConfig = rolesMap.get(role);
            if (roleConfig == null)
                throw new IllegalArgumentException("Role " + role + " does not exist");
            services.addAll(Arrays.asList(roleConfig.getServices()));
        }

        return services;
    }


    Object getInstance(Object i) throws InstantiationException, IllegalAccessException {

        if (i instanceof InstanceConfiguration) {
            InstanceConfiguration ic = (InstanceConfiguration) i;
            BeanMap bean = new BeanMap(ic.getType().newInstance());
            if (ic.getConfiguration() == null || ic.getConfiguration().isEmpty())
                return bean.getBean();
            for (Map.Entry<String, Object> e: ic.getConfiguration().entrySet()) {
                if (bean.containsKey(e.getKey())) {
                    bean.put(e.getKey(), getInstance(e.getValue()));
                }
            }
            return bean.getBean();
        }
        else {
            return i;
        }
    }

    List<LifeCycle> instantiateServices(List<Object> serviceConfigs) throws Exception {

        List<LifeCycle> services = new ArrayList<>();
        for (Object cfg: serviceConfigs) {
            LifeCycle instance = (LifeCycle) getInstance(cfg);
            services.add(instance);
        }

        return services;
    }


    void injectServices(Object o, Services services) throws IllegalAccessException {

        List<Field> fields = ReflectionUtils.getFieldsIncludingSuperclasses(o.getClass());
        for (Field f : fields) {
            if (f.getAnnotation(Service.class) != null) {
                Object service = services.getService(f.getType());
                if (service != null) {
                    f.setAccessible(true);
                    f.set(o, service);
                }
            }
        }
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

        List<Object> serviceConfigs = getServices(containerConfig.getRoles(), roles);
        List<LifeCycle> serviceInstances = instantiateServices(serviceConfigs);

        for (LifeCycle lc: serviceInstances) {
            injectServices(lc, services);
            lc.init(services);
            lc.start();
            sd.putService(lc);
        }

        return new Container(sd, rpcClient, rpcServer, serverAddress, services);

    }

}
