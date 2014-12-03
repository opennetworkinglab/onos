/*
 * Copyright 2014 Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the JSON codec brokering service.
 */
@Component(immediate = true)
@Service
public class CodecManager implements CodecService {

    private static Logger log = LoggerFactory.getLogger(CodecManager.class);

    private final Map<Class<?>, JsonCodec> codecs = new ConcurrentHashMap<>();

    @Activate
    public void activate() {
        codecs.clear();
        registerCodec(Annotations.class, new AnnotationsCodec());
        registerCodec(Device.class, new DeviceCodec());
        registerCodec(Port.class, new PortCodec());
        registerCodec(ConnectPoint.class, new ConnectPointCodec());
        registerCodec(Link.class, new LinkCodec());
        log.info("Started");
    }

    @Deactivate
    public void deativate() {
        codecs.clear();
        log.info("Stopped");
    }

    @Override
    public Set<Class<?>> getCodecs() {
        return ImmutableSet.copyOf(codecs.keySet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonCodec<T> getCodec(Class<T> entityClass) {
        return codecs.get(entityClass);
    }

    @Override
    public <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec) {
        codecs.putIfAbsent(entityClass, codec);
    }

    @Override
    public void unregisterCodec(Class<?> entityClass) {
        codecs.remove(entityClass);
    }

}
