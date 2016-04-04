/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.pcep.controller;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of capabilities supported by client.
 */
public class ClientCapability {
    private boolean pceccCapability;
    private boolean statefulPceCapability;
    private boolean pcInstantiationCapability;

    /**
     * Creates new instance of client capability.
     *
     * @param pceccCapability represents PCECC capability
     * @param statefulPceCapability represents stateful PCE capability
     * @param pcInstantiationCapability represents PC initiation capability
     */
    public ClientCapability(boolean pceccCapability, boolean statefulPceCapability, boolean pcInstantiationCapability) {
        this.pceccCapability = pceccCapability;
        this.statefulPceCapability = statefulPceCapability;
        this.pcInstantiationCapability = pcInstantiationCapability;
    }

    /**
     * Obtains PCECC capability.
     *
     * @return true if client supports PCECC capability otherwise false
     */
    public boolean pceccCapability() {
        return pceccCapability;
    }

    /**
     * Obtains stateful PCE capability.
     *
     * @return true if client supports stateful PCE capability otherwise false
     */
    public boolean statefulPceCapability() {
        return statefulPceCapability;
    }

    /**
     * Obtains PC initiation capability.
     *
     * @return true if client supports PC initiation capability otherwise false
     */
    public boolean pcInstantiationCapability() {
        return pcInstantiationCapability;
    }

    @Override
    public int hashCode() {
        return Objects.hash(pceccCapability, statefulPceCapability, pcInstantiationCapability);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ClientCapability) {
            ClientCapability other = (ClientCapability) obj;
            return Objects.equals(pceccCapability, other.pceccCapability)
                    && Objects.equals(statefulPceCapability, other.statefulPceCapability)
                    && Objects.equals(pcInstantiationCapability, other.pcInstantiationCapability);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("pceccCapability", pceccCapability)
                .add("statefulPceCapability", statefulPceCapability)
                .add("pcInstantiationCapability", pcInstantiationCapability)
                .toString();
    }
}