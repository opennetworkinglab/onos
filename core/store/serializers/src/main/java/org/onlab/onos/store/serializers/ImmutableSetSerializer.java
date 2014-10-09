package org.onlab.onos.store.serializers;

import java.util.ArrayList;
import java.util.List;

import org.onlab.util.KryoPool.FamilySerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.google.common.collect.ImmutableSet;

/**
* Kryo Serializer for {@link ImmutableSet}.
*/
public class ImmutableSetSerializer extends FamilySerializer<ImmutableSet<?>> {

    private final CollectionSerializer serializer = new CollectionSerializer();

    /**
     * Creates {@link ImmutableSet} serializer instance.
     */
    public ImmutableSetSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ImmutableSet<?> object) {
        kryo.writeObject(output, object.asList(), serializer);
    }

    @Override
    public ImmutableSet<?> read(Kryo kryo, Input input,
                                Class<ImmutableSet<?>> type) {
        List<?> elms = kryo.readObject(input, ArrayList.class, serializer);
        return ImmutableSet.copyOf(elms);
    }

    @Override
    public void registerFamilies(Kryo kryo) {
        kryo.register(ImmutableSet.of().getClass(), this);
        kryo.register(ImmutableSet.of(1).getClass(), this);
        kryo.register(ImmutableSet.of(1, 2).getClass(), this);
        // TODO register required ImmutableSet variants
    }
}
