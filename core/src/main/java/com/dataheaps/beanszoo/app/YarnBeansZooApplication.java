package com.dataheaps.beanszoo.app;

import com.dataheaps.beanszoo.exceptions.BeansZooException;
import com.dataheaps.beanszoo.lifecycle.Configuration;
import com.dataheaps.beanszoo.lifecycle.ContainerConfiguration;
import com.dataheaps.beanszoo.lifecycle.YarnLifeCycleManager;
import com.google.common.base.Preconditions;

import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.twill.api.TwillApplication;
import org.apache.twill.api.TwillRunnerService;
import org.apache.twill.api.TwillSpecification;
import org.apache.twill.yarn.YarnTwillRunnerService;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Properties;

/**
 *
 * @author chandras
 */
@AllArgsConstructor @Getter @Setter
public class YarnBeansZooApplication extends AbstractYarnBeansZooApplication implements TwillApplication {

    YarnConfiguration confYarn;
    Configuration config;
    String zkAddress;
    String name;

    public YarnBeansZooApplication(Configuration conf, String zkConnectionString, String name) {
        new YarnBeansZooApplication(new YarnConfiguration(), conf, zkConnectionString, name);
    }

    public YarnBeansZooApplication(Configuration hdfsConf, Configuration conf, String zkConnectionString, String name) {
        super();
    }

    @Override
    public TwillSpecification configure() {
        TwillSpecification.Builder.MoreRunnable mr = TwillSpecification.Builder.with().setName(name).withRunnable();
        TwillSpecification.Builder.RunnableSetter rs = null;
        for(ContainerConfiguration cc : config.getContainers()){
            try {
                if(rs == null){
                    rs = mr.add(cc.getId(), new YarnLifeCycleManager(cc.getId(), config, zkAddress, new Properties())).noLocalFiles();
                } else {
                    rs.add(cc.getId(), new YarnLifeCycleManager(cc.getId(), config, zkAddress, new Properties())).noLocalFiles();
                }
            } catch (Exception e) {
                throw new BeansZooException(e.getCause());
            }
        }
        return rs.anyOrder().build();
    }

    @Override
    public void start() throws BeansZooException {
        runner = new YarnTwillRunnerService(confYarn, zkAddress);
        runner.start();
        preparer = runner.prepare(new YarnBeansZooApplication(confYarn,config,zkAddress,name));
        preparer.start();
    }

    public TwillRunnerService getRunner(){
        Preconditions.checkNotNull(runner);
        return runner;
    }

    @Override
    public void stop() throws BeansZooException {
    }
}
