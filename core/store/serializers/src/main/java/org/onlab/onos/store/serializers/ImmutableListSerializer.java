package org.onlab.onos.store.serializers;

import org.onlab.util.KryoPool.FamilySerializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

/**
 * Creates {@link ImmutableList} serializer instance.
 */
public class ImmutableListSerializer extends FamilySerializer<ImmutableList<?>> {

    /**
     * Creates {@link ImmutableList} serializer instance.
     */
    public ImmutableListSerializer() {
        // non-null, immutable
        super(false, true);
    }
    @Override
    public void write(Kryo kryo, Output output, ImmutableList<?> object) {
        output.writeInt(object.size());
        for (Object e : object) {
            kryo.writeClassAndObject(output, e);
        }
    }

    @Override
    public ImmutableList<?> read(Kryo kryo, Input input,
            Class<ImmutableList<?>> type) {
        final int size = input.readInt();
        Builder<Object> builder = ImmutableList.builder();
        for (int i = 0; i < size; ++i) {
            builder.add(kryo.readClassAndObject(input));
        }
        return builder.build();
    }

    @Override
    public void registerFamilies(Kryo kryo) {
        kryo.register(ImmutableList.of(1).getClass(), this);
        kryo.register(ImmutableList.of(1, 2).getClass(), this);
        // TODO register required ImmutableList variants
    }

}
