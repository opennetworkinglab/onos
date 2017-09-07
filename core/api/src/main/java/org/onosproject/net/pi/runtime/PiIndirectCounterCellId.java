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
import org.onlab.util.Identifier;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.pi.runtime.PiCounterType.INDIRECT;

/**
 * Identifier of an indirect counter cell in a protocol-independent pipeline.
 */
@Beta
public final class PiIndirectCounterCellId extends Identifier<String> implements PiCounterCellId {

    private final PiCounterId counterId;
    private final long index;

    private PiIndirectCounterCellId(PiCounterId counterId, long index) {
        super(counterId.toString() + "[" + index + "]");
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
    public static PiIndirectCounterCellId of(PiCounterId counterId, long index) {
        checkNotNull(counterId);
        checkArgument(counterId.type() == INDIRECT, "Counter ID must be of type INDIRECT");
        checkArgument(index >= 0, "Index must be a positive integer");
        return new PiIndirectCounterCellId(counterId, index);
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
    public PiCounterId counterId() {
        return counterId;
    }

    @Override
    public PiCounterType type() {
        return INDIRECT;
    }
}
