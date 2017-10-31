package com.dataheaps.beanszoo.rpc;

import com.dataheaps.beanszoo.sd.ServiceDirectory;
import com.dataheaps.beanszoo.codecs.RPCRequestCodec;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by matteopelati on 25/10/15.
 */

@RequiredArgsConstructor
public class ServiceDirectoryRPCRequestHandler implements RpcRequestHandler {

    final ServiceDirectory sd;

    @Override
    public byte[] handleRPCRequest(RPCRequestCodec codec, byte[] data, String objectPath, String method) throws Exception {

        List<?> args = (List<?>) codec.deserialize(data);
        Object target = sd.getService(objectPath);
        if (target == null)
            throw new RpcStatusException(RpcConstants.STATUS_SERVER_EXCEPTION, objectPath);
        Class<?>[] types = (Class[])((List<?>)args.stream().map(t -> t.getClass()).collect(Collectors.toList())).toArray(new Class[0]);
        Method m = target.getClass().getMethod(method, types);
        if (m == null)
            throw new IllegalArgumentException("No method " + method);
        m.setAccessible(true);
        Object ret = m.invoke(target, args.toArray());
        return codec.serialize(ret);
    }
}
