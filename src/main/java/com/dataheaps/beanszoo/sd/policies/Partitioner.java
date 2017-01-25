package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.sd.ServiceDescriptor;

import java.util.List;
import java.util.Map;

/**
 * Created by admin on 25/1/17.
 */
public interface Partitioner {

    <T> List<List<T>> partition(List<T> data, List<ServiceDescriptor> instances);
    Object union(List data, List<ServiceDescriptor> instances);
}
