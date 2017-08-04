/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.tl1;

import com.google.common.annotations.Beta;

import java.util.Optional;

/**
 * Representation of a TL1 command, which is sent from the controller to a network element.
 *
 * The following shows the typical TL1 command structure:
 *      {@literal VERB-MODIFIER:<tid>:<aid>:<ctag>::parameter-list;}
 *
 * The ctag must be a non-zero decimal number consisting of not more than six characters,
 * and is assumed to be a unique message identifier per device.
 */
@Beta
public interface Tl1Command {

    /**
     * Minimum CTAG value.
     */
    int MIN_CTAG = 0;

    /**
     * Maximum CTAG value.
     */
    int MAX_CTAG = 999999;

    /**
     * Returns the verb of the command.
     *
     * @return the verb
     */
    String verb();

    /**
     * Returns the modifier of the command.
     *
     * @return the modifier
     */
    String modifier();

    /**
     * Returns the optional target identifier (tid).
     *
     * @return the tid
     */
    Optional<String> tid();

    /**
     * Returns the optional access identifier (aid).
     *
     * @return the aid
     */
    Optional<String> aid();

    /**
     * Returns the correlation tag (ctag).
     *
     * @return correlation tag
     */
    int ctag();

    /**
     * Returns the optional parameters.
     *
     * @return the parameters
     */
    Optional<String> parameters();

    /**
     * TL1 command builder.
     *
     * @param <T> builder implementation type
     */
    interface Builder<T extends Builder<T>> {
        /**
         * Assigns a verb to this TL1 command.
         *
         * @param verb a verb
         * @return this
         */
        T withVerb(String verb);

        /**
         * Assigns a modifier to this TL1 command.
         *
         * @param modifier a modifier
         * @return this
         */
        T withModifier(String modifier);

        /**
         * Assigns a target identifier to this TL1 command.
         *
         * @param tid a tid
         * @return this
         */
        T forTid(String tid);

        /**
         * Assigns an access identifier to this TL1 command.
         *
         * @param aid an aid
         * @return this
         */
        T withAid(String aid);

        /**
         * Assigns a correlation tag to this TL1 command.
         *
         * @param ctag a ctag
         * @return this
         */
        T withCtag(int ctag);

        /**
         * Assigns parameters to this TL1 command.
         *
         * @param parameters the parameters
         * @return this
         */
        T withParameters(String parameters);

        /**
         * Builds a TL1 command.
         *
         * @return the TL1 command
         */
        Tl1Command build();
    }
}
