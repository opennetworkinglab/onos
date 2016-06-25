/*
 * Copyright 2015-present Open Networking Laboratory
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

import java.net.URI;

/**
 * Serializer for {@link URI}.
 */
public class UriSerializer extends Serializer<URI> {

    /**
     * Creates {@link URI} serializer instance.
     */
    public UriSerializer() {
        super(false);
    }

    @Override
    public void write(Kryo kryo, Output output, URI object) {
        output.writeString(object.toString());
    }

    @Override
    public URI read(Kryo kryo, Input input, Class<URI> type) {
        return URI.create(input.readString());
    }
}
