/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.store.serializers;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onlab.util.ImmutableByteSequence;

/**
 * Kryo serializer for {@link ImmutableByteSequence}.
 */
public class ImmutableByteSequenceSerializer extends Serializer<ImmutableByteSequence> {

    /**
     * Creates a new {@link ImmutableByteSequence} serializer instance.
     */
    public ImmutableByteSequenceSerializer() {
        // non-null, immutable.
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ImmutableByteSequence object) {
        byte[] data = object.asArray();
        output.writeInt(data.length);
        output.write(data);
    }

    @Override
    public ImmutableByteSequence read(Kryo kryo, Input input, Class<ImmutableByteSequence> type) {
        int length = input.readInt();
        byte[] data = new byte[length];
        input.read(data);
        return ImmutableByteSequence.copyFrom(data);
    }
}
