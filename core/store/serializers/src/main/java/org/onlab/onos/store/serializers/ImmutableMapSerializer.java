package org.onlab.onos.store.serializers;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.onlab.util.KryoPool.FamilySerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.MapSerializer;
import com.google.common.collect.ImmutableMap;

/**
* Kryo Serializer for {@link ImmutableMap}.
*/
public class ImmutableMapSerializer extends FamilySerializer<ImmutableMap<?, ?>> {

    private final MapSerializer mapSerializer = new MapSerializer();

    public ImmutableMapSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ImmutableMap<?, ?> object) {
        // wrapping with unmodifiableMap proxy
        // to avoid Kryo from writing only the reference marker of this instance,
        // which will be embedded right before this method call.
        kryo.writeObject(output, Collections.unmodifiableMap(object), mapSerializer);
    }

    @Override
    public ImmutableMap<?, ?> read(Kryo kryo, Input input,
                                    Class<ImmutableMap<?, ?>> type) {
        Map<?, ?> map = kryo.readObject(input, HashMap.class, mapSerializer);
        return ImmutableMap.copyOf(map);
    }

    @Override
    public void registerFamilies(Kryo kryo) {
        kryo.register(ImmutableMap.of().getClass(), this);
        kryo.register(ImmutableMap.of(1, 2).getClass(), this);
        kryo.register(ImmutableMap.of(1, 2, 3, 4).getClass(), this);
        // TODO register required ImmutableMap variants
    }
}
