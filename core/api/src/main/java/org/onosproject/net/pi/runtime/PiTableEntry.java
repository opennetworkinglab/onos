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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Optional;
import java.util.OptionalInt;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a table entry in a protocol-independent pipeline.
 */
@Beta
public final class PiTableEntry implements PiEntity {

    private static final int NO_PRIORITY = -1;
    private static final double NO_TIMEOUT = -1;

    private final PiTableId tableId;
    private final PiMatchKey matchKey;
    private final PiTableAction tableAction;
    private final boolean isDefaultAction;
    private final long cookie;
    private final int priority;
    private final double timeout;
    private final PiCounterCellData counterData;

    private PiTableEntry(PiTableId tableId, PiMatchKey matchKey,
                         PiTableAction tableAction, boolean isDefaultAction,
                         long cookie, int priority, double timeout, PiCounterCellData data) {
        this.tableId = tableId;
        this.matchKey = matchKey;
        this.tableAction = tableAction;
        this.isDefaultAction = isDefaultAction;
        this.cookie = cookie;
        this.priority = priority;
        this.timeout = timeout;
        this.counterData = data;
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
     * Returns the match key of this table entry.
     * <p>
     * If {@link #isDefaultAction()} is {@code true} this method returns the
     * empty match key ({@link PiMatchKey#EMPTY}).
     *
     * @return match key
     */
    public PiMatchKey matchKey() {
        return matchKey;
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
     * Returns true if this table entry contains the default action for this
     * table, a.k.a. table-miss entry, false otherwise.
     *
     * @return boolean
     */
    public boolean isDefaultAction() {
        return isDefaultAction;
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
     * Returns the priority of this table entry, if present. If the priority
     * value is not present, then this table entry has no explicit priority.
     *
     * @return optional priority
     */
    public OptionalInt priority() {
        return priority == NO_PRIORITY ? OptionalInt.empty() : OptionalInt.of(priority);
    }

    /**
     * Returns the timeout in seconds of this table entry, if present. If the
     * timeout value is not present, then this table entry is meant to be
     * permanent.
     *
     * @return optional timeout value in seconds
     */
    public Optional<Double> timeout() {
        return timeout == NO_TIMEOUT ? Optional.empty() : Optional.of(timeout);
    }

    /**
     * Returns the data of the counter cell associated with this table entry.
     * This method is meaningful only if the table entry was read from the
     * infrastructure device and the table has direct counters, otherwise
     * returns null.
     *
     * @return counter cell data
     */
    public PiCounterCellData counter() {
        return counterData;
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
                cookie == that.cookie &&
                Double.compare(that.timeout, timeout) == 0 &&
                Objects.equal(tableId, that.tableId) &&
                Objects.equal(matchKey, that.matchKey) &&
                Objects.equal(isDefaultAction, that.isDefaultAction) &&
                Objects.equal(tableAction, that.tableAction);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tableId, matchKey, isDefaultAction, tableAction,
                                priority, cookie, timeout);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tableId", tableId)
                .add("matchKey", isDefaultAction ? "DEFAULT-ACTION" : matchKey)
                .add("tableAction", tableActionToString(tableAction))
                .add("priority", priority == NO_PRIORITY ? "N/A" : String.valueOf(priority))
                .add("timeout", timeout == NO_TIMEOUT ? "PERMANENT" : String.valueOf(timeout))
                .toString();
    }

    private String tableActionToString(PiTableAction tableAction) {
        if (tableAction == null) {
            return "null";
        }
        switch (tableAction.type()) {
            case ACTION_PROFILE_GROUP_ID:
                return "ACT_PROF_GROUP:" + ((PiActionProfileGroupId) tableAction).id();
            case ACTION_PROFILE_MEMBER_ID:
                return "ACT_PROF_MEMBER:" + ((PiActionProfileMemberId) tableAction).id();
            case ACTION_SET:
                return "ACTION_SET:" + tableAction.toString();
            case ACTION:
            default:
                return tableAction.toString();
        }
    }

    /**
     * Returns a table entry builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.TABLE_ENTRY;
    }

    @Override
    public PiTableEntryHandle handle(DeviceId deviceId) {
        return PiTableEntryHandle.of(deviceId, this);
    }

    public static final class Builder {

        private PiTableId tableId;
        private PiMatchKey matchKey = PiMatchKey.EMPTY;
        private PiTableAction tableAction;
        private long cookie = 0;
        private int priority = NO_PRIORITY;
        private double timeout = NO_TIMEOUT;
        private PiCounterCellData counterData;

        private Builder() {
            // Hides constructor.
        }

        /**
         * Sets the table identifier for this entry.
         *
         * @param tableId table identifier
         * @return this
         */
        public Builder forTable(PiTableId tableId) {
            this.tableId = checkNotNull(tableId, "Table ID cannot be null");
            return this;
        }

        /**
         * Sets the action of this table entry.
         *
         * @param tableAction table action
         * @return this
         */
        public Builder withAction(PiTableAction tableAction) {
            this.tableAction = checkNotNull(tableAction, "Action cannot be null");
            return this;
        }

        /**
         * Sets the match key of this table entry. By default, the match key is
         * {@link PiMatchKey#EMPTY}, i.e. any match.
         *
         * @param matchKey match key
         * @return this
         */
        public Builder withMatchKey(PiMatchKey matchKey) {
            this.matchKey = checkNotNull(matchKey, "Match key cannot be null");
            return this;
        }

        /**
         * Sets the cookie, i.e. a controller-specific metadata.
         *
         * @param cookie cookie
         * @return this
         */
        public Builder withCookie(long cookie) {
            this.cookie = cookie;
            return this;
        }

        /**
         * Sets the priority of this table entry.
         *
         * @param priority priority
         * @return this
         */
        public Builder withPriority(int priority) {
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
        public Builder withTimeout(double seconds) {
            checkArgument(seconds > 0, "Timeout must be greater than zero.");
            this.timeout = seconds;
            return this;
        }

        /**
         * Sets the counter cell data of this table entry.
         *
         * @param data counter cell data
         * @return this
         */
        public Builder withCounterCellData(PiCounterCellData data) {
            this.counterData = checkNotNull(data, "Counter cell data cannot be null");
            return this;
        }

        /**
         * Builds the table entry.
         *
         * @return a new table entry
         */
        public PiTableEntry build() {
            checkNotNull(tableId);
            checkNotNull(matchKey);
            final boolean isDefaultAction = matchKey.equals(PiMatchKey.EMPTY);
            return new PiTableEntry(tableId, matchKey, tableAction,
                                    isDefaultAction, cookie, priority, timeout, counterData);
        }
    }
}
