package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.exceptions.BeansZooException;
import com.dataheaps.beanszoo.utils.ContainerUtils;

import org.apache.twill.api.AbstractTwillRunnable;

import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Properties;

/**
 * Created by cs186076 on 29/9/17.
 * @author number9code
 */

@AllArgsConstructor
public abstract class AbstractYarnLifeCycleManager extends AbstractTwillRunnable{

    final Configuration config;
    final Properties props;
    String containerId;

    protected void runCommands(Container c, ContainerConfiguration cf, Properties props) throws Exception {
        for (Command cmd: cf.getCommands()) {
            cmd.run(c.services, props);
        }
    }

    @Override
    public void run() {
        for (ContainerConfiguration c : config.getContainers()) {
            if (c.getId().equals(containerId)) {
                try {
                    Container container = new ContainerUtils().createContainer(c, config.getRoles(), config.getRpcFactory(), config.getSdFactory());
                    runCommands(container, c, props);
                } catch (Exception e) {
                    throw new BeansZooException(e.getLocalizedMessage());
                }
                return;
            }
        }
    }

    @Override
    public void stop() {
        super.stop();
    }

    @Override
    public void destroy() {
        super.destroy();
    }
}
