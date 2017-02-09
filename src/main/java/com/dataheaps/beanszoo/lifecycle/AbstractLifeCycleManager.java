package com.dataheaps.beanszoo.lifecycle;

import lombok.AllArgsConstructor;

import java.util.List;
import java.util.Set;

/**
 * Created by admin on 8/2/17.
 */

@AllArgsConstructor
public abstract class AbstractLifeCycleManager {

    final Configuration config;

    public abstract void start() throws Exception;
    public abstract void stop() throws Exception;


}
