package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;

/**
 * Created by admin on 9/2/17.
 */
public class SimpleService0Impl extends AbstractLifeCycle implements SimpleService0 {

    Services services;

    public SimpleService0Impl() {
    }

    @Override
    public void init(Services services) throws Exception {
        this.services = services;
    }

    @Override
    public String test0() {
        return services.getService(SimpleService1.class).test1();
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

}