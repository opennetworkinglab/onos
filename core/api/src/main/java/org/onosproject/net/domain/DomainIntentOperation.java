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
package org.onosproject.net.domain;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of an intent operation on a network domain.
 */
public class DomainIntentOperation {

    /**
     * Type of domain intent operations.
     * <p>
     * TODO MODIFY to be added here.
     */
    public enum Type {
        ADD,
        REMOVE
    }

    private final DomainIntent intent;
    private final Type type;

    /**
     * Creates a domain intent operation using the supplied information.
     *
     * @param intent the domain intent
     * @param type the operation type
     */
    public DomainIntentOperation(DomainIntent intent, Type type) {
        this.intent = checkNotNull(intent, "Intent cannot be null");
        this.type = checkNotNull(type, "Operation type cannot be null");
    }

    /**
     * Returns the type of operation.
     *
     * @return type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the domain intent.
     *
     * @return domain intent
     */
    public DomainIntent intent() {
        return intent;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("intent", intent)
                .add("type", type)
                .toString();
    }
}
