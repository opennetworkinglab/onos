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
package org.onosproject.app.vtnrsc;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a physicalnetwork identity.
 */
public final class PhysicalNetwork {

    private final String physicalnetwork;

    // Public construction is prohibited
    private PhysicalNetwork(String physicalnetwork) {
        checkNotNull(physicalnetwork, "Physicalnetwork cannot be null");
        this.physicalnetwork = physicalnetwork;
    }

    /**
     * Creates a network id using the physicalnetwork.
     *
     * @param physicalnetwork network String
     * @return physicalnetwork
     */
    public static PhysicalNetwork physicalNetwork(String physicalnetwork) {
        return new PhysicalNetwork(physicalnetwork);
    }

    /**
     *
     * @return physicalnetwork
     */
    public String physicalnetwork() {
        return physicalnetwork;
    }

    @Override
    public int hashCode() {
        return Objects.hash(physicalnetwork);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PhysicalNetwork) {
            final PhysicalNetwork that = (PhysicalNetwork) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.physicalnetwork,
                                      that.physicalnetwork);
        }
        return false;
    }

    @Override
    public String toString() {
        return physicalnetwork;
    }

}
