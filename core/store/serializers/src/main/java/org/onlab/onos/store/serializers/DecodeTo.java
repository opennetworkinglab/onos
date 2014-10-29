package org.onlab.onos.store.serializers;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Function;

/**
 * Function to convert byte[] into {@code T}.
 *
 * @param <T> Type after decoding
 */
public final class DecodeTo<T> implements Function<byte[], T> {

    private StoreSerializer serializer;

    public DecodeTo(StoreSerializer serializer) {
        this.serializer = checkNotNull(serializer);
    }

    @Override
    public T apply(byte[] input) {
        return serializer.decode(input);
    }
}