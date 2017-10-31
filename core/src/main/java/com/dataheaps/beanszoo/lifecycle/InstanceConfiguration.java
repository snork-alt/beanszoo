package com.dataheaps.beanszoo.lifecycle;

import java.util.HashMap;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by admin on 8/2/17.
 */

@Data @AllArgsConstructor @NoArgsConstructor
public class InstanceConfiguration {
    Class<?> type;
    Map<String,Object> configuration = new HashMap<>();
}
