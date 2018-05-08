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
 * Boolean entity in a protocol-independent pipeline.
 */
@Beta
public final class PiBool implements PiData {
    private final Boolean bool;

    /**
     * Creates a new protocol-independent Bool instance.
     *
     * @param bool boolean
     */
    private PiBool(Boolean bool) {
        this.bool = bool;
    }

    /**
     * Returns a new protocol-independent Bool.
     * @param bool boolean
     * @return protocol-independent Bool
     */
    public static PiBool of(Boolean bool) {
        return new PiBool(bool);
    }

    /**
     * Return protocol-independent Boolean instance.
     *
     * @return bool
     */
    public Boolean bool() {
        return this.bool;
    }

    @Override
    public Type type() {
        return Type.BOOL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PiBool boolValue = (PiBool) o;
        return Objects.equal(bool, boolValue.bool);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bool);
    }

    @Override
    public String toString() {
        return bool.toString();
    }
}
