package org.onosproject.store.serializers;

import org.onosproject.net.DefaultAnnotations;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.util.HashMap;

public class AnnotationsSerializer extends Serializer<DefaultAnnotations> {

    public AnnotationsSerializer() {
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultAnnotations object) {
        kryo.writeObject(output, object.asMap());
    }

    @Override
    public DefaultAnnotations read(Kryo kryo, Input input, Class<DefaultAnnotations> type) {
        DefaultAnnotations.Builder b = DefaultAnnotations.builder();
        HashMap<String, String> map = kryo.readObject(input, HashMap.class);
        map.forEach((k, v) -> b.set(k, v));

        return b.build();
    }

}
