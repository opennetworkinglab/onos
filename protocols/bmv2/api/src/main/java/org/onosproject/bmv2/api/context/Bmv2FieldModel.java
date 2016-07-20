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
import com.google.common.base.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A BMv2 header field model.
 */
@Beta
public final class Bmv2FieldModel {

    private final Bmv2HeaderModel header;
    private final Bmv2FieldTypeModel type;

    protected Bmv2FieldModel(Bmv2HeaderModel header, Bmv2FieldTypeModel type) {
        this.header = header;
        this.type = type;
    }

    /**
     * Returns the header instance of this field instance.
     *
     * @return a header instance
     */
    public Bmv2HeaderModel header() {
        return header;
    }

    /**
     * Returns the type of this field instance.
     *
     * @return a field type value
     */
    public Bmv2FieldTypeModel type() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(header, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2FieldModel other = (Bmv2FieldModel) obj;
        return Objects.equal(this.header, other.header)
                && Objects.equal(this.type, other.type);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("header", header)
                .add("type", type)
                .toString();
    }
}
