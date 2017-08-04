/*
 * Copyright 2015-present Open Networking Foundation
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
 * Enum to Provide the Different BGP-LS NLRI types.
 */
public enum NlriType {

    NODE(1), LINK(2), PREFIX_IPV4(3), PREFIX_IPV6(4);

    int value;

    /**
     * Assign value with the value as the LINK-STATE NLRI type.
     *
     * @param value LINK-STATE NLRI type
     */
    NlriType(int value) {
        this.value = value;
    }

    /**
     * Returns value as LINK-STATE NLRI type.
     *
     * @return value LINK-STATE NLRI type
     */
    public byte getType() {
        return (byte) value;
    }
}