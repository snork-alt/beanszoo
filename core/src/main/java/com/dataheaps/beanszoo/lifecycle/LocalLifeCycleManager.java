package com.dataheaps.beanszoo.lifecycle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by admin on 8/2/17.
 */
public class LocalLifeCycleManager extends AbstractLifeCycleManager {

    List<Container> containers = new ArrayList<>();

    public LocalLifeCycleManager(Configuration config, Map<String,String> props) {
        super(config, props);
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



