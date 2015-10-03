/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a physical network identity.
 */
public final class PhysicalNetwork {

    private final String physicalNetwork;

    // Public construction is prohibited
    private PhysicalNetwork(String physicalNetwork) {
        checkNotNull(physicalNetwork, "PhysicalNetwork cannot be null");
        this.physicalNetwork = physicalNetwork;
    }

    /**
     * Creates a PhysicalNetwork object.
     *
     * @param physicalNetwork physical network
     * @return physical network
     */
    public static PhysicalNetwork physicalNetwork(String physicalNetwork) {
        return new PhysicalNetwork(physicalNetwork);
    }

    /**
     * Returns a physicalNetwork.
     *
     * @return physical network
     */
    public String physicalNetwork() {
        return physicalNetwork;
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalNetwork);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PhysicalNetwork) {
            final PhysicalNetwork that = (PhysicalNetwork) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.physicalNetwork,
                                      that.physicalNetwork);
        }
        return false;
    }

    @Override
    public String toString() {
        return physicalNetwork;
    }

}
