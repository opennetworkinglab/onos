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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;

/**
 * Type of runtime entity of a protocol-independent pipeline.
 */
@Beta
public enum PiEntityType {
    /**
     * Table entry.
     */
    TABLE_ENTRY("table entry"),

    /**
     * Action profile group.
     */
    ACTION_PROFILE_GROUP("action profile group"),

    /**
     * Action profile member.
     */
    ACTION_PROFILE_MEMBER("action profile member"),

    /**
     * Meter cell config.
     */
    METER_CELL_CONFIG("meter cell config"),

    /**
     * Register cell.
     */
    REGISTER_CELL("register cell"),

    /**
     * Counter cell.
     */
    COUNTER_CELL("counter cell"),

    /**
     * Packet Replication Engine (PRE) entry.
     */
    PRE_ENTRY("PRE entry");

    private final String humanReadableName;

    PiEntityType(String humanReadableName) {
        this.humanReadableName = humanReadableName;
    }

    /**
     * Returns a human readable representation of this PI entity type (useful
     * for logging).
     *
     * @return string
     */
    public String humanReadableName() {
        return humanReadableName;
    }
}
