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
package org.onosproject.store.resource.impl;

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * A set of {@link DiscreteResourceCodec DiscreteResourceCodecs}.
 */
final class Codecs {
    private static final Codecs INSTANCE = new Codecs();

    private final Map<Class<?>, DiscreteResourceCodec<?>> codecs;

    /**
     * Returns an instance of this class.
     *
     * @return instance
     */
    static Codecs getInstance() {
        return INSTANCE;
    }

    private Codecs() {
        this.codecs = new HashMap<>();
        init();
    }

    private void init() {
        codecs.put(PortNumber.class, new PortNumberCodec());
        codecs.put(VlanId.class, new VlanIdCodec());
        codecs.put(MplsLabel.class, new MplsLabelCodec());
    }

    /**
     * Returns if there is a codec available for the specified resource.
     *
     * @param resource resource to be encoded
     * @return true if this class can encode the resource, otherwise false.
     */
    boolean isEncodable(DiscreteResource resource) {
        return resource.valueAs(Object.class)
                .map(Object::getClass)
                .map(codecs::containsKey)
                .orElse(Boolean.FALSE);
    }

    /**
     * Returns the codec for the specified class.
     *
     * @param cls class of an instance to be encoded
     * @return the codec for the class
     */
    DiscreteResourceCodec<?> getCodec(Class<?> cls) {
        return codecs.get(cls);
    }
}
