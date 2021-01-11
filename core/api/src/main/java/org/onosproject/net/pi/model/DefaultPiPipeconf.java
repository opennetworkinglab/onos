/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import org.onosproject.net.driver.Behaviour;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Default pipeconf implementation.
 */
public final class DefaultPiPipeconf implements PiPipeconf {

    private final PiPipeconfId id;
    private final PiPipelineModel pipelineModel;
    private final long fingerprint;
    private final Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours;
    private final Map<ExtensionType, URL> extensions;

    private DefaultPiPipeconf(PiPipeconfId id, PiPipelineModel pipelineModel, long fingerprint,
                              Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours,
                              Map<ExtensionType, URL> extensions) {
        this.id = id;
        this.pipelineModel = pipelineModel;
        this.fingerprint = fingerprint;
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
    public long fingerprint() {
        return fingerprint;
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
        if (extensions.containsKey(type)) {
            try {
                return Optional.of(extensions.get(type).openStream());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return Optional.empty();
        }
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
        private ImmutableMap.Builder<ExtensionType, URL> extensionMapBuilder = ImmutableMap.builder();

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
         * @param type extension type
         * @param url  url pointing at the extension file
         * @return this
         */
        public Builder addExtension(ExtensionType type, URL url) {
            checkNotNull(type);
            checkNotNull(url);
            checkArgument(checkUrl(url), format("Extension url %s seems to be empty/non existent", url.toString()));
            extensionMapBuilder.put(type, url);
            return this;
        }

        private boolean checkUrl(URL url) {
            try {
                int byteCount = url.openStream().available();
                return byteCount > 0;
            } catch (IOException e) {
                return false;
            }
        }

        /**
         * Creates a new pipeconf.
         *
         * @return pipeconf instance
         */
        public PiPipeconf build() {
            checkNotNull(id);
            checkNotNull(pipelineModel);

            Map<ExtensionType, URL> extensions = extensionMapBuilder.build();
            Map<Class<? extends Behaviour>, Class<? extends Behaviour>> behaviours =
                    behaviourMapBuilder.build();
            return new DefaultPiPipeconf(
                    id, pipelineModel, generateFingerprint(extensions),
                    behaviours, extensions);
        }

        private long generateFingerprint(Map<ExtensionType, URL> extensions) {
            Collection<Integer> hashes = new ArrayList<>();
            for (ExtensionType extensionType : ExtensionType.values()) {
                try {
                    // Get the extension if present and then hash the content
                    URL extUrl = extensions.get(extensionType);
                    if (extUrl != null) {
                        HashingInputStream hin = new HashingInputStream(
                                Hashing.crc32(), extUrl.openStream());
                        //noinspection StatementWithEmptyBody
                        while (hin.read() != -1) {
                            // Do nothing. Reading all input stream to update hash.
                        }
                        hashes.add(hin.hash().asInt());
                    }
                } catch (IOException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            //  FIXME: how to include behaviours in the hash?
            int low = Arrays.hashCode(hashes.toArray());
            int high = pipelineModel.hashCode();
            return ByteBuffer.allocate(8).putInt(high).putInt(low).getLong(0);
        }
    }
}
