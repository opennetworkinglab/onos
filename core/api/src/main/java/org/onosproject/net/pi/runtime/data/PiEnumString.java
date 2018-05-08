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
 * EnumString entity in a protocol-independent pipeline.
 */
@Beta
public final class PiEnumString implements PiData {
    private final String enumString;

    /**
     * Creates a new protocol-independent enum string instance.
     *
     * @param enumString enum string
     */
    private PiEnumString(String enumString) {
        this.enumString = enumString;
    }

    /**
     * Returns a new protocol-independent enum string.
     * @param enumString enum string
     * @return enum string
     */
    public static PiEnumString of(String enumString) {
        return new PiEnumString(enumString);
    }

    /**
     * Return protocol-independent enum string instance.
     *
     * @return enum string
     */
    public String enumString() {
        return this.enumString;
    }

    @Override
    public Type type() {
        return Type.ENUMSTRING;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiEnumString enumStr = (PiEnumString) o;
        return Objects.equal(enumString, enumStr.enumString);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(enumString);
    }

    @Override
    public String toString() {
        return enumString;
    }
}