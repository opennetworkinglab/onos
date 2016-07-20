/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.meter;

import com.google.common.collect.ImmutableList;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Immutable collection of meter operation to be used between
 * core and provider layers of group subsystem.
 *
 */
public final class MeterOperations {
    private final List<MeterOperation> operations;

    /**
     * Creates a immutable list of meter operation.
     *
     * @param operations list of meter operation
     */
    public MeterOperations(List<MeterOperation> operations) {
        this.operations = ImmutableList.copyOf(checkNotNull(operations));
    }

    /**
     * Returns immutable list of Meter operation.
     *
     * @return list of Meter operation
     */
    public List<MeterOperation> operations() {
        return operations;
    }

}
