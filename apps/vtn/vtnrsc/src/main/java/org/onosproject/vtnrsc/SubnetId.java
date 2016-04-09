/*
 * Copyright 2015-present Open Networking Laboratory
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

import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a subnet identifier.
 */
public final class SubnetId extends Identifier<String> {
    // Public construction is prohibited
    private SubnetId(String subnetId) {
        super(checkNotNull(subnetId, "SubnetId cannot be null"));
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
        return identifier;
    }
}
