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
import com.google.common.base.MoreObjects;
import org.onosproject.net.pi.model.PiActionParamModel;

import java.util.Objects;

import static com.google.common.base.Preconditions.*;

/**
 * BMv2 action parameter model.
 */
@Beta
public final class Bmv2ActionParamModel implements PiActionParamModel {
    private final String name;
    private final int bitWidth;

    /**
     * Builds a BMv2 action parameter model with given name and bit width.
     *
     * @param name the name
     * @param bitWidth the bit width
     */
    public Bmv2ActionParamModel(String name, int bitWidth) {
        checkNotNull(name, "Parameter name can't be null");
        checkArgument(bitWidth > 0, "Bit width should be a non-zero positive integer");
        this.name = name;
        this.bitWidth = bitWidth;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int bitWidth() {
        return bitWidth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bitWidth);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Bmv2ActionParamModel)) {
            return false;
        }
        Bmv2ActionParamModel that = (Bmv2ActionParamModel) obj;
        return Objects.equals(this.name, that.name) &&
                this.bitWidth == that.bitWidth;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("bitWidth", bitWidth)
                .toString();
    }
}
