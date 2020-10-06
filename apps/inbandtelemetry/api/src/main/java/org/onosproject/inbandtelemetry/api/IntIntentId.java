/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.inbandtelemetry.api;

import org.onlab.util.Identifier;

/**
 * Representation of a IntIntent ID.
 */
public final class IntIntentId extends Identifier<Long> {
    private IntIntentId(long id) {
        super(id);
    }

    /**
     * Creates an IntIntent ID from a given long value.
     *
     * @param id long value
     * @return IntIntent ID
     */
    public static IntIntentId valueOf(long id) {
        return new IntIntentId(id);
    }

    /**
     * Gets the IntIntent ID value.
     *
     * @return IntIntent ID value as long
     */
    public long value() {
        return this.identifier;
    }

    @Override
    public String toString() {
        return Long.toString(this.identifier);
    }
}
