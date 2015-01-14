/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Mock codec context for use in codec unit tests.
 */
public final class MockCodecContext implements CodecContext {

    private ObjectMapper mapper = new ObjectMapper();
    private CodecManager manager = new CodecManager();

    /**
     * Constructs a new mock codec context.
     */
    public MockCodecContext() {
        manager.activate();
    }

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return manager.getCodec(entityClass);
    }

    @Override
    public <T> T get(Class<T> serviceClass) {
        return null;
    }

}
