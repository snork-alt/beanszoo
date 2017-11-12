package com.dataheaps.beanszoo.rpc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by matteopelati on 28/10/15.
 */
public class StreamUtils {

    public static byte[] toByteArray(Object[] items) throws IOException, IllegalArgumentException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(os);

        for (Object o: items) {
            if (o instanceof String)
                dataOut.writeUTF((String)o);
            else if (o instanceof Integer)
                dataOut.writeInt((Integer)o);
            else if (o instanceof Long)
                dataOut.writeLong((Long)o);
            else if (o.getClass().isArray() && o.getClass().getComponentType().equals(byte.class)) {
                dataOut.writeInt(((byte[]) o).length);
                dataOut.write((byte[]) o);
            }
            else if (o instanceof Boolean)
                dataOut.writeBoolean((Boolean)o);
            else throw new IllegalArgumentException("Invalid data type");
        }

        dataOut.flush();
        return os.toByteArray();

    }

    public static Object[] fromByteArray(Object[] types, byte[] data) throws IOException, IllegalArgumentException {

        ByteArrayInputStream is = new ByteArrayInputStream(data);
        DataInputStream dataIn = new DataInputStream(is);
        List<Object> result = new ArrayList<>();

        for (Object c : types) {
            if (c.equals(String.class))
                result.add(dataIn.readUTF());
            else if (c.equals(Integer.class))
                result.add(dataIn.readInt());
            else if (c.equals(Long.class))
                result.add(dataIn.readLong());
            else if (c.getClass().isArray() && c.getClass().getComponentType().equals(byte.class)) {
                byte[] buffer = new byte[dataIn.readInt()];
                dataIn.read(buffer);
                result.add(buffer);
            }
            else if (c.equals(Boolean.class))
                result.add(dataIn.readBoolean());
            else
                throw new IllegalArgumentException("Invalid data type");

        }

        return result.toArray();




    }


}
