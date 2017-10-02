package com.dataheaps.beanszoo.app;

import com.dataheaps.beanszoo.exceptions.BeansZooException;

/**
 * Created by cs186076 on 29/9/17.
 */
public interface Application {

    public void start() throws BeansZooException;

    public void stop() throws BeansZooException;
}
