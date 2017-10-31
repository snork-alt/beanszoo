package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;
import com.dataheaps.beanszoo.sd.ZookeeperServiceDirectoryTest;

/**
 * Created by admin on 9/2/17.
 */
public class SimpleService1Impl extends AbstractLifeCycle implements SimpleService1 {

    Services services;

    public SimpleService1Impl() {
    }

    @Override
    public void init(Services services) throws Exception {
        this.services = services;
    }

    @Override
    public String test1() {
        return ZookeeperServiceDirectoryTest.SampleServiceImpl1.class.getCanonicalName();
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

}