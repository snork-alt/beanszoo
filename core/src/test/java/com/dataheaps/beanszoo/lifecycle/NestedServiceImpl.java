package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.sd.Services;
import lombok.Getter;
import lombok.Setter;

/**
 * Created by admin on 19/2/17.
 */
public class NestedServiceImpl extends AbstractLifeCycle implements NestedService {

    @Getter @Setter NestedService nested;
    @Getter @Setter String id;
    @Getter @Setter Class<?> klass;

    @Override
    public String test() {
        if (nested != null)
            return nested.test();
        else
            return id;
    }

    @Override
    public void init(Services services) throws Exception {

    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }
}
