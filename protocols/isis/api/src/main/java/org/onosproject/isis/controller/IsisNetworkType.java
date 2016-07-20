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
package org.onosproject.isis.controller;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents ISIS network types.
 */
public enum IsisNetworkType {
    /**
     * Represents point-to-point network.
     */
    P2P(1),
    /**
     * Represents broadcast network.
     */
    BROADCAST(2);
    // Reverse lookup table
    private static final Map<Integer, IsisNetworkType> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (IsisNetworkType isisNetworkType : EnumSet.allOf(IsisNetworkType.class)) {
            LOOKUP.put(isisNetworkType.value(), isisNetworkType);
        }
    }

    private int value;

    /**
     * Creates an instance of ISIS network type.
     *
     * @param value represents ISIS network type
     */
    private IsisNetworkType(int value) {
        this.value = value;
    }

    /**
     * Gets the enum instance from type value - reverse lookup purpose.
     *
     * @param isisNetworkTypeValue interface network type value
     * @return ISIS interface network type instance
     */
    public static IsisNetworkType get(int isisNetworkTypeValue) {
        return LOOKUP.get(isisNetworkTypeValue);
    }

    /**
     * Gets the value representing network type.
     *
     * @return value represents network type
     */
    public int value() {
        return value;
    }
}