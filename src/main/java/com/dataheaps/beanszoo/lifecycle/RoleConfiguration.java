package com.dataheaps.beanszoo.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by admin on 8/2/17.
 */

@Data @AllArgsConstructor @NoArgsConstructor
public class RoleConfiguration {
    String id;
    InstanceConfiguration[] services;
}
