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

/**
 * Representation of a BMv2 match parameter.
 */
@Beta
public interface Bmv2MatchParam {

    /**
     * Returns the match type of this parameter.
     *
     * @return a match type value
     */
    Type type();

    /**
     * BMv2 match types.
     */
    enum Type {
        /**
         * Exact match type.
         */
        EXACT,
        /**
         * Ternary match type.
         */
        TERNARY,
        /**
         * Longest-prefix match type.
         */
        LPM,
        /**
         * Valid match type.
         */
        VALID;
    }
}
