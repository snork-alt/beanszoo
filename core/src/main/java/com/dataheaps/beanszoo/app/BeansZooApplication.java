package com.dataheaps.beanszoo.app;

import com.dataheaps.beanszoo.exceptions.BeansZooException;

/**
 * Created by cs186076 on 29/9/17.
 */
public abstract class BeansZooApplication implements Application{

    public abstract void start() throws BeansZooException;

    public abstract void stop() throws BeansZooException;

}
