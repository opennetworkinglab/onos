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

/**
 * Identifier of a counter of a protocol-independent pipeline.
 */
@Beta
public final class PiCounterId {

    private final String name;
    private final PiCounterType type;

    private PiCounterId(String name, PiCounterType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Returns a counter identifier for the given name and type.
     *
     * @param name counter name
     * @param type counter type
     * @return counter identifier
     */
    public static PiCounterId of(String name, PiCounterType type) {
        checkNotNull(name);
        checkNotNull(type);
        checkArgument(!name.isEmpty(), "Name can't be empty");
        return new PiCounterId(name, type);
    }

    /**
     * Returns the name of the counter.
     *
     * @return counter name
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the type of the counter.
     *
     * @return counter type
     */
    public PiCounterType type() {
        return this.type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiCounterId)) {
            return false;
        }
        PiCounterId that = (PiCounterId) o;
        return Objects.equal(name, that.name) &&
                type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, type);
    }

    @Override
    public String toString() {
        return type.name().toLowerCase() + ":" + name;
    }
}
