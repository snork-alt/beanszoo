package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;

import java.util.Map;

/**
 * Created by matteopelati on 9/29/17.
 */
public interface Command {

    void run(Services services, Map<String,String> props) throws Exception;
}
