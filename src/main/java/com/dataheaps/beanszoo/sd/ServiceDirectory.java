package com.dataheaps.beanszoo.sd;

import java.util.Set;

/**
 * Created by matteopelati on 25/10/15.
 */
public interface ServiceDirectory {

    ServiceDescriptor getService(String id);
    Set<ServiceDescriptor> getServicesByType(Class type, String name);
    Object getLocalInstance(ServiceDescriptor d);
    void putService(Object service);
    void putService(String id, Object service);
    boolean removeService(Object service);
    boolean removeService(String id);
    void start() throws Exception;
    void stop() throws Exception;

}
