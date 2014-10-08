package org.onlab.onos.store.serializers;

import org.onlab.onos.store.cluster.messaging.MessageSubject;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public final class MessageSubjectSerializer extends Serializer<MessageSubject> {

    /**
     * Creates a serializer for {@link MessageSubject}.
     */
    public MessageSubjectSerializer() {
        // non-null, immutable
        super(false, true);
    }


    @Override
    public void write(Kryo kryo, Output output, MessageSubject object) {
        output.writeString(object.value());
    }

    @Override
    public MessageSubject read(Kryo kryo, Input input,
            Class<MessageSubject> type) {
        return new MessageSubject(input.readString());
    }
}
