/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.net.OchSignalType;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OCh (Optical Channel) signal type criterion.
 */
public class OchSignalTypeCriterion implements Criterion {

    private final OchSignalType signalType;

    /**
     * Creates a criterion with the specified value.
     *
     * @param signalType OCh signal type
     */
    OchSignalTypeCriterion(OchSignalType signalType) {
        this.signalType = checkNotNull(signalType);
    }

    @Override
    public Type type() {
        return Type.OCH_SIGTYPE;
    }

    /**
     * Returns the OCh signal type to match.
     *
     * @return the OCh signal type to match
     */
    public OchSignalType signalType() {
        return signalType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), signalType);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OchSignalTypeCriterion)) {
            return false;
        }
        final OchSignalTypeCriterion that = (OchSignalTypeCriterion) obj;
        return Objects.equals(this.signalType, that.signalType);
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + signalType;
    }
}
