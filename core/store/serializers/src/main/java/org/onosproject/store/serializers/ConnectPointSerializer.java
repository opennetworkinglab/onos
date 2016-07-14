/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.ElementId;
import org.onosproject.net.PortNumber;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/**
 * Kryo Serializer for {@link ConnectPointSerializer}.
 */
public class ConnectPointSerializer extends Serializer<ConnectPoint> {

    /**
     * Creates {@link ConnectPointSerializer} serializer instance.
     */
    public ConnectPointSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, ConnectPoint object) {
        kryo.writeClassAndObject(output, object.elementId());
        kryo.writeClassAndObject(output, object.port());
    }

    @Override
    public ConnectPoint read(Kryo kryo, Input input, Class<ConnectPoint> type) {
        ElementId elementId = (ElementId) kryo.readClassAndObject(input);
        PortNumber portNumber = (PortNumber) kryo.readClassAndObject(input);
        return new ConnectPoint(elementId, portNumber);
    }
}
