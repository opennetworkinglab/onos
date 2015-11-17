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
package org.onosproject.faultmanagement.web;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;

/**
 * Mock codec context for use in codec unit tests.
 */
public class AlarmCodecContext implements CodecContext {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Class<?>, JsonCodec> codecs = new ConcurrentHashMap<>();

    /**
     * Constructs a new mock codec context.
     */
    public AlarmCodecContext() {
        codecs.clear();
        registerCodec(Alarm.class, new AlarmCodec());

    }

    @Override
    public ObjectMapper mapper() {
        return mapper;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getService(Class<T> serviceClass) {
        // TODO
        return null;
    }

    /**
     * Registers the specified JSON codec for the given entity class.
     *
     * @param entityClass entity class
     * @param codec       JSON codec
     * @param <T>         entity type
     */
    public <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec) {
        codecs.putIfAbsent(entityClass, codec);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> JsonCodec<T> codec(Class<T> entityClass) {
        return codecs.get(entityClass);
    }
}
