package com.dataheaps.beanszoo.lifecycle;

import java.util.Map;
import java.util.Properties;

/**
 * Created by admin on 19/2/17.
 */
public class SingleProcessLifeCycleManager extends AbstractLifeCycleManager {

    Container container;
    String containerId;

    public SingleProcessLifeCycleManager(String containerId, Configuration config, Properties props) {
        super(config, props);
        this.containerId = containerId;
    }

    @Override
    public void start() throws Exception {
        for (ContainerConfiguration c : config.getContainers()) {
            if (c.getId().equals(containerId)) {
                container = createContainer(c, config.getRoles(), config.getRpcFactory(), config.getSdFactory());
                runCommands(container, c, props);
                return;
            }
        }
    }

    @Override
    public void stop() throws Exception {

    }
}
