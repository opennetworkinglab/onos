/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.impl;

import com.eclipsesource.json.Json;
import com.google.common.collect.Maps;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Mock pipeconf implementation.
 */
public class MockPipeconf implements PiPipeconf {

    private static final String PIPECONF_ID = "org.project.pipeconf.default";
    private static final String JSON_PATH = "/org/onosproject/net/pi/impl/default.json";

    private final PiPipeconfId id;
    private final PiPipelineModel pipelineModel;
    protected final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;

    public MockPipeconf() throws IOException {
        this.id = new PiPipeconfId(PIPECONF_ID);
        this.pipelineModel = loadDefaultModel();
        this.behaviours = Maps.newHashMap();

        behaviours.put(PiPipelineInterpreter.class, MockInterpreter.class);
    }

    static PiPipelineModel loadDefaultModel() throws IOException {
        return Bmv2PipelineModelParser.parse(Json.parse(new BufferedReader(new InputStreamReader(
                MockPipeconf.class.getResourceAsStream(JSON_PATH)))).asObject());
    }

    @Override
    public PiPipeconfId id() {
        return this.id;
    }

    @Override
    public PiPipelineModel pipelineModel() {
        return pipelineModel;
    }

    @Override
    public Collection<Class<? extends Behaviour>> behaviours() {
        return behaviours.keySet();
    }

    @Override
    public Optional<Class<? extends Behaviour>> implementation(Class<? extends Behaviour> behaviour) {
        return Optional.ofNullable(behaviours.get(behaviour));
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return behaviours.containsKey(behaviourClass);
    }

    @Override
    public Optional<ByteBuffer> extension(ExtensionType type) {
        return Optional.empty();
    }
}
