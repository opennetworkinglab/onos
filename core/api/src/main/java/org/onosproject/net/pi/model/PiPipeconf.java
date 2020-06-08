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

import com.google.common.annotations.Beta;
import org.onosproject.net.driver.Behaviour;

import java.io.InputStream;
import java.util.Collection;
import java.util.Optional;

/**
 * Configuration of a protocol-independent pipeline that includes a pipeline model, a collection of pipeline-specific
 * behaviour implementations, and extensions.
 */
@Beta
public interface PiPipeconf {

    /**
     * Returns the identifier of this pipeline configuration.
     *
     * @return a identifier
     */
    PiPipeconfId id();

    /**
     * Returns the pipeline model.
     *
     * @return a pipeline model
     */
    PiPipelineModel pipelineModel();

    /**
     * Returns the fingerprint of pipeconf.
     *
     * @return a fingerprint
     */
    long fingerprint();

    /**
     * Returns all pipeline-specific behaviour interfaces defined by this configuration.
     *
     * @return a collection of behaviours
     */
    Collection<Class<? extends Behaviour>> behaviours();

    /**
     * Returns the implementation class for the given behaviour, if present.
     *
     * @param behaviour behaviour interface
     * @return implementation class
     */
    Optional<Class<? extends Behaviour>> implementation(Class<? extends Behaviour> behaviour);

    /**
     * Indicates whether or not the pipeconf supports the specified class of behaviour.
     *
     * @param behaviourClass behaviour class
     * @return true if behaviour is supported
     */
    boolean hasBehaviour(Class<? extends Behaviour> behaviourClass);

    /**
     * Returns, if present, an input stream pointing at the beginning of a file representing a device-specific or
     * control protocol-specific extension of this configuration. For example, if requesting a target-specific P4
     * binary, this will return the same bytes produced by the P4 compiler.
     *
     * @param type extension type
     * @return extension input stream
     */
    Optional<InputStream> extension(ExtensionType type);

    /**
     * Type of extension of a protocol-independent pipeline configuration.
     */
    enum ExtensionType {

        /**
         * The P4Info as returned by the p4c compiler in text format.
         */
        P4_INFO_TEXT,

        /**
         * BMv2 JSON configuration.
         */
        BMV2_JSON,

        /**
         * Mellanox Spectrum configuration binary.
         */
        SPECTRUM_BIN,

        /**
         * Barefoot's Tofino configuration binary.
         */
        TOFINO_BIN,

        /**
         * Barefoot's Tofino context JSON.
         */
        TOFINO_CONTEXT_JSON,

        /**
         * Stratum Fixed Pipeline Model (FPM) pipeline configuration binary.
         */
        STRATUM_FPM_BIN,

        /**
         * CPU port file in UTF 8 encoding.
         */
        // TODO: consider a better way to get the CPU port in the interpreter
        // (see FabricInterpreter.java mapLogicalPortNumber). Perhaps using
        // pipeconf annotations?
        CPU_PORT_TXT,

        /**
         * Raw device config.
         */
        RAW_DEVICE_CONFIG
    }
}
