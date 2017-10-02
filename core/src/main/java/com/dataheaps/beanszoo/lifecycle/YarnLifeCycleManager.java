package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.app.YarnBeansZooApplication;
import com.dataheaps.beanszoo.exceptions.BeansZooException;

import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillController;
import org.apache.twill.api.TwillPreparer;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.api.TwillSpecification;
import org.apache.twill.yarn.YarnTwillRunnerService;

/**
 * Created by admin on 9/2/17.
 */
public class YarnLifeCycleManager extends AbstractYarnLifeCycleManager {

    String zkAddress;

    public YarnLifeCycleManager(String containerId, Configuration config, String zkAddress) {
        super(config,containerId);
        this.zkAddress = zkAddress;
    }

    @Override
    public void stop() throws BeansZooException {

    }
}
