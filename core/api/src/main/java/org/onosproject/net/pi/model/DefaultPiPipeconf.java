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

package org.onosproject.net.pi.model;

import com.google.common.collect.ImmutableMap;
import org.onosproject.net.driver.Behaviour;

import java.io.InputStream;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default pipeconf implementation.
 */
public final class DefaultPiPipeconf implements PiPipeconf {

    private final PiPipeconfId id;
    private final PiPipelineModel pipelineModel;
    private final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
    private final Map<ExtensionType, InputStream> extensions;

    private DefaultPiPipeconf(PiPipeconfId id, PiPipelineModel pipelineModel,
                              Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours,
                              Map<ExtensionType, InputStream> extensions) {
        this.id = id;
        this.pipelineModel = pipelineModel;
        this.behaviours = behaviours;
        this.extensions = extensions;
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
        return Optional.ofNullable(extensions.get(type));
    }

    /**
     * Returns a new pipeconf builder.
     *
     * @return pipeconf builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of pipeconf implementations.
     */
    public static class Builder {

        private PiPipeconfId id;
        private PiPipelineModel pipelineModel;
        private ImmutableMap.Builder<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviourMapBuilder
                = ImmutableMap.builder();
        private ImmutableMap.Builder<ExtensionType, InputStream> extensionMapBuilder = ImmutableMap.builder();

        /**
         * Sets the identifier of this pipeconf.
         *
         * @param id pipeconf identifier
         * @return this
         */
        public Builder withId(PiPipeconfId id) {
            this.id = id;
            return this;
        }

        /**
         * Sets the pipeline model of this pipeconf.
         *
         * @param model pipeline model
         * @return this
         */
        public Builder withPipelineModel(PiPipelineModel model) {
            this.pipelineModel = model;
            return this;
        }

        /**
         * Adds a behaviour to this pipeconf.
         *
         * @param clazz          behavior interface class
         * @param implementation behavior implementation class
         * @return this
         */
        public Builder addBehaviour(Class<? extends Behaviour> clazz, Class<? extends Behaviour> implementation) {
            checkNotNull(clazz);
            checkNotNull(implementation);
            behaviourMapBuilder.put(clazz, implementation);
            return this;
        }

        /**
         * Adds an extension to this pipeconf.
         *
         * @param type        extension type
         * @param inputStream input stream pointing at the extension
         * @return this
         */
        public Builder addExtension(ExtensionType type, InputStream inputStream) {
            checkNotNull(type);
            checkNotNull(inputStream);
            extensionMapBuilder.put(type, inputStream);
            return this;
        }

        /**
         * Creates a new pipeconf.
         *
         * @return pipeconf instance
         */
        public PiPipeconf build() {
            checkNotNull(id);
            checkNotNull(pipelineModel);
            return new DefaultPiPipeconf(id, pipelineModel, behaviourMapBuilder.build(), extensionMapBuilder.build());
        }

    }
}
