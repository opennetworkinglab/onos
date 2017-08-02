/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.d.config.sync.operation;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.d.config.sync.operation.SetRequest.Change.Operation;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ResourceId;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

// One SetRequest is expected to be a transaction, all-or-nothing
/**
 * Collection of changes about a single Device,
 * intended to be applied to the Device transactionally.
 */
@Beta
public final class SetRequest {

    private final Collection<Change> changes;

    SetRequest(Collection<Change> changes) {
        this.changes = ImmutableList.copyOf(changes);
    }

    public Collection<Change> changes() {
        return changes;
    }

    public Collection<Pair<Operation, ResourceId>> subjects() {
        return changes.stream()
                    .map(c -> Pair.of(c.op(), c.path()))
                    .collect(ImmutableList.toImmutableList());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SetRequest) {
            SetRequest that = (SetRequest) obj;
            return Objects.equals(this.changes, that.changes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(changes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("changes", changes)
                .toString();
    }
    public static SetRequest.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final List<Change> changes = new ArrayList<>();

        /**
         * Returns changes contained in this builder.
         *
         * @return unmodifiable list view of Changes
         */
        public List<Change> changes() {
            return Collections.unmodifiableList(changes);
        }

        /**
         * Adds request to remove specified {@code path}.
         *
         * @param path resource path relative to device root node
         * @return self
         */
        public SetRequest.Builder delete(ResourceId path) {
            changes.add(Change.delete(path));
            return this;
        }

        /**
         * Adds request to replace specified {@code path} with specified {@code val}.
         *
         * @param path resource path relative to device root node
         * @param val  resource value
         * @return self
         */
        public SetRequest.Builder replace(ResourceId path, DataNode val) {
            changes.add(Change.replace(path, val));
            return this;
        }

        /**
         * Adds request to update/merge specified {@code val} to the {@code path}.
         *
         * @param path resource path relative to device root node
         * @param val  resource value
         * @return self
         */
        public SetRequest.Builder update(ResourceId path, DataNode val) {
            changes.add(Change.update(path, val));
            return this;
        }

        public SetRequest build() {
            return new SetRequest(changes);
        }
    }

    public static final class Change {

        public enum Operation {

            // Note: equivalent to remove in netconf
            /**
             * Request to delete specified {@code path}.
             * If path does not exist, it is silently ignored.
             */
            DELETE,
            // Note: equivalent to replace in netconf
            /**
             * Request to replace specified {@code path} with specified {@code val}.
             */
            REPLACE,
            // Note: equivalent to merge in netconf
            /**
             * Request to update/merge specified {@code val} to the {@code path}.
             */
            UPDATE
        }

        private final Operation op;
        private final ResourceId path;
        private final Optional<DataNode> val;

        public static Change delete(ResourceId path) {
            return new Change(Operation.DELETE, path, Optional.empty());
        }

        public static Change replace(ResourceId path, DataNode val) {
            return new Change(Operation.REPLACE, path, Optional.of(val));
        }

        public static Change update(ResourceId path, DataNode val) {
            return new Change(Operation.UPDATE, path, Optional.of(val));
        }

        Change(Operation op, ResourceId path, Optional<DataNode> val) {
            this.op = checkNotNull(op);
            this.path = checkNotNull(path);
            this.val = checkNotNull(val);
        }

        /**
         * Returns type of change operation.
         *
         * @return Operation
         */
        public Operation op() {
            return op;
        }

        /**
         * Returns resource path to be changed.
         *
         * @return resource path relative to device root node
         */
        public ResourceId path() {
            return path;
        }

        /**
         * Returns the {@code val} specified.
         *
         * @return {@code val}
         * @throws NoSuchElementException if this object represent {@code DELETE} op.
         */
        public DataNode val() {
            return val.get();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof Change) {
                Change that = (Change) obj;
                return Objects.equals(this.op, that.op) &&
                       Objects.equals(this.path, that.path) &&
                       Objects.equals(this.val, that.val);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(op, path, val);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .add("op", op)
                    .add("path", path)
                    .add("val", val)
                    .toString();
        }
    }
}