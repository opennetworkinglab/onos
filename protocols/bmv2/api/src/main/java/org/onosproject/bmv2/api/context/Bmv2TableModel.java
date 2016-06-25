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

package org.onosproject.bmv2.api.context;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;

import java.util.List;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A BMv2 table model.
 */
@Beta
public final class Bmv2TableModel {

    private final String name;
    private final int id;
    private final String matchType;
    private final String type;
    private final int maxSize;
    private final boolean hasCounters;
    private final boolean hasTimeouts;
    private final List<Bmv2TableKeyModel> keys;
    private final Set<Bmv2ActionModel> actions;

    /**
     * Creates a new table model.
     *
     * @param name           name
     * @param id             id
     * @param matchType      match type
     * @param type           type
     * @param maxSize        max number of entries
     * @param withCounters   if table has counters
     * @param supportTimeout if table supports aging
     * @param keys           list of match keys
     * @param actions        list of actions
     */
    protected Bmv2TableModel(String name, int id, String matchType, String type,
                             int maxSize, boolean withCounters, boolean supportTimeout,
                             List<Bmv2TableKeyModel> keys, Set<Bmv2ActionModel> actions) {
        this.name = name;
        this.id = id;
        this.matchType = matchType;
        this.type = type;
        this.maxSize = maxSize;
        this.hasCounters = withCounters;
        this.hasTimeouts = supportTimeout;
        this.keys = keys;
        this.actions = actions;
    }

    /**
     * Returns the name of this table.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Returns the id of this table.
     *
     * @return an integer value
     */
    public int id() {
        return id;
    }

    /**
     * Return the match type of this table.
     *
     * @return a string value
     */
    public String matchType() {
        return matchType;
    }

    /**
     * Return the match type of this table.
     *
     * @return a string value
     */
    public String type() {
        return type;
    }

    /**
     * Returns the maximum number of entries supported by this table.
     *
     * @return an integer value
     */
    public int maxSize() {
        return maxSize;
    }

    /**
     * Returns true if this table has counters, false otherwise.
     *
     * @return a boolean value
     */
    public boolean hasCounters() {
        return hasCounters;
    }

    /**
     * Returns true if this table supports aging, false otherwise.
     *
     * @return a boolean value
     */
    public boolean hasTimeouts() {
        return hasTimeouts;
    }

    /**
     * Returns the list of match keys supported by this table.
     * The list is ordered accordingly to the model's table definition.
     *
     * @return a list of match keys
     */
    public List<Bmv2TableKeyModel> keys() {
        return keys;
    }

    /**
     * Returns the set of actions supported by this table.
     *
     * @return a list of actions
     */
    public Set<Bmv2ActionModel> actions() {
        return actions;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, id, matchType, type, maxSize, hasCounters,
                                hasTimeouts, keys, actions);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2TableModel other = (Bmv2TableModel) obj;
        return Objects.equal(this.name, other.name)
                && Objects.equal(this.id, other.id)
                && Objects.equal(this.matchType, other.matchType)
                && Objects.equal(this.type, other.type)
                && Objects.equal(this.maxSize, other.maxSize)
                && Objects.equal(this.hasCounters, other.hasCounters)
                && Objects.equal(this.hasTimeouts, other.hasTimeouts)
                && Objects.equal(this.keys, other.keys)
                && Objects.equal(this.actions, other.actions);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("id", id)
                .add("matchType", matchType)
                .add("type", type)
                .add("maxSize", maxSize)
                .add("hasCounters", hasCounters)
                .add("hasTimeouts", hasTimeouts)
                .add("keys", keys)
                .add("actions", actions)
                .toString();
    }

}