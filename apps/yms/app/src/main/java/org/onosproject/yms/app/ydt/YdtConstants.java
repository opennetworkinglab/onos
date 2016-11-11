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

package org.onosproject.yms.app.ydt;

/**
 * Represents common constant utility for YANG data tree.
 */
final class YdtConstants {

    //No instantiation.
    private YdtConstants() {
    }

    /**
     * Error formatting string for duplicate entries found in ydt.
     */
    public static final String FMT_DUP_ENTRY = "Duplicate entry with name %s.";

    /**
     * Returns the error string by filling the parameters in the given
     * formatted error string.
     *
     * @param fmt    error format string
     * @param params parameters to be filled in formatted string
     * @return error string
     */
    public static String errorMsg(String fmt, Object... params) {
        return String.format(fmt, params);
    }
}