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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiMatchType;

/**
 * A valid field match in a protocol-independent pipeline.
 */
@Beta
public final class PiValidFieldMatch extends PiFieldMatch {

    private final boolean isValid;

    /**
     * Creates a new valid field match.
     *
     * @param fieldId field identifier
     * @param isValid validity flag
     */
    public PiValidFieldMatch(PiHeaderFieldId fieldId, boolean isValid) {
        super(fieldId);
        this.isValid = isValid;
    }

    @Override
    public final PiMatchType type() {
        return PiMatchType.VALID;
    }

    /**
     * Returns the boolean flag of this valid match parameter.
     *
     * @return valid match flag
     */
    public boolean isValid() {
        return isValid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiValidFieldMatch that = (PiValidFieldMatch) o;
        return Objects.equal(this.fieldId(), that.fieldId()) &&
                isValid == that.isValid;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.fieldId(), isValid);
    }

    @Override
    public String toString() {
        return this.fieldId().toString() + '=' + (isValid ? "VALID" : "NOT_VALID");
    }
}
