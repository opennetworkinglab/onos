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
package org.onosproject.ospf.controller;

/**
 * Represents OSPF LSA types.
 */
public enum OspfLsaType {

    ROUTER(1),
    NETWORK(2),
    SUMMARY(3),
    ASBR_SUMMARY(4),
    EXTERNAL_LSA(5),
    LINK_LOCAL_OPAQUE_LSA(9),
    AREA_LOCAL_OPAQUE_LSA(10),
    AS_OPAQUE_LSA(11),
    UNDEFINED(20);

    private int value;

    /**
     * Creates an instance of OSPF LSA type.
     *
     * @param value represents LSA type
     */
    OspfLsaType(int value) {
        this.value = value;
    }

    /**
     * Gets the value representing LSA type.
     *
     * @return value represents LSA type
     */
    public int value() {
        return value;
    }
}