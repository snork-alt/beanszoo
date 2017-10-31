package com.dataheaps.beanszoo.app;

import org.apache.twill.api.TwillPreparer;
import org.apache.twill.api.TwillRunnerService;

/**
 * Created by cs186076 on 3/10/17.
 */
public abstract class AbstractYarnBeansZooApplication extends BeansZooApplication {

    public TwillPreparer preparer;

    public TwillRunnerService runner;

}