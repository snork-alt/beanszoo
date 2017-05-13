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


}



