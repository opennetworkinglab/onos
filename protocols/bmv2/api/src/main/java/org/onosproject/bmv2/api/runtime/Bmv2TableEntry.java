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

package org.onosproject.bmv2.api.runtime;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An entry of a match-action table in a BMv2 device.
 */
@Beta
public final class Bmv2TableEntry {

    private static final int NO_PRIORITY_VALUE = -1;
    private static final int NO_TIMEOUT_VALUE = -1;

    private final String tableName;
    private final Bmv2MatchKey matchKey;
    private final Bmv2Action action;
    private final int priority;
    private final double timeout;

    private Bmv2TableEntry(String tableName, Bmv2MatchKey matchKey,
                           Bmv2Action action, int priority, double timeout) {
        this.tableName = tableName;
        this.matchKey = matchKey;
        this.action = action;
        this.priority = priority;
        this.timeout = timeout;
    }

    /**
     * Returns a new BMv2 table entry builder.
     *
     * @return a new builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns the name of the table where this entry is installed.
     *
     * @return table name
     */
    public final String tableName() {
        return this.tableName;
    }

    /**
     * Returns the match key of this table entry.
     *
     * @return match key
     */
    public final Bmv2MatchKey matchKey() {
        return matchKey;
    }

    /**
     * Returns the action of this table entry.
     *
     * @return action
     */
    public final Bmv2Action action() {
        return action;
    }

    /**
     * Returns true is the entry has a valid priority.
     *
     * @return true if priority is set, false elsewhere
     */
    public final boolean hasPriority() {
        return this.priority != NO_PRIORITY_VALUE;
    }

    /**
     * Return the priority of this table entry.
     *
     * @return priority
     */
    public final int priority() {
        return priority;
    }

    /**
     * Returns true is this table entry has a valid timeout.
     *
     * @return true if timeout is set, false elsewhere
     */
    public final boolean hasTimeout() {
        return this.timeout != NO_PRIORITY_VALUE;
    }

    /**
     * Returns the timeout (in fractional seconds) of this table entry.
     *
     * @return a timeout vale (in fractional seconds)
     */
    public final double timeout() {
        return timeout;
    }

    @Override
    public final int hashCode() {
        return Objects.hash(matchKey, action, priority, timeout);
    }

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2TableEntry other = (Bmv2TableEntry) obj;
        return Objects.equals(this.matchKey, other.matchKey)
                && Objects.equals(this.action, other.action)
                && Objects.equals(this.priority, other.priority)
                && Objects.equals(this.timeout, other.timeout);
    }

    @Override
    public final String toString() {
        return com.google.common.base.MoreObjects.toStringHelper(this)
                .addValue(matchKey)
                .addValue(action)
                .add("priority", priority)
                .add("timeout", timeout)
                .toString();
    }

    public static final class Builder {

        private String tableName;
        private Bmv2MatchKey matchKey;
        private Bmv2Action action;
        private int priority = NO_PRIORITY_VALUE;
        private double timeout = NO_TIMEOUT_VALUE;

        private Builder() {
            // hide constructor
        }

        /**
         * Sets the table name.
         *
         * @param tableName a string value
         * @return this
         */
        public Builder withTableName(String tableName) {
            this.tableName = checkNotNull(tableName, "table name cannot be null");
            return this;
        }

        /**
         * Sets the match key.
         *
         * @param matchKey a match key value
         * @return this
         */
        public Builder withMatchKey(Bmv2MatchKey matchKey) {
            this.matchKey = checkNotNull(matchKey, "match key cannot be null");
            return this;
        }

        /**
         * Sets the action.
         *
         * @param action an action value
         * @return this
         */
        public Builder withAction(Bmv2Action action) {
            this.action = checkNotNull(action, "action cannot be null");
            return this;
        }

        public Builder withPriority(int priority) {
            checkArgument(priority >= 0, "priority cannot be negative");
            this.priority = priority;
            return this;
        }

        /**
         * Sets the timeout.
         *
         * @param timeout a timeout value in fractional seconds
         * @return this
         */
        public Builder withTimeout(double timeout) {
            checkArgument(timeout > 0, "timeout must be a positive non-zero value");
            this.timeout = timeout;
            return this;
        }

        /**
         * Build the table entry.
         *
         * @return a new table entry object
         */
        public Bmv2TableEntry build() {
            return new Bmv2TableEntry(tableName, matchKey, action, priority,
                                      timeout);

        }
    }
}
