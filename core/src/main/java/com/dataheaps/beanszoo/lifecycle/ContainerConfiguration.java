package com.dataheaps.beanszoo.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by admin on 8/2/17.
 */

@Data @NoArgsConstructor @AllArgsConstructor
public class ContainerConfiguration {
    String id;
    String[] roles;
    int instances;
    Command[] commands;
}
