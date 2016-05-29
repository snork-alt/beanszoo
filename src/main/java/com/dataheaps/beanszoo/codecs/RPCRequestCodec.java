package com.dataheaps.beanszoo.codecs;

import java.io.IOException;

/**
 * Created by matteopelati on 24/10/15.
 */
public interface RPCRequestCodec {

    Object deserialize(byte[] o) throws IOException;
    byte[] serialize(Object o) throws IOException;
    String getContentType();

}
