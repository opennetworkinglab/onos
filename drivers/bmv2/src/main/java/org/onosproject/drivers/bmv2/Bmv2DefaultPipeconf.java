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

package org.onosproject.drivers.bmv2;

import com.eclipsesource.json.Json;
import com.google.common.collect.ImmutableMap;
import org.onosproject.bmv2.model.Bmv2PipelineModelParser;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineInterpreter;
import org.onosproject.net.pi.model.PiPipelineModel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Implementation of the default pipeconf for a BMv2 device.
 */
public final class Bmv2DefaultPipeconf implements PiPipeconf {

    private static final String PIPECONF_ID = "bmv2-default-pipeconf";
    private static final String JSON_PATH = "/default.json";
    private static final String P4INFO_PATH = "/default.p4info";

    private final PiPipeconfId id;
    private final PiPipelineModel pipelineModel;
    private final InputStream jsonConfigStream = this.getClass().getResourceAsStream(JSON_PATH);
    private final InputStream p4InfoStream = this.getClass().getResourceAsStream(P4INFO_PATH);
    private final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;

    Bmv2DefaultPipeconf() {
        this.id = new PiPipeconfId(PIPECONF_ID);
        try {
            this.pipelineModel = Bmv2PipelineModelParser.parse(
                    Json.parse(new BufferedReader(new InputStreamReader(jsonConfigStream))).asObject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.behaviours = ImmutableMap.of(
                PiPipelineInterpreter.class, Bmv2DefaultInterpreter.class
                // TODO: reuse default single table pipeliner.
        );
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
    public Optional<InputStream> extension(ExtensionType type) {

        switch (type) {
            case BMV2_JSON:
                return Optional.of(jsonConfigStream);
            case P4_INFO_TEXT:
                return Optional.of(p4InfoStream);
            default:
                return Optional.empty();
        }
    }
}
