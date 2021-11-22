/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;

import java.util.Optional;

/**
 * An exception indicating a an error happened in the UPF programmable behaviour.
 * Possible errors include the attempted insertion of a malformed flow rule, the
 * reading or writing of an out-of-bounds counter cell, the deletion of a non-existent
 * flow rule, and the attempted insertion of a flow rule into a full table.
 */
@Beta
public class UpfProgrammableException extends Exception {
    private final Type type;
    private final UpfEntityType entityType;

    public enum Type {
        /**
         * The UpfProgrammable did not provide a specific exception type.
         */
        UNKNOWN,
        /**
         * The target entity is at capacity.
         */
        ENTITY_EXHAUSTED,
        /**
         * A provided index was out of range.
         */
        ENTITY_OUT_OF_RANGE,
        /**
         * The UpfProgrammable implementation doesn't support the operation.
         */
        UNSUPPORTED_OPERATION
    }

    /**
     * Creates a new exception for the given message.
     *
     * @param message message
     */
    public UpfProgrammableException(String message) {
        super(message);
        this.type = Type.UNKNOWN;
        this.entityType = null;
    }

    /**
     * Creates a new exception for the given message and type.
     *
     * @param message exception message
     * @param type    exception type
     */
    public UpfProgrammableException(String message, Type type) {
        super(message);
        this.type = type;
        this.entityType = null;
    }

    /**
     * Creates a new exception for the given message, type and entity type.
     *
     * @param message    exception message
     * @param type       exception type
     * @param entityType entity type
     */
    public UpfProgrammableException(String message, Type type, UpfEntityType entityType) {
        super(message);
        this.type = type;
        this.entityType = entityType;
    }

    /**
     * Get the type of the exception.
     *
     * @return exception type
     */
    public Type getType() {
        return type;
    }

    /**
     * Get the type of the entity that generated the exception.
     *
     * @return entity type
     */
    public Optional<UpfEntityType> getEntityType() {
        return Optional.ofNullable(entityType);
    }
}
