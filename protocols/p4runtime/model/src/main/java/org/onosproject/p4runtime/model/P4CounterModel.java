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

import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiCounterModel for P4Runtime.
 */
final class P4CounterModel implements PiCounterModel {

    private final PiCounterId id;
    private final PiCounterType counterType;
    private final Unit unit;
    private final PiTableId table;
    private final long size;

    P4CounterModel(PiCounterId id, PiCounterType counterType,
                   Unit unit, PiTableId table, long size) {
        this.id = id;
        this.counterType = counterType;
        this.unit = unit;
        this.table = table;
        this.size = size;
    }

    @Override
    public PiCounterId id() {
        return id;
    }

    @Override
    public PiCounterType counterType() {
        return counterType;
    }

    @Override
    public Unit unit() {
        return unit;
    }

    @Override
    public PiTableId table() {
        return table;
    }

    @Override
    public long size() {
        return size;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, counterType, unit, table, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4CounterModel other = (P4CounterModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.counterType, other.counterType)
                && Objects.equals(this.unit, other.unit)
                && Objects.equals(this.table, other.table)
                && Objects.equals(this.size, other.size);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("counterType", counterType)
                .add("unit", unit)
                .add("table", table)
                .add("size", size)
                .toString();
    }
}
