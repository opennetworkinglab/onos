/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.context;

import com.google.common.annotations.Beta;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A BMv2 action runtime data model.
 */
@Beta
public final class Bmv2RuntimeDataModel {

    private final String name;
    private final int bitWidth;

    /**
     * Creates a new runtime data model.
     *
     * @param name     name
     * @param bitWidth bitwidth
     */
    protected Bmv2RuntimeDataModel(String name, int bitWidth) {
        this.name = name;
        this.bitWidth = bitWidth;
    }

    /**
     * Return the name of this runtime data.
     *
     * @return a string value
     */
    public String name() {
        return name;
    }

    /**
     * Return the bit width of this runtime data.
     *
     * @return an integer value
     */
    public int bitWidth() {
        return bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bitWidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2RuntimeDataModel other = (Bmv2RuntimeDataModel) obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.bitWidth, other.bitWidth);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("bitWidth", bitWidth)
                .toString();
    }
}
