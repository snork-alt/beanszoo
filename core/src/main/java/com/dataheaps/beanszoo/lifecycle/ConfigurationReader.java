package com.dataheaps.beanszoo.lifecycle;

/**
 * Created by admin on 9/2/17.
 */
public interface ConfigurationReader {
    Configuration load(String path) throws Exception;
}
