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
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiTableMatchFieldModel;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * BMv2 table match field model.
 */
@Beta
public final class Bmv2TableMatchFieldModel implements PiTableMatchFieldModel {
    private final PiMatchType matchType;
    private final Bmv2HeaderFieldModel field;

    /**
     * Creates new BMv2 table match field model using the given type and header field.
     *
     * @param matchType the match type
     * @param field the header field
     */
    public Bmv2TableMatchFieldModel(PiMatchType matchType, Bmv2HeaderFieldModel field) {
        checkNotNull(matchType, "Match type can't be null");
        checkNotNull(field, "Header field can't be null");

        this.matchType = matchType;
        this.field = field;
    }

    @Override
    public PiMatchType matchType() {
        return matchType;
    }

    @Override
    public Bmv2HeaderFieldModel field() {
        return field;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(matchType, field);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2TableMatchFieldModel other = (Bmv2TableMatchFieldModel) obj;
        return Objects.equal(this.matchType, other.matchType)
                && Objects.equal(this.field, other.field);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("matchType", matchType)
                .add("field", field)
                .toString();
    }


}
