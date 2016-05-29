package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.utils.Multimap;
import org.apache.commons.lang3.ClassUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by matteopelati on 25/10/15.
 */
public class InMemoryServiceDirectory implements ServiceDirectory {

    Map<String,Object> namedServices = new ConcurrentHashMap<>();
    Map<Object,String> namedServicesInverted = new ConcurrentHashMap<>();
    Multimap<String,Object> classServices = new Multimap<>();

    @Override
    public Object getService(String id) {
        return namedServices.get(id);
    }

    @Override
    public Object getServiceByType(Class type, String name) {
        Set ret = classServices.get(type.getCanonicalName() + ((name == null) ? "" : ("!" + name)));
        if (ret == null || ret.size() == 0) return null;
        return ret.toArray()[0];
    }

    @Override
    public Set getServicesByType(Class type) {
        Set ret = classServices.get(type.getCanonicalName());
        if (ret == null) return Collections.EMPTY_SET;
        else return ret;
    }

    @Override
    public void putService(Object service) {

        putService(UUID.randomUUID().toString(), service);
    }

    @Override
    public void putService(String id, Object service) {
        namedServices.put(id, service);
        namedServicesInverted.put(service, id);
        Name name = service.getClass().getAnnotation(Name.class);
        ClassUtils.getAllInterfaces(service.getClass()).forEach(
                c -> {
                    classServices.put(c.getCanonicalName(), service);
                    if (name != null)
                        classServices.put(c.getCanonicalName() + "!" + name.value(), service);
                }
        );
    }

    @Override
    public boolean removeService(Object service) {
        String id = namedServicesInverted.get(service);
        return removeService(id, service);
    }

    @Override
    public boolean removeService(String id) {
        Object service = namedServices.get(id);
        return removeService(id, service);
    }

    public boolean removeService(String id, Object service) {
        if (id == null || service == null) return false;
        ClassUtils.getAllInterfaces(service.getClass()).forEach(c -> classServices.remove(c.getCanonicalName(), service));
        classServices.remove(id, service);
        namedServices.remove(id);
        namedServicesInverted.remove(service);
        return true;
    }

    @Override
    public synchronized void start() throws Exception {

    }

    @Override
    public synchronized void stop() throws Exception {

    }
}
