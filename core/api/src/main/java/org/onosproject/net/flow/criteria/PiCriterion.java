/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.runtime.PiFieldMatch;

import java.util.Collection;
import java.util.StringJoiner;

/**
 * Protocol-indepedent criterion.
 */
@Beta
public final class PiCriterion implements Criterion {

    private final Collection<PiFieldMatch> fieldMatches;

    /**
     * Creates a new protocol-independent criterion for the given match fields.
     *
     * @param fieldMatches fields to match
     */
    PiCriterion(Collection<PiFieldMatch> fieldMatches) {
        this.fieldMatches = fieldMatches;
    }

    /**
     * Returns the match parameters map of this selector.
     *
     * @return a match parameter map
     */
    public Collection<PiFieldMatch> fieldMatches() {
        return fieldMatches;
    }

    @Override
    public Type type() {
        return Type.PROTOCOL_INDEPENDENT;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiCriterion that = (PiCriterion) o;
        return Objects.equal(fieldMatches, that.fieldMatches);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fieldMatches);
    }

    @Override
    public String toString() {
        StringJoiner stringParams = new StringJoiner(", ", "{", "}");
        fieldMatches.forEach(f -> stringParams.add(f.toString()));
        return stringParams.toString();
    }
}
