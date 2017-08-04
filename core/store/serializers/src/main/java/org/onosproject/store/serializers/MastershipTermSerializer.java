/*
 * Copyright 2014-present Open Networking Foundation
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

import static org.onosproject.store.serializers.NodeIdSerializer.nodeIdSerializer;

import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipTerm;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link org.onosproject.mastership.MastershipTerm}.
 */
public final class MastershipTermSerializer extends Serializer<MastershipTerm> {

    /**
     * Creates {@link MastershipTerm} serializer instance.
     */
    public MastershipTermSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public MastershipTerm read(Kryo kryo, Input input, Class<MastershipTerm> type) {
        final NodeId node = kryo.readObjectOrNull(input, NodeId.class, nodeIdSerializer());
        final long term = input.readLong();
        return MastershipTerm.of(node, term);
    }

    @Override
    public void write(Kryo kryo, Output output, MastershipTerm object) {
        kryo.writeObjectOrNull(output, object.master(), nodeIdSerializer());
        output.writeLong(object.termNumber());
    }
}
