package com.dataheaps.beanszoo.lifecycle;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by admin on 19/2/17.
 */
public class SingleProcessLifeCycleManager extends AbstractLifeCycleManager {

    Container container;
    String containerId;

    public SingleProcessLifeCycleManager(String containerId, Configuration config) {
        super(config);
        this.containerId = containerId;
    }

    @Override
    public void start() throws Exception {
        for (ContainerConfiguration c : config.getContainers()) {
            if (c.getId().equals(containerId)) {
                container = createContainer(c, config.getRoles(), config.getRpcFactory(), config.getSdFactory());
                runCommands(container, c);
                return;
            }
        }
    }

    @Override
    public void stop() throws Exception {

    }
}
