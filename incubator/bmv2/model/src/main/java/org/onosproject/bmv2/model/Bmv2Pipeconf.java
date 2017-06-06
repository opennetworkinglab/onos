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
package org.onosproject.bmv2.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiPipeconfId;
import org.onosproject.net.pi.model.PiPipelineModel;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 pipeline configuration (pipeconf).
 */
public final class Bmv2Pipeconf implements PiPipeconf {
    private final PiPipeconfId id;
    private final Bmv2PipelineModel pipelineModel;
    private final Set<Class<? extends Behaviour>> behaviours;
    private final Map<ExtensionType, ByteBuffer> extensions;

    /**
     * Builds a new BMv2 pipeline configuration (pipeconf) by given information.
     *
     * @param id the pipeconf id
     * @param pipelineModel the pipeline model
     * @param behaviours the behaviors of the pipeline
     * @param extensions the extensions of the pipeline
     */
    public Bmv2Pipeconf(PiPipeconfId id,
                        Bmv2PipelineModel pipelineModel,
                        Set<Class<? extends Behaviour>> behaviours,
                        Map<ExtensionType, ByteBuffer> extensions) {
        checkNotNull(id, "Pipeconf Id can't be null");
        checkNotNull(pipelineModel, "Pipeline model can't be null");

        this.id = id;
        this.pipelineModel = pipelineModel;
        this.behaviours = behaviours == null ? ImmutableSet.of() : behaviours;
        this.extensions = extensions == null ? Maps.newHashMap() : extensions;
    }

    @Override
    public PiPipeconfId id() {
        return id;
    }

    @Override
    public PiPipelineModel pipelineModel() {
        return pipelineModel;
    }

    @Override
    public Collection<Class<? extends Behaviour>> behaviours() {
        return behaviours;
    }

    @Override
    public Optional<Class<? extends Behaviour>> implementation(Class<? extends Behaviour> behaviour) {
        return behaviours.stream()
                .filter(behaviour::isAssignableFrom)
                .findAny();
    }

    @Override
    public boolean hasBehaviour(Class<? extends Behaviour> behaviourClass) {
        return behaviours.stream()
                .anyMatch(behaviourClass::isAssignableFrom);
    }

    @Override
    public Optional<ByteBuffer> extension(ExtensionType type) {
        return Optional.ofNullable(extensions.get(type));
    }
}
