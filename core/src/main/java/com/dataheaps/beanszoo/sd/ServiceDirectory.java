package com.dataheaps.beanszoo.sd;

import java.util.Set;

/**
 * Created by matteopelati on 25/10/15.
 */
public interface ServiceDirectory {

    @FunctionalInterface
    public interface UnlockCb {
        void unlocked(String id) throws Exception;
    }


    ServiceDescriptor getService(String id);
    Set<ServiceDescriptor> getServicesByType(Class<?> type, String name);
    Object getLocalInstance(ServiceDescriptor d);
    void putService(Object service) throws Exception;
    void putService(String id, Object service) throws Exception;
    void removeService(Object service) throws Exception;
    void removeService(String id) throws Exception;
    boolean addLock(String id, UnlockCb unlockCb) throws Exception;
    void removeLock(String id) throws Exception;
    void start() throws Exception;
    void stop() throws Exception;


}
