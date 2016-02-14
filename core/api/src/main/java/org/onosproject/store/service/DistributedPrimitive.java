/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.store.service;

import java.util.concurrent.CompletableFuture;

import org.onosproject.core.ApplicationId;

/**
 * Interface for all distributed primitives.
 */
public interface DistributedPrimitive {

    /**
     * Type of distributed primitive.
     */
    public enum Type {
        /**
         * Map with strong consistency semantics.
         */
        CONSISTENT_MAP,

        /**
         * Map with eventual consistency semantics.
         */
        EVENTUALLY_CONSISTENT_MAP,

        /**
         * distributed set.
         */
        SET,

        /**
         * atomic counter.
         */
        COUNTER,

        /**
         * Atomic value.
         */
        VALUE,

        /**
         * Distributed queue.
         */
        QUEUE,

        /**
         * Leader elector.
         */
        LEADER_ELECTOR,

        /**
         * Transaction Context.
         */
        TRANSACTION_CONTEXT
    }

    static final long DEFAULT_OPERTATION_TIMEOUT_MILLIS = 5000L;

    /**
     * Returns the name of this primitive.
     * @return name
     */
    String name();

    /**
     * Returns the type of primitive.
     * @return primitive type
     */
    Type primitiveType();

    /**
     * Returns the application owning this primitive.
     * @return application id
     */
    default ApplicationId applicationId() {
        return null;
    }

    /**
     * Purges state associated with this primitive.
     * <p>
     * Implementations can override and provide appropriate clean up logic for purging
     * any state state associated with the primitive. Whether modifications made within the
     * destroy method have local or global visibility is left unspecified.
     * @return {@code CompletableFuture} that is completed when the operation completes
     */
    default CompletableFuture<Void> destroy() {
        return CompletableFuture.completedFuture(null);
    }
}
