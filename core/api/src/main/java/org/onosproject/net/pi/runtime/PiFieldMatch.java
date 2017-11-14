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

import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchType;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Instance of a field match in a protocol-independent pipeline.
 */
public abstract class PiFieldMatch {

    private final PiMatchFieldId fieldId;

    /**
     * Creates a new field match for the given header field identifier.
     *
     * @param fieldId field identifier.
     */
    PiFieldMatch(PiMatchFieldId fieldId) {
        this.fieldId = checkNotNull(fieldId);
    }


    /**
     * Returns the identifier of the field to be matched.
     *
     * @return a header field ID value
     */
    public final PiMatchFieldId fieldId() {
        return fieldId;
    }

    /**
     * Returns the type of match to be performed.
     *
     * @return a match type value
     */
    public abstract PiMatchType type();

}
