package com.dataheaps.beanszoo.sd;

import com.dataheaps.beanszoo.rpc.RpcServerAddress;
import lombok.*;

/**
 * Created by admin on 9/2/17.
 */

@RequiredArgsConstructor @NoArgsConstructor
public class ZookeeperServiceDirectoryFactory implements ServiceDirectoryFactory {

    @Getter @Setter @NonNull String connectionString;
    @Getter @Setter @NonNull Integer timeout;
    @Getter @Setter @NonNull String basepath = "/beanszoo";

    @Override
    public ServiceDirectory create(RpcServerAddress address) {
        return new ZookeeperServiceDirectory(address, connectionString, basepath, timeout);
    }
}
