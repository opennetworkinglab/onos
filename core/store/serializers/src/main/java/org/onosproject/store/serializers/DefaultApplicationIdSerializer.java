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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.onosproject.core.DefaultApplicationId;

/**
 * Kryo Serializer for {@link org.onosproject.core.DefaultApplicationId}.
 */
public final class DefaultApplicationIdSerializer extends Serializer<DefaultApplicationId> {

    /**
     * Creates {@link org.onosproject.core.DefaultApplicationId} serializer instance.
     */
    public DefaultApplicationIdSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultApplicationId object) {
        output.writeShort(object.id());
        output.writeString(object.name());
    }

    @Override
    public DefaultApplicationId read(Kryo kryo, Input input, Class<DefaultApplicationId> type) {
        short id = input.readShort();
        String name = input.readString();
        return new DefaultApplicationId(id, name);
    }
}
