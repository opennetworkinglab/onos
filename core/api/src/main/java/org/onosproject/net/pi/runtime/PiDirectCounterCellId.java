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
import com.google.common.base.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.onosproject.net.pi.runtime.PiCounterType.DIRECT;

/**
 * Identifier of a direct counter cell of a protocol-independent pipeline.
 */
@Beta
public final class PiDirectCounterCellId implements PiCounterCellId {

    private final PiCounterId counterId;
    private final PiTableEntry tableEntry;

    private PiDirectCounterCellId(PiCounterId counterId, PiTableEntry tableEntry) {
        this.counterId = counterId;
        this.tableEntry = tableEntry;
    }

    /**
     * Returns a direct counter cell identifier for the given counter identifier and table entry.
     *
     * @param counterId  counter identifier
     * @param tableEntry table entry
     * @return direct counter cell identifier
     */
    public static PiDirectCounterCellId of(PiCounterId counterId, PiTableEntry tableEntry) {
        checkNotNull(counterId);
        checkNotNull(tableEntry);
        checkArgument(counterId.type() == DIRECT, "Counter ID must be of type DIRECT");
        return new PiDirectCounterCellId(counterId, tableEntry);
    }

    /**
     * Returns the table entry associated with this cell identifier.
     *
     * @return cell table entry
     */
    public PiTableEntry tableEntry() {
        return tableEntry;
    }

    @Override
    public PiCounterId counterId() {
        return counterId;
    }

    @Override
    public PiCounterType type() {
        return DIRECT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiDirectCounterCellId)) {
            return false;
        }
        PiDirectCounterCellId that = (PiDirectCounterCellId) o;
        return Objects.equal(counterId, that.counterId) &&
                Objects.equal(tableEntry, that.tableEntry);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(counterId, tableEntry);
    }

    @Override
    public String toString() {
        return format("%s[{%s}]", counterId, tableEntry);
    }
}
