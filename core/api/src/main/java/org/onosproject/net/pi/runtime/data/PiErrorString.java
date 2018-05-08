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

package org.onosproject.net.pi.runtime.data;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiData;

/**
 * ErrorString entity in a protocol-independent pipeline.
 */
@Beta
public final class PiErrorString implements PiData {
    private final String errorString;

    /**
     * Creates a new protocol-independent error string instance.
     *
     * @param errorString error string
     */
    private PiErrorString(String errorString) {
        this.errorString = errorString;
    }

    /**
     * Returns a new protocol-independent error string.
     * @param errorString error string
     * @return error string
     */
    public static PiErrorString of(String errorString) {
        return new PiErrorString(errorString);
    }

    /**
     * Return protocol-independent error string instance.
     *
     * @return error string
     */
    public String errorString() {
        return this.errorString;
    }

    @Override
    public Type type() {
        return Type.ERRORSTRING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiErrorString errorStr = (PiErrorString) o;
        return Objects.equal(errorString, errorStr.errorString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(errorString);
    }

    @Override
    public String toString() {
        return errorString;
    }
}