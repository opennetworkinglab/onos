/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.bgpio.protocol;

/**
 * Enum to Provide the Different types of BGP messages.
 */
public enum BgpType {

    NONE(0), OPEN(1), UPDATE(2), NOTIFICATION(3), KEEP_ALIVE(4);

    int value;

    /**
     * Assign value with the value val as the types of BGP message.
     *
     * @param val type of BGP message
     */
    BgpType(int val) {
        value = val;
    }

    /**
     * Returns value as type of BGP message.
     *
     * @return value type of BGP message
     */
    public byte getType() {
        return (byte) value;
    }
}