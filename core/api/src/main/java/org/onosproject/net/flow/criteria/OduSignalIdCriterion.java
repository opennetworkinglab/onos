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

import org.onosproject.net.OduSignalId;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of ODU (Optical channel Data Unit) signal ID signal criterion.
 * This criterion is based on the specification of "OFPXMT_EXP_ODU_SIGID" in
 * Open Networking Foundation "Optical Transport Protocol Extension Version 1.0", but
 * defined in protocol agnostic way.
 */
public final class OduSignalIdCriterion implements Criterion {

    private final OduSignalId oduSignalId;

    /**
     * Create an instance with the specified ODU signal ID.
     *
     * @param oduSignalId - ODU signal ID
     */
    OduSignalIdCriterion(OduSignalId oduSignalId) {
        this.oduSignalId = checkNotNull(oduSignalId);
    }

    @Override
    public Type type() {
        return Type.ODU_SIGID;
    }

    /**
     * Returns the ODU Signal to match.
     *
     * @return the ODU signal to match
     */
    public OduSignalId oduSignalId() {
        return oduSignalId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), oduSignalId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OduSignalIdCriterion)) {
            return false;
        }
        final OduSignalIdCriterion that = (OduSignalIdCriterion) obj;
        return Objects.equals(this.oduSignalId, that.oduSignalId);
    }

    @Override
    public String toString() {
        return  type().toString() + SEPARATOR + oduSignalId;
    }

}
