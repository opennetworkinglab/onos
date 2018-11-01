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
package org.onosproject.net.flowobjective;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents a next action specified by next id.
 */
public final class IdNextTreatment implements NextTreatment {
    private final int nextId;

    /**
     * Constructs IdNextTreatment.
     *
     * @param nextId next id
     */
    private IdNextTreatment(int nextId) {
        this.nextId = nextId;
    }

    /**
     * Returns next id.
     *
     * @return next id
     */
    public int nextId() {
        return nextId;
    }

    /**
     * Returns an instance of IdNextTreatment with given next id.
     *
     * @param nextId next id
     * @return an instance of IdNextTreatment
     */
    public static IdNextTreatment of(int nextId) {
        return new IdNextTreatment(nextId);
    }

    @Override
    public Type type() {
        return Type.ID;
    }
    @Override
    public int hashCode() {
        return Objects.hash(nextId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IdNextTreatment) {
            final IdNextTreatment other = (IdNextTreatment) obj;
            return this.nextId == other.nextId;
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("nextId", nextId)
                .toString();
    }
}
