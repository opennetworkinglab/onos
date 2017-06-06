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
import com.google.common.base.Objects;
import org.onosproject.net.pi.model.PiHeaderFieldModel;
import org.onosproject.net.pi.model.PiHeaderFieldTypeModel;
import org.onosproject.net.pi.model.PiHeaderModel;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 header field model.
 */
@Beta
public final class Bmv2HeaderFieldModel implements PiHeaderFieldModel {
    private final Bmv2HeaderModel header;
    private final Bmv2HeaderFieldTypeModel type;

    /**
     * Builds a BMv2 header field model with given BMv2 header model and header field type model.
     *
     * @param header the header model
     * @param type the header field type model
     */
    public Bmv2HeaderFieldModel(Bmv2HeaderModel header, Bmv2HeaderFieldTypeModel type) {
        checkNotNull(header, "Header can't be null");
        checkNotNull(type, "Type can't be null");
        this.header = header;
        this.type = type;
    }

    @Override
    public PiHeaderModel header() {
        return header;
    }

    @Override
    public PiHeaderFieldTypeModel type() {
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
        if (!(obj instanceof Bmv2HeaderFieldModel)) {
            return false;
        }
        Bmv2HeaderFieldModel that = (Bmv2HeaderFieldModel) obj;
        return Objects.equal(this.header, that.header) &&
                Objects.equal(this.type, that.type);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("header", header)
                .add("type", type)
                .toString();
    }
}
