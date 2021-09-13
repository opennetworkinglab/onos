/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.bgpmonitoring;

/**
 * Enum to Provide the Different types of BMP termination message.
 */
public enum TerminationType {

    UTF8_STRING(0),

    REASON(1);

    private final int value;

    /**
     * Assign value with the value val as the types of BMP termination message.
     *
     * @param val type of BMP termination message
     */
    TerminationType(int val) {
        value = val;
    }

    /**
     * Returns value as type of BMP termination message.
     *
     * @return value type of BMP termination message
     */
    public int getType() {
        return value;
    }
}
