package com.dataheaps.beanszoo.sd;

/**
 * Created by admin on 26/1/17.
 */
public class LeaderElectionHandler implements RegistrationHandler {

    @Override
    public boolean beforeRegister(String id, Object service, ServiceDirectory sd) throws Exception {

        Name name = service.getClass().getAnnotation(Name.class);
        if (name == null)
            throw new IllegalArgumentException("Class must be annotated with @Name for leader election");
        boolean added = sd.addLock(
                service.getClass().getCanonicalName() + "_" + name.value(),
                lockId -> sd.putService(id, service)
        );
        return added;
    }

    @Override
    public void beforeUnregister(String id, Object service, ServiceDirectory sd) throws Exception {

        Name name = service.getClass().getAnnotation(Name.class);
        sd.removeLock(
                service.getClass().getCanonicalName() + "_" + name.value()
        );
    }
}
