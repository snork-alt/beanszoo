package com.dataheaps.beanszoo.codecs;

import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by admin on 19/1/17.
 */
public class FstRPCRequestCodec implements RPCRequestCodec {

    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    @Override
    public Object deserialize(byte[] o) throws IOException {
        return conf.asObject(o);
    }

    @Override
    public byte[] serialize(Object o) throws IOException {
        return conf.asByteArray(o);
    }

}
