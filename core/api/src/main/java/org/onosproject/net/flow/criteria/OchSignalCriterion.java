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

import org.onosproject.net.OchSignal;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OCh (Optical Channel) signal criterion.
 * This criterion is based on the specification of "OFPXMT_EXP_OCH_SIGID" in
 * Open Networking Foundation "Optical Transport Protocol Extension Version 1.0", but
 * defined in protocol agnostic way.
 */
public final class OchSignalCriterion implements Criterion {

    private final OchSignal lambda;

    /**
     * Create an instance with the specified OCh signal.
     *
     * @param lambda OCh signal
     */
    OchSignalCriterion(OchSignal lambda) {
        this.lambda = checkNotNull(lambda);
    }

    @Override
    public Type type() {
        return Type.OCH_SIGID;
    }

    /**
     * Returns the OCh signal to match.
     *
     * @return the OCh signal to match
     */
    public OchSignal lambda() {
        return lambda;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), lambda);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OchSignalCriterion)) {
            return false;
        }
        final OchSignalCriterion that = (OchSignalCriterion) obj;
        return Objects.equals(this.lambda, that.lambda);
    }

    @Override
    public String toString() {
        return type().toString() + SEPARATOR + lambda;
    }
}
