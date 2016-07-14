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
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Representation of a table entry installed on a BMv2 device.
 */
@Beta
public final class Bmv2ParsedTableEntry {
    private final long entryId;
    private final Bmv2MatchKey matchKey;
    private final Bmv2Action action;
    private final int priority;

    /**
     * Creates a new parsed table entry.
     *
     * @param entryId  a long value
     * @param matchKey a match key
     * @param action   an action
     * @param priority an integer value
     */
    public Bmv2ParsedTableEntry(long entryId, Bmv2MatchKey matchKey, Bmv2Action action, int priority) {
        this.entryId = entryId;
        this.matchKey = matchKey;
        this.action = action;
        this.priority = priority;
    }

    /**
     * Returns the entry ID.
     *
     * @return a long value
     */
    public long entryId() {
        return entryId;
    }

    /**
     * Returns the match key.
     *
     * @return a match key object
     */
    public Bmv2MatchKey matchKey() {
        return matchKey;
    }

    /**
     * Returns the action.
     *
     * @return an action object
     */
    public Bmv2Action action() {
        return action;
    }

    /**
     * Returns the priority.
     *
     * @return an integer value
     */
    public int getPriority() {
        return priority;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(entryId, matchKey, action, priority);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ParsedTableEntry other = (Bmv2ParsedTableEntry) obj;
        return Objects.equal(this.entryId, other.entryId)
                && Objects.equal(this.matchKey, other.matchKey)
                && Objects.equal(this.action, other.action)
                && Objects.equal(this.priority, other.priority);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("entryId", entryId)
                .add("matchKey", matchKey)
                .add("action", action)
                .toString();
    }
}
