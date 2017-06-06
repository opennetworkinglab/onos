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

package org.onosproject.bmv2.model;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiHeaderFieldTypeModel;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 header field type model.
 */
@Beta
public final class Bmv2HeaderFieldTypeModel implements PiHeaderFieldTypeModel {
    private final String name;
    private final int bitWidth;
    private final boolean signed;

    /**
     * Builds a BMv2 header field type model with given information.
     *
     * @param name the name of field type
     * @param bitWidth the bit width of field type
     * @param signed header type field is signed or not
     */
    public Bmv2HeaderFieldTypeModel(String name, int bitWidth, boolean signed) {
        checkNotNull(name, "Header field type name can't be null");
        checkArgument(bitWidth > 0, "Bit width should be non-zero positive integer");
        this.name = name;
        this.bitWidth = bitWidth;
        this.signed = signed;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int bitWidth() {
        return bitWidth;
    }

    /**
     * Determine whether the header type field is signed or not.
     *
     * @return true if it is signed; otherwise false
     */
    public boolean signed() {
        return signed;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, bitWidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2HeaderFieldTypeModel other = (Bmv2HeaderFieldTypeModel) obj;
        return Objects.equal(this.name, other.name) &&
                Objects.equal(this.bitWidth, other.bitWidth) &&
                Objects.equal(this.signed, other.signed);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("bitWidth", bitWidth)
                .toString();
    }


}
