package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.LocalRpcServerAddress;

/**
 * Created by admin on 17/9/17.
 */
public class LocalServiceDirectory extends AbstractServiceDirectory {

    public LocalServiceDirectory() {
        super(new LocalRpcServerAddress());
    }
}
