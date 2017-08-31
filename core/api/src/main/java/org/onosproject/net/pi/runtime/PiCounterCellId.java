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
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Identifier of a counter cell of a protocol-independent pipeline.
 */
@Beta
public final class PiCounterCellId extends Identifier<String> {

    private final PiCounterId counterId;
    private final long index;

    private PiCounterCellId(PiCounterId counterId, long index) {
        super(counterId.id() + "[" + index + "]");
        this.counterId = counterId;
        this.index = index;
    }

    /**
     * Returns a counter cell identifier for the given counter identifier and index.
     *
     * @param counterId counter identifier
     * @param index     index
     * @return counter cell identifier
     */
    public static PiCounterCellId of(PiCounterId counterId, long index) {
        checkNotNull(counterId);
        checkArgument(index >= 0, "Index must be a positive integer");
        return new PiCounterCellId(counterId, index);
    }

    /**
     * Returns the counter identifier of this cell.
     *
     * @return counter identifier
     */
    public PiCounterId counterId() {
        return counterId;
    }

    /**
     * Returns the index of this cell.
     *
     * @return cell index
     */
    public long index() {
        return index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiCounterCellId)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PiCounterCellId that = (PiCounterCellId) o;
        return index == that.index &&
                Objects.equal(counterId, that.counterId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), counterId, index);
    }
}
