package com.dataheaps.beanszoo.codecs;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.BeanAccess;

import java.io.UnsupportedEncodingException;

/**
 * Created by matteopelati on 24/10/15.
 */
public class YamlRPCRequestCodec implements RPCRequestCodec {

    @Override
    public Object deserialize(byte[] o) {

        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);

        try {
            String s = new String(o, "UTF-8");
            return yaml.load(s);
        }
        catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] serialize(Object o) {

        Yaml yaml = new Yaml();
        yaml.setBeanAccess(BeanAccess.FIELD);

        try {
            String r = yaml.dump(o);
            return r.getBytes("UTF-8");
        }
        catch(UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}