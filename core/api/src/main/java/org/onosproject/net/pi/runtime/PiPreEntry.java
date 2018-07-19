/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;

/**
 * Configuration entry of a Packet Replication Engine (PRE) of
 * protocol-independent pipeline.
 */
@Beta
public interface PiPreEntry extends PiEntity {

    /**
     * Type of PRE entry.
     */
    enum PiPreEntryType {
        /**
         * Multicast group entry.
         */
        MULTICAST_GROUP,
        /**
         * Clone session entry.
         */
        CLONE_SESSION
    }

    /**
     * Returns the type of this PRE entry.
     *
     * @return PRE entry type
     */
    PiPreEntryType preEntryType();
}
