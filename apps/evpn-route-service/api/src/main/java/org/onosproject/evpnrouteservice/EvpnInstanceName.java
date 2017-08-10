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

package org.onosproject.evpnrouteservice;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents the EvpnInstanceName.
 */
public final class EvpnInstanceName {
    private final String evpnName;

    /**
     * Constructor to initialize the parameters.
     *
     * @param evpnName EvpnInstanceName
     */
    private EvpnInstanceName(String evpnName) {
        this.evpnName = evpnName;
    }

    /**
     * Creates instance of EvpnInstanceName.
     *
     * @param evpnName evpnName
     * @return evpnInstanceName
     */
    public static EvpnInstanceName evpnName(String evpnName) {
        return new EvpnInstanceName(evpnName);
    }

    /**
     * Get vpn instance name.
     *
     * @return evpnName
     */
    public String getEvpnName() {
        return evpnName;
    }

    @Override
    public int hashCode() {
        return Objects.hash(evpnName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof EvpnInstanceName) {
            EvpnInstanceName other = (EvpnInstanceName) obj;
            return Objects.equals(this.evpnName, other.evpnName);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("evpnName", evpnName).toString();
    }
}
