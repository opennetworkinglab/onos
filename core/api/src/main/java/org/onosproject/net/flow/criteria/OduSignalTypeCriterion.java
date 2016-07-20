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
package org.onosproject.net.flow.criteria;

import org.onosproject.net.OduSignalType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of ODU (Optical channel Data Unit) signal Type criterion.
 * This criterion is based on the specification of "OFPXMT_EXP_ODU_SIGTYPE" in
 * Open Networking Foundation "Optical Transport Protocol Extension Version 1.0", but
 * defined in protocol agnostic way.
 */
public final class OduSignalTypeCriterion implements Criterion {

    private final OduSignalType signalType;

    /**
     * Create an instance with the specified ODU signal Type.
     *
     * @param signalType - ODU signal Type
     */
    OduSignalTypeCriterion(OduSignalType signalType) {
        this.signalType = checkNotNull(signalType);
    }

    @Override
    public Type type() {
        return Type.ODU_SIGTYPE;
    }

    /**
     * Returns the ODU Signal Type to match.
     *
     * @return the ODU signal Type to match
     */
    public OduSignalType signalType() {
        return signalType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), signalType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OduSignalTypeCriterion)) {
            return false;
        }
        final OduSignalTypeCriterion that = (OduSignalTypeCriterion) obj;
        return Objects.equals(this.signalType, that.signalType);
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + signalType;
    }
}
