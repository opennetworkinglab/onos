/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Table entry in a protocol-independent pipeline.
 */
@Beta
public final class PiTableEntry {

    private static final int NO_PRIORITY = -1;
    private static final double NO_TIMEOUT = -1;

    private final PiTableId tableId;
    private final Collection<PiFieldMatch> fieldMatches;
    private final PiTableAction tableAction;
    private final long cookie;
    private final int priority;
    private final double timeout;

    private PiTableEntry(PiTableId tableId, Map<PiHeaderFieldId, PiFieldMatch> fieldMatches,
                         PiTableAction tableAction, long cookie, int priority, double timeout) {
        this.tableId = tableId;
        this.fieldMatches = ImmutableSet.copyOf(fieldMatches.values());
        this.tableAction = tableAction;
        this.cookie = cookie;
        this.priority = priority;
        this.timeout = timeout;
    }

    /**
     * Returns the table where this entry is installed.
     *
     * @return table identifier
     */
    public PiTableId table() {
        return tableId;
    }

    /**
     * Returns an immutable view of the field matches of this table entry.
     *
     * @return collection of field matches
     */
    public Collection<PiFieldMatch> fieldMatches() {
        return fieldMatches;
    }

    /**
     * Returns the action of this table entry.
     *
     * @return action
     */
    public PiTableAction action() {
        return tableAction;
    }

    /**
     * Returns the cookie of this table entry.
     *
     * @return cookie
     */
    public long cookie() {
        return cookie;
    }

    /**
     * Returns the priority of this table entry, if present.
     * If the priority value is not present, then this table entry has no explicit priority.
     *
     * @return optional priority
     */
    public Optional<Integer> priority() {
        return priority == NO_PRIORITY ? Optional.empty() : Optional.of(priority);
    }

    /**
     * Returns the timeout in seconds of this table entry, if present.
     * If the timeout value is not present, then this table entry is meant to be permanent.
     *
     * @return optional timeout value in seconds
     */
    public Optional<Double> timeout() {
        return timeout == NO_TIMEOUT ? Optional.empty() : Optional.of(timeout);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiTableEntry that = (PiTableEntry) o;
        return priority == that.priority &&
                Double.compare(that.timeout, timeout) == 0 &&
                Objects.equal(tableId, that.tableId) &&
                Objects.equal(fieldMatches, that.fieldMatches) &&
                Objects.equal(tableAction, that.tableAction);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tableId, fieldMatches, tableAction, priority, timeout);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tableId", tableId)
                .add("fieldMatches", fieldMatches)
                .add("tableAction", tableAction)
                .add("priority", priority == NO_PRIORITY ? "N/A" : String.valueOf(priority))
                .add("timeout", timeout == NO_TIMEOUT ? "PERMANENT" : String.valueOf(timeout))
                .toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private PiTableId tableId;
        private Map<PiHeaderFieldId, PiFieldMatch> fieldMatches = Maps.newHashMap();
        private PiTableAction tableAction;
        private long cookie = 0;
        private int priority = NO_PRIORITY;
        private double timeout = NO_TIMEOUT;

        private Builder() {
            // Hides constructor.
        }

        /**
         * Sets the table identifier for this entry.
         *
         * @param tableId table identifier
         * @return this
         */
        Builder forTable(PiTableId tableId) {
            this.tableId = checkNotNull(tableId);
            return this;
        }

        /**
         * Sets the action of this table entry.
         *
         * @param tableAction table action
         * @return this
         */
        Builder withAction(PiTableAction tableAction) {
            this.tableAction = checkNotNull(tableAction);
            return this;
        }

        /**
         * Adds one field match to this table entry.
         *
         * @param fieldMatch field match
         * @return this
         */
        Builder withFieldMatch(PiFieldMatch fieldMatch) {
            this.fieldMatches.put(fieldMatch.fieldId(), fieldMatch);
            return this;
        }

        /**
         * Adds many field matches to this table entry.
         *
         * @param fieldMatches collection of field matches
         * @return this
         */
        Builder withFieldMatches(Collection<PiFieldMatch> fieldMatches) {
            fieldMatches.forEach(f -> this.fieldMatches.put(f.fieldId(), f));
            return this;
        }

        /**
         * Sets the cookie, i.e. a controller-specific metadata.
         *
         * @param cookie cookie
         * @return this
         */
        Builder withCookie(long cookie) {
            this.cookie = cookie;
            return this;
        }

        /**
         * Sets the priority of this table entry.
         *
         * @param priority priority
         * @return this
         */
        Builder withPriority(int priority) {
            checkArgument(priority >= 0, "Priority must be a positive integer.");
            this.priority = priority;
            return this;
        }

        /**
         * Sets the timeout of this table entry.
         *
         * @param seconds timeout in seconds
         * @return this
         */
        Builder withTimeout(double seconds) {
            checkArgument(seconds > 0, "Timeout must be greater than zero.");
            this.timeout = seconds;
            return this;
        }

        /**
         * Builds the table entry.
         *
         * @return a new table entry
         */
        PiTableEntry build() {
            checkNotNull(tableId);
            checkNotNull(tableAction);
            return new PiTableEntry(tableId, fieldMatches, tableAction, cookie, priority, timeout);
        }
    }
}
