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
package org.onosproject.mapping;

import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.instructions.MappingInstruction;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.instructions.ExtensionTreatment;

import java.util.List;

/**
 * Abstraction of mapping treatment.
 */
public interface MappingTreatment {

    /**
     * Obtains a mapping address.
     *
     * @return a mapping address
     */
    MappingAddress address();

    /**
     * Obtains a collection of mapping instructions.
     *
     * @return a collection of mapping instructions
     */
    List<MappingInstruction> instructions();

    interface Builder {

        /**
         * Specifies a mapping address.
         *
         * @param address mapping address
         * @return a mapping treatment builder
         */
        Builder withAddress(MappingAddress address);

        /**
         * Specifies a collection of mapping instructions.
         *
         * @param instruction a mapping instruction
         * @return a mapping treatment builder
         */
        Builder add(MappingInstruction instruction);

        /**
         * Adds an unicast weight instruction.
         *
         * @param weight unicast weight value
         * @return a mapping treatment builder
         */
        Builder setUnicastWeight(int weight);

        /**
         * Adds an unicast priority instruction.
         *
         * @param priority unicast priority value
         * @return a mapping treatment builder
         */
        Builder setUnicastPriority(int priority);

        /**
         * Adds a multicast weight instruction.
         *
         * @param weight multicast weight value
         * @return a mapping treatment builder
         */
        Builder setMulticastWeight(int weight);

        /**
         * Adds a multicast priority instruction.
         *
         * @param priority multicast priority value
         * @return a mapping treatment builder
         */
        Builder setMulticastPriority(int priority);

        /**
         * uses an extension treatment.
         *
         * @param extension extension treatment
         * @param deviceId  device identifier
         * @return a treatment builder
         */
        Builder extension(ExtensionTreatment extension, DeviceId deviceId);

        /**
         * Builds an immutable mapping treatment.
         *
         * @return a mapping treatment
         */
        MappingTreatment build();
    }
}
