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
package org.onosproject.store.consistent.impl;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Objects;

/**
 * Result of a database update operation.
 *
 * @param <V> return value type
 */
public final class Result<V> {

    public enum Status {
        /**
         * Indicates a successful update.
         */
        OK,

        /**
         * Indicates a failure due to underlying state being locked by another transaction.
         */
        LOCKED
    }

    private final Status status;
    private final V value;

    /**
     * Creates a new Result instance with the specified value with status set to Status.OK.
     *
     * @param <V> result value type
     * @param value result value
     * @return Result instance
     */
    public static <V> Result<V> ok(V value) {
        return new Result<>(value, Status.OK);
    }

    /**
     * Creates a new Result instance with status set to Status.LOCKED.
     *
     * @param <V> result value type
     * @return Result instance
     */
    public static <V> Result<V> locked() {
        return new Result<>(null, Status.LOCKED);
    }

    private Result(V value, Status status) {
        this.value = value;
        this.status = status;
    }

    /**
     * Returns true if this result indicates a successful execution i.e status is Status.OK.
     *
     * @return true if successful, false otherwise
     */
    public boolean success() {
        return status == Status.OK;
    }

    /**
     * Returns the status of database update operation.
     *
     * @return database update status
     */
    public Status status() {
        return status;
    }

    /**
     * Returns the return value for the update.
     *
     * @return value returned by database update. If the status is another
     * other than Status.OK, this returns a null
     */
    public V value() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, status);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Result)) {
            return false;
        }
        Result<V> that = (Result<V>) other;
        return Objects.equals(this.value, that.value) &&
               Objects.equals(this.status, that.status);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("status", status)
                .add("value", value)
                .toString();
    }
}