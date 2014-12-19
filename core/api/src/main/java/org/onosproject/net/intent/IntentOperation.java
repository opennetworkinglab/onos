/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;


import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Abstraction of an intent-related operation, e.g. add, remove, replace.
 */
public final class IntentOperation {

    private final Type type;
    private final IntentId intentId;
    private final Intent intent;

    /**
     * Operation type.
     */
    public enum Type {
        /**
         * Indicates that an intent should be added.
         */
        SUBMIT,

        /**
         * Indicates that an intent should be removed.
         */
        WITHDRAW,

        /**
         * Indicates that an intent should be replaced with another.
         */
        REPLACE,

        /**
         * Indicates that an intent should be updated (i.e. recompiled/reinstalled).
         */
        UPDATE,
    }

    /**
     * Creates an intent operation.
     *
     * @param type     operation type
     * @param intentId identifier of the intent subject to the operation
     * @param intent   intent subject
     */
    IntentOperation(Type type, IntentId intentId, Intent intent) {
        this.type = checkNotNull(type);
        this.intentId = checkNotNull(intentId);
        this.intent = intent;
    }

    /**
     * Returns the type of the operation.
     *
     * @return operation type
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the identifier of the intent to which this operation applies.
     *
     * @return intent identifier
     */
    public IntentId intentId() {
        return intentId;
    }

    /**
     * Returns the intent to which this operation applied. For remove,
     * this can be null.
     *
     * @return intent that is the subject of the operation; null for remove
     */
    public Intent intent() {
        return intent;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, intentId, intent);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final IntentOperation other = (IntentOperation) obj;
        return Objects.equals(this.type, other.type) &&
                Objects.equals(this.intentId, other.intentId) &&
                Objects.equals(this.intent, other.intent);
    }


    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", type)
                .add("intentId", intentId)
                .add("intent", intent)
                .toString();
    }
}
