package com.dataheaps.beanszoo.sd;

/**
 * Created by admin on 26/1/17.
 */
public interface RegistrationHandler {

    boolean beforeRegister(String id, Object service, ServiceDirectory sd) throws Exception;
    void beforeUnregister(String id, Object service, ServiceDirectory sd) throws Exception;
}
