package com.dataheaps.beanszoo.lifecycle;

import com.dataheaps.beanszoo.rpc.RpcFactory;
import com.dataheaps.beanszoo.sd.ServiceDirectoryFactory;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by admin on 8/2/17.
 */

@Data @AllArgsConstructor
@NoArgsConstructor
public class Configuration {
    RpcFactory<?> rpcFactory;
    ServiceDirectoryFactory sdFactory;
    RoleConfiguration[] roles;
    ContainerConfiguration[] containers;
}
