package com.dataheaps.beanszoo.lifecycle;

import java.io.InputStream;
import java.util.Properties;

/**
 * Created by admin on 9/2/17.
 */
public interface ConfigurationReader {
    Configuration load(InputStream yamlStream, Properties p) throws Exception;
}
