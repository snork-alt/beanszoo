package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;

import java.util.Map;
import java.util.Properties;

/**
 * Created by matteopelati on 9/29/17.
 */
public interface Command {

    void run(Services services, Properties props) throws Exception;
}
