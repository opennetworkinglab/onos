/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.TrafficSelector;

/**
 * Kryo Serializer for {@link FilteredConnectPointSerializer}.
 */
public class FilteredConnectPointSerializer extends Serializer<FilteredConnectPoint> {

    /**
     * Creates {@link FilteredConnectPointSerializer} serializer instance.
     */
    public FilteredConnectPointSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, FilteredConnectPoint object) {
        kryo.writeClassAndObject(output, object.connectPoint());
        kryo.writeClassAndObject(output, object.trafficSelector());
    }

    @Override
    public FilteredConnectPoint read(Kryo kryo, Input input, Class<FilteredConnectPoint> type) {
        ConnectPoint connectPoint = (ConnectPoint) kryo.readClassAndObject(input);
        TrafficSelector trafficSelector = (TrafficSelector) kryo.readClassAndObject(input);
        return new FilteredConnectPoint(connectPoint, trafficSelector);
    }
}
