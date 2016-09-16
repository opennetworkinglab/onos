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
package org.onosproject.vpls.config;

import com.google.common.collect.ImmutableSet;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration of a VPLS Network.
 */
public class VplsNetworkConfig {
    private final String name;
    private final Set<String> ifaces;

    /**
     * Creates a new VPLS configuration.
     *
     * @param name the VPLS name
     * @param ifaces the interfaces associated with the VPLS
     */
    public VplsNetworkConfig(String name, Set<String> ifaces) {
        this.name = checkNotNull(name);
        this.ifaces = checkNotNull(ImmutableSet.copyOf(ifaces));
    }

    /**
     * Returns the name of the VPLS.
     *
     * @return the name of the VPLS
     */
    public String name() {
        return name;
    }

    /**
     * Returns the name of interfaces associated with the VPLS.
     *
     * @return a set of interface names associated with the VPLS
     */
    public Set<String> ifaces() {
        return ImmutableSet.copyOf(ifaces);
    }

    /**
     * States if a given interface is part of a VPLS.
     *
     * @param iface the interface attached to a VPLS
     * @return true if the interface is associated to the VPLS; false otherwise
     */
    public boolean isAttached(String iface) {
        return ifaces.stream().anyMatch(i -> i.equals(iface));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VplsNetworkConfig) {
            final VplsNetworkConfig that = (VplsNetworkConfig) obj;
            return Objects.equals(this.name, that.name) &&
                    Objects.equals(this.ifaces, that.ifaces);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, ifaces);
    }
}
