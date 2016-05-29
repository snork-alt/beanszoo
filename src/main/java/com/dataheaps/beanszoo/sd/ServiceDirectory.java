package com.dataheaps.beanszoo.sd;

import java.util.Set;

/**
 * Created by matteopelati on 25/10/15.
 */
public interface ServiceDirectory {

    Object getService(String id);
    Object getServiceByType(Class type, String name);
    Set getServicesByType(Class type);
    void putService(Object service);
    void putService(String id, Object service);
    boolean removeService(Object service);
    boolean removeService(String id);
    void start() throws Exception;
    void stop() throws Exception;

}
