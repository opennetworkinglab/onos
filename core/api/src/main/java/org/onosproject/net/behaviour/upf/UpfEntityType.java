/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;

/**
 * Type of UPF entity.
 */
@Beta
public enum UpfEntityType {
    INTERFACE("interface"),
    TERMINATION_DOWNLINK("termination_downlink"),
    TERMINATION_UPLINK("termination_uplink"),
    SESSION_DOWNLINK("session_downlink"),
    SESSION_UPLINK("session_downlink"),
    TUNNEL_PEER("tunnel_peer"),
    INGRESS_COUNTER("ingress_counter"),
    EGRESS_COUNTER("egress_counter"),
    COUNTER("counter"),
    APPLICATION("application"),
    SESSION_METER("session_meter"),
    APPLICATION_METER("application_meter"),
    // TODO: slice meter shouldn't be exposed via UpfProgrammable driver behaviour
    //  we should have dedicated driver behaviour for the slicing functionality.
    SLICE_METER("slice_meter");

    private final String humanReadableName;

    UpfEntityType(String humanReadableName) {
        this.humanReadableName = humanReadableName;
    }

    /**
     * Returns a human-readable representation of this UPF entity type (useful
     * for logging).
     *
     * @return string
     */
    public String humanReadableName() {
        return humanReadableName;
    }
}
