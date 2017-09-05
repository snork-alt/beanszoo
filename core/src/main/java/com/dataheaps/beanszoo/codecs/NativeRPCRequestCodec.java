package com.dataheaps.beanszoo.codecs;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;

/**
 * Created by admin on 30/5/16.
 */
public class NativeRPCRequestCodec implements RPCRequestCodec {

    @Override
    public Object deserialize(byte[] o) throws IOException {
        return SerializationUtils.deserialize(o);
    }

    @Override
    public byte[] serialize(Object o) throws IOException {
        return SerializationUtils.serialize((Serializable)o);
    }

}
