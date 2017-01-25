package com.dataheaps.beanszoo.sd.policies;

import com.dataheaps.beanszoo.sd.ServiceDescriptor;

import java.util.*;

/**
 * Created by admin on 25/1/17.
 */
public class RandomPartitioner implements Partitioner {

    @Override
    public <T> List<List<T>> partition(List<T> data, List<ServiceDescriptor> instances) {

        List<List<T>> pData = new ArrayList<>(instances.size());
        for (int ctr=0;ctr<instances.size();ctr++)
            pData.add(new ArrayList<T>());

        int currIdx = 0;
        for (T datum: data) {
            pData.get(currIdx).add(datum);
            currIdx = ((currIdx + 1) % instances.size());
        }

        return pData;

    }

    @Override
    public Object union(List data, List<ServiceDescriptor> instances) {
        List res = new ArrayList();
        for (Object v : data) {
            if (v != null)
                res.addAll((Collection)v);
        }
        return res;
    }
}
