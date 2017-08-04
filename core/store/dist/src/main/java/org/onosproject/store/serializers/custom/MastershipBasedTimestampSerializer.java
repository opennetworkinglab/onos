/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.serializers.custom;

import org.onosproject.store.impl.MastershipBasedTimestamp;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

// To be used if Timestamp ever needs to cross bundle boundary.
/**
 * Kryo Serializer for {@link MastershipBasedTimestamp}.
 */
public class MastershipBasedTimestampSerializer extends Serializer<MastershipBasedTimestamp> {

    /**
     * Creates a serializer for {@link MastershipBasedTimestamp}.
     */
    public MastershipBasedTimestampSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipBasedTimestamp object) {
        output.writeLong(object.termNumber());
        output.writeLong(object.sequenceNumber());
    }

    @Override
    public MastershipBasedTimestamp read(Kryo kryo, Input input, Class<MastershipBasedTimestamp> type) {
        final long term = input.readLong();
        final long sequence = input.readLong();
        return new MastershipBasedTimestamp(term, sequence);
    }
}
