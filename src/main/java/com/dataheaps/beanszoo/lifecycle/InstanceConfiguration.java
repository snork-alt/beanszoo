package com.dataheaps.beanszoo.lifecycle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by admin on 8/2/17.
 */

@Data @AllArgsConstructor @NoArgsConstructor
public class InstanceConfiguration {
    Class<?> type;
    Map<String,Object> configuration = new HashMap<>();
}
