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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Immutable representation of a subnet identifier.
 */
public final class SubnetId {

    private final String subnetId;

    // Public construction is prohibited
    private SubnetId(String subnetId) {
        checkNotNull(subnetId, "SubnetId cannot be null");
        this.subnetId = subnetId;
    }

    /**
     * Creates a Subnet identifier.
     *
     * @param subnetId the subnet identifier
     * @return the subnet identifier
     */
    public static SubnetId subnetId(String subnetId) {
        return new SubnetId(subnetId);
    }

    /**
     * Returns the subnet identifier.
     *
     * @return the subnet identifier
     */
    public String subnetId() {
        return subnetId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(subnetId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SubnetId) {
            final SubnetId that = (SubnetId) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.subnetId, that.subnetId);
        }
        return false;
    }

    @Override
    public String toString() {
        return subnetId;
    }
}
