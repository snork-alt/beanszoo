package com.dataheaps.beanszoo.lifecycle;

import java.io.InputStream;

/**
 * Created by admin on 9/2/17.
 */
public interface ConfigurationReader {
    Configuration load(InputStream yamlStream, InputStream propsStream) throws Exception;
}
