package producer;

import java.io.*;

public class Serializer {

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        os.close();
        out.close();
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        in.close();
        is.close();
        return is.readObject();
    }

}
