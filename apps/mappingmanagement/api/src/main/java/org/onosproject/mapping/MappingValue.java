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
package org.onosproject.mapping;

import org.onosproject.mapping.actions.MappingAction;

import java.util.List;

/**
 * Abstraction of value of mapping information.
 */
public interface MappingValue {

    /**
     * Obtains a mapping action.
     *
     * @return a mapping action
     */
    MappingAction action();

    /**
     * Obtains a collection of mapping instructions.
     *
     * @return a collection of mapping instructions
     */
    List<MappingTreatment> treatments();

    interface Builder {

        /**
         * Specifies a mapping action.
         *
         * @param action mapping action
         * @return a mapping value builder
         */
        Builder withAction(MappingAction action);

        /**
         * Specifies a collection of mapping treatment.
         *
         * @param treatment a mapping treatment
         * @return a mapping value builder
         */
        Builder add(MappingTreatment treatment);

        /**
         * Builds an immutable mapping value.
         *
         * @return a mapping value
         */
        MappingValue build();
    }
}
