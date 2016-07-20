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
import org.onosproject.bmv2.api.runtime.Bmv2MatchParam;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * A BMv2 table key model.
 */
@Beta
public final class Bmv2TableKeyModel {

    private final Bmv2MatchParam.Type matchType;
    private final Bmv2FieldModel field;

    /**
     * Creates a new table key model.
     *
     * @param matchType match type
     * @param field     field instance
     */
    protected Bmv2TableKeyModel(Bmv2MatchParam.Type matchType, Bmv2FieldModel field) {
        this.matchType = matchType;
        this.field = field;
    }

    /**
     * Returns the match type of this key.
     *
     * @return a string value
     * TODO returns enum of match type
     */
    public Bmv2MatchParam.Type matchType() {
        return matchType;
    }

    /**
     * Returns the header field instance matched by this key.
     *
     * @return a header field value
     */
    public Bmv2FieldModel field() {
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
        final Bmv2TableKeyModel other = (Bmv2TableKeyModel) obj;
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
