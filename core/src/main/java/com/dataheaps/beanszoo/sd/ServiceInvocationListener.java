package com.dataheaps.beanszoo.sd;

import java.lang.reflect.Method;

/**
 * Created by admin on 5/5/18.
 */
public interface ServiceInvocationListener {

    void onSuccess(Method m, Object[] args, Object ret);
    void onError(Method m, Object[] args, Exception e);

}
