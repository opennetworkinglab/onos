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

import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable representation of a floating IP identifier.
 */
public final class FloatingIpId extends Identifier<UUID> {
    // Public construction is prohibited
    private FloatingIpId(UUID floatingIpId) {
        super(checkNotNull(floatingIpId, "floatingIpId cannot be null"));
    }

    /**
     * Creates a floating IP identifier.
     *
     * @param floatingIpId the UUID id of floating IP identifier
     * @return object of floating IP identifier
     */
    public static FloatingIpId of(UUID floatingIpId) {
        return new FloatingIpId(floatingIpId);
    }

    /**
     * Creates a floating IP identifier.
     *
     * @param floatingIpId the floating IP identifier in string
     * @return object of floating IP identifier
     */
    public static FloatingIpId of(String floatingIpId) {
        return new FloatingIpId(UUID.fromString(floatingIpId));
    }

    /**
     * Returns the floating IP identifier.
     *
     * @return the floating IP identifier
     */
    public UUID floatingIpId() {
        return identifier;
    }
}
