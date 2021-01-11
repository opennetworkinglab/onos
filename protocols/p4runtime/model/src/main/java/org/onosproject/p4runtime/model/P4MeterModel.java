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

import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiTableId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of PiMeterModel for P4Runtime.
 */
final class P4MeterModel implements PiMeterModel {

    private final PiMeterId id;
    private final PiMeterType meterType;
    private final Unit unit;
    private final PiTableId table;
    private final long size;

    P4MeterModel(PiMeterId id, PiMeterType meterType, Unit unit, PiTableId table, long size) {
        this.id = id;
        this.meterType = meterType;
        this.unit = unit;
        this.table = table;
        this.size = size;
    }

    @Override
    public PiMeterId id() {
        return id;
    }

    @Override
    public PiMeterType meterType() {
        return meterType;
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
        return Objects.hash(id, meterType, unit, table, size);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final P4MeterModel other = (P4MeterModel) obj;
        return Objects.equals(this.id, other.id)
                && Objects.equals(this.meterType, other.meterType)
                && Objects.equals(this.unit, other.unit)
                && Objects.equals(this.table, other.table)
                && Objects.equals(this.size, other.size);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("meterType", meterType)
                .add("unit", unit)
                .add("table", table)
                .add("size", size)
                .toString();
    }
}
