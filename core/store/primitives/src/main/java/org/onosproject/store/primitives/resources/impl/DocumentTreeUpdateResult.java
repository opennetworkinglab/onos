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

package org.onosproject.store.primitives.resources.impl;

import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.Versioned;

import com.google.common.base.MoreObjects;

/**
 * Result of a document tree node update operation.
 * <p>
 * Both old and new values are accessible along with a status of update.
 *
 * @param <V> value type
 */
public class DocumentTreeUpdateResult<V> {

    public enum Status {
        /**
         * Indicates a successful update.
         */
        OK,

        /**
         * Indicates a noop i.e. existing and new value are both same.
         */
        NOOP,

        /**
         * Indicates a failed update due to a write lock.
         */
        WRITE_LOCK,

        /**
         * Indicates a failed update due to a invalid path.
         */
        INVALID_PATH,

        /**
         * Indicates a failed update due to a illegal modification attempt.
         */
        ILLEGAL_MODIFICATION,
    }

    private final DocumentPath path;
    private final Status status;
    private final Versioned<V> oldValue;
    private final Versioned<V> newValue;

    public DocumentTreeUpdateResult(DocumentPath path,
            Status status,
            Versioned<V> newValue,
            Versioned<V> oldValue) {
        this.status = status;
        this.path = path;
        this.newValue = newValue;
        this.oldValue = oldValue;
    }

    public static <V> DocumentTreeUpdateResult<V> invalidPath(DocumentPath path) {
        return new DocumentTreeUpdateResult<>(path, Status.INVALID_PATH, null, null);
    }

    public static <V> DocumentTreeUpdateResult<V> illegalModification(DocumentPath path) {
        return new DocumentTreeUpdateResult<>(path, Status.ILLEGAL_MODIFICATION, null, null);
    }

    public Status status() {
        return status;
    }

    public DocumentPath path() {
        return path;
    }

    public Versioned<V> oldValue() {
        return oldValue;
    }

    public Versioned<V> newValue() {
        return this.newValue;
    }

    public boolean updated() {
        return status == Status.OK;
    }

    public boolean created() {
        return updated() && oldValue == null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("path", path)
                .add("status", status)
                .add("newValue", newValue)
                .add("oldValue", oldValue)
                .toString();
    }
}
