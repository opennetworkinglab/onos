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

package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Collection;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiActionProfileModel for P4Runtime.
 */
final class P4ActionProfileModel implements PiActionProfileModel {

    private final PiActionProfileId id;
    private final ImmutableSet<PiTableId> tables;
    private final boolean hasSelector;
    private final long size;
    private final int maxGroupSize;

    P4ActionProfileModel(PiActionProfileId id,
                         ImmutableSet<PiTableId> tables, boolean hasSelector,
                         long size, int maxGroupSize) {
        this.id = id;
        this.tables = tables;
        this.hasSelector = hasSelector;
        this.size = size;
        this.maxGroupSize = maxGroupSize;
    }

    @Override
    public PiActionProfileId id() {
        return id;
    }

    @Override
    public Collection<PiTableId> tables() {
        return tables;
    }

    @Override
    public boolean hasSelector() {
        return hasSelector;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public int maxGroupSize() {
        return maxGroupSize;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, tables, hasSelector, size, maxGroupSize);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4ActionProfileModel other = (P4ActionProfileModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.tables, other.tables)
                && Objects.equals(this.hasSelector, other.hasSelector)
                && Objects.equals(this.size, other.size)
                && Objects.equals(this.maxGroupSize, other.maxGroupSize);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("tables", tables)
                .add("hasSelector", hasSelector)
                .add("size", size)
                .add("maxGroupSize", maxGroupSize)
                .toString();
    }
}
