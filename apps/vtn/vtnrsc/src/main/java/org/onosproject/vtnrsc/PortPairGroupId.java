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
 * Representation of a Port Pair Group ID.
 */
public final class PortPairGroupId extends Identifier<UUID> {
    /**
     * Private constructor for port pair group id.
     *
     * @param id UUID id of port pair group
     */
    private PortPairGroupId(UUID id) {
        super(checkNotNull(id, "Port pair group id can not be null"));
    }

    /**
     * Returns newly created port pair group id object.
     *
     * @param id port pair group id in UUID
     * @return object of port pair group id
     */
    public static PortPairGroupId of(UUID id) {
        return new PortPairGroupId(id);
    }

    /**
     * Returns newly created port pair group id object.
     *
     * @param id port pair group id in string
     * @return object of port pair group id
     */
    public static PortPairGroupId of(String id) {
        return new PortPairGroupId(UUID.fromString(id));
    }

    /**
     * Returns the value of port pair group id.
     *
     * @return port pair group id
     */
    public UUID value() {
        return identifier;
    }
}
