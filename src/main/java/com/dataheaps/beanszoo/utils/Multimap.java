package com.dataheaps.beanszoo.utils;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by admin on 29/5/16.
 */
public class Multimap<K, V> {

    Map<K, Set<V>> root = new HashMap<>();

    synchronized public void put(K key, V value) {

        Set<V> values = root.get(key);
        if (values == null) {
            values = Collections.synchronizedSet(new HashSet<V>());
            root.put(key, values);
        }
        values.add(value);

    }

    synchronized public Set<V> get(K key) {
        return root.get(key);
    }

    synchronized public void remove(K key, V value) {
        Set<V> values = root.get(key);
        if (values == null) return;
        values.remove(value);
    }

    synchronized public void removeAll(K key) {
        root.remove(key);
    }

}
