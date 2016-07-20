/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.flowapi;

/**
 * Ext target prefix class class.
 */
public interface ExtTarget extends ExtFlowTypes {

    /**
     * Returns the ExtType.
     *
     * @return the ExtType
     */
    ExtType type();

    /**
     * Returns the local speaker prefix list.
     *
     * @return the prefix list
     */
    ExtPrefix localSpeaker();

    /**
     * Returns the remote speaker prefix list.
     *
     * @return the prefix list
     */
    ExtPrefix remoteSpeaker();

    /**
     * Returns whether this prefix list is an exact match to the prefix list given
     * in the argument.
     *
     * @param prefix other prefix to match against
     * @return true if the prefix are an exact match, otherwise false
     */
    boolean exactMatch(ExtTarget prefix);

    /**
     * A prefix builder..
     */
    interface Builder {

        /**
         * Assigns the ExtType to this object.
         *
         * @param type the prefix
         * @return this the builder object
         */
        Builder setType(ExtType type);

        /**
         * Add the local speaker prefix to this object.
         *
         * @param localSpeaker list of local speakers
         * @return this the builder object
         */
        Builder setLocalSpeaker(ExtPrefix localSpeaker);

        /**
         * Add the remote speaker prefix to this object.
         *
         * @param remoteSpeaker list of remote speakers
         * @return this the builder object
         */
        Builder setRemoteSpeaker(ExtPrefix remoteSpeaker);

        /**
         * Builds a prefix object.
         *
         * @return a port chain.
         */
        ExtTarget build();
    }
}
