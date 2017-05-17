package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;

/**
 * Created by admin on 8/2/17.
 */
public interface LifeCycle {

    enum Status {
        Idle, Starting, Running, Stopping
    }

    void init(Services services) throws Exception;
    void start() throws Exception;
    void stop() throws Exception;
    Status getStatus();

}
