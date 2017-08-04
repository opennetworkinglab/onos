/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.ovsdb.rfc.notation;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;
import java.util.Set;

import org.onosproject.ovsdb.rfc.notation.json.OvsdbSetSerializer;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

/**
 * OvsdbSet is either an atom, representing a set with exactly one element, or
 * a 2-element JSON array that represents a database set value.
 *
 */
@JsonSerialize(using = OvsdbSetSerializer.class)
public final class OvsdbSet {

    private final Set set;

    /**
     * OvsdbSet constructor.
     * @param set java.util.Set
     */
    private OvsdbSet(Set set) {
        checkNotNull(set, "set cannot be null");
        this.set = set;
    }

    /**
     * Returns set.
     * @return set
     */
    public Set set() {
        return set;
    }

    /**
     * convert Set into OvsdbSet.
     * @param set java.util.Set
     * @return OvsdbSet
     */
    public static OvsdbSet ovsdbSet(Set set) {
        return new OvsdbSet(set);
    }

    @Override
    public int hashCode() {
        return set.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbSet) {
            final OvsdbSet other = (OvsdbSet) obj;
            return Objects.equals(this.set, other.set);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("set", set).toString();
    }
}
