package com.dataheaps.beanszoo.utils;

import com.dataheaps.beanszoo.lifecycle.InstanceConfiguration;
import com.dataheaps.beanszoo.lifecycle.LifeCycle;
import com.dataheaps.beanszoo.lifecycle.RoleConfiguration;
import com.dataheaps.beanszoo.lifecycle.Service;
import com.dataheaps.beanszoo.sd.Services;

import org.apache.commons.beanutils.BeanMap;
import org.codehaus.plexus.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by cs186076 on 29/9/17.
 */
public class LifeCycleUtils {

    public List<Object> getServices(String[] roles, RoleConfiguration[] allRoles)  {

        Map<String, RoleConfiguration> rolesMap =
            Arrays.stream(allRoles).collect(Collectors.toMap(RoleConfiguration::getId, Function.identity()));

        List<Object> services = new ArrayList<>();
        for (String role: roles) {
            RoleConfiguration roleConfig = rolesMap.get(role);
            if (roleConfig == null)
                throw new IllegalArgumentException("Role " + role + " does not exist");
            services.addAll(Arrays.asList(roleConfig.getServices()));
        }

        return services;
    }


    public Object getInstance(Object i) throws InstantiationException, IllegalAccessException {

        if (i instanceof InstanceConfiguration) {
            InstanceConfiguration ic = (InstanceConfiguration) i;
            BeanMap bean = new BeanMap(ic.getType().newInstance());
            if (ic.getConfiguration() == null || ic.getConfiguration().isEmpty())
                return bean.getBean();
            for (Map.Entry<String, Object> e: ic.getConfiguration().entrySet()) {
                if (bean.containsKey(e.getKey())) {
                    bean.put(e.getKey(), getInstance(e.getValue()));
                }
            }
            return bean.getBean();
        }
        else {
            return i;
        }
    }

    public List<LifeCycle> instantiateServices(List<Object> serviceConfigs) throws Exception {

        List<LifeCycle> services = new ArrayList<>();
        for (Object cfg: serviceConfigs) {
            LifeCycle instance = (LifeCycle) getInstance(cfg);
            services.add(instance);
        }

        return services;
    }

    public void injectServices(Object o, Services services) throws IllegalAccessException, IllegalArgumentException {

        List<Field> fields = ReflectionUtils.getFieldsIncludingSuperclasses(o.getClass());
        for (Field f : fields) {
            if (f.getAnnotation(Service.class) != null) {
                Object service = null;
                if (f.getAnnotation(Service.class).value().isEmpty())
                    service = services.getService(f.getType());
                else
                    service = services.getService(f.getType(), f.getAnnotation(Service.class).value());

                if (service == null)
                    throw new IllegalArgumentException("The service type " + f.getType().getCanonicalName() +  " cannot be found");
                f.setAccessible(true);
                f.set(o, service);
            }
        }
    }

}
