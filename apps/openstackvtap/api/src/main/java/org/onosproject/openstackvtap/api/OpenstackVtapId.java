/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Immutable representation of an openstack vtap identifier.
 */
public final class OpenstackVtapId {

    /**
     * Represents either no vtap, or an unspecified vtap.
     */
    public static final OpenstackVtapId NONE = new OpenstackVtapId(null);

    private final UUID uuid;

    // Public construction is prohibited
    private OpenstackVtapId(UUID uuid) {
        this.uuid = uuid;
    }

    // Default constructor for serialization
    private OpenstackVtapId() {
        this.uuid = null;
    }

    /**
     * Returns an unique UUID.
     *
     * @return UUID
     */
    public UUID uuid() {
        return uuid;
    }

    /**
     * Creates a vtap identifier using the supplied UUID.
     *
     * @param uuid vtap UUID
     * @return vtap identifier
     */
    public static OpenstackVtapId vtapId(UUID uuid) {
        return new OpenstackVtapId(uuid);
    }

    /**
     * Creates a vtap identifier using the supplied string format of UUID.
     *
     * @param uuidString vtap UUID
     * @return vtap identifier
     */
    public static OpenstackVtapId vtapId(String uuidString) {
        try {
            return vtapId(UUID.fromString(uuidString));
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid UUID string: " + uuidString);
        }
    }

    /**
     * Creates a openstack vtap id using random uuid.
     *
     * @return openstack vtap identifier
     */
    public static OpenstackVtapId vtapId() {
        return new OpenstackVtapId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return uuid.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OpenstackVtapId) {
            final OpenstackVtapId other = (OpenstackVtapId) obj;
            return Objects.equals(this.uuid, other.uuid);
        }
        return false;
    }
}
