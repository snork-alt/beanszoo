package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.exceptions.BeansZooException;

import java.util.Map;

/**
 * Created by admin on 9/2/17.
 */
public class YarnLifeCycleManager extends AbstractYarnLifeCycleManager {

    String zkAddress;

    public YarnLifeCycleManager(String containerId, Configuration config, String zkAddress, Map<String,String> props) {
        super(config, props, containerId);
        this.zkAddress = zkAddress;
    }

    @Override
    public void stop() throws BeansZooException {

    }
}
