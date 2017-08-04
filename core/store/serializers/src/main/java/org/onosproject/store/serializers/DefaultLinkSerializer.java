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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.Link.State;
import org.onosproject.net.Link.Type;
import org.onosproject.net.provider.ProviderId;

/**
 * Kryo Serializer for {@link DefaultLink}.
 */
public class DefaultLinkSerializer extends Serializer<DefaultLink> {

    /**
     * Creates {@link DefaultLink} serializer instance.
     */
    public DefaultLinkSerializer() {
        // non-null, immutable
        super(false, true);
    }

    @Override
    public void write(Kryo kryo, Output output, DefaultLink object) {
        kryo.writeClassAndObject(output, object.providerId());
        kryo.writeClassAndObject(output, object.src());
        kryo.writeClassAndObject(output, object.dst());
        kryo.writeClassAndObject(output, object.type());
        kryo.writeClassAndObject(output, object.state());
        output.writeBoolean(object.isExpected());
    }

    @Override
    public DefaultLink read(Kryo kryo, Input input, Class<DefaultLink> type) {
        ProviderId providerId = (ProviderId) kryo.readClassAndObject(input);
        ConnectPoint src = (ConnectPoint) kryo.readClassAndObject(input);
        ConnectPoint dst = (ConnectPoint) kryo.readClassAndObject(input);
        Type linkType = (Type) kryo.readClassAndObject(input);
        State state = (State) kryo.readClassAndObject(input);
        boolean isDurable = input.readBoolean();
        return DefaultLink.builder()
                .providerId(providerId)
                .src(src)
                .dst(dst)
                .type(linkType)
                .state(state)
                .isExpected(isDurable)
                .build();
    }
}
