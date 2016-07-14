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

package org.onosproject.bmv2.api.runtime;


import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Representation of a BMv2 valid match parameter.
 */
@Beta
public final class Bmv2ValidMatchParam implements Bmv2MatchParam {

    private final boolean flag;

    /**
     * Creates a new valid match parameter using the given boolean flag.
     *
     * @param flag a boolean value
     */
    public Bmv2ValidMatchParam(boolean flag) {
        this.flag = flag;
    }

    @Override
    public Type type() {
        return Type.VALID;
    }

    /**
     * Returns the boolean flag of this parameter.
     *
     * @return a boolean value
     */
    public boolean flag() {
        return flag;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(flag);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2ValidMatchParam other = (Bmv2ValidMatchParam) obj;
        return this.flag == other.flag;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("flag", flag)
                .toString();
    }
}
