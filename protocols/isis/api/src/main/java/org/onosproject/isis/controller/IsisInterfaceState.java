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
 * Enum represents ISIS Interface state.
 */
public enum IsisInterfaceState {
    /**
     * Represents interface is in "up" state.
     */
    UP(0),
    /**
     * Represents interface is in "initial" state.
     */
    INITIAL(1),
    /**
     * Represents interface is in "down" state.
     */
    DOWN(2);

    // Reverse lookup table
    private static final Map<Integer, IsisInterfaceState> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (IsisInterfaceState isisInterfaceState : EnumSet.allOf(IsisInterfaceState.class)) {
            LOOKUP.put(isisInterfaceState.value(), isisInterfaceState);
        }
    }

    private int value;

    /**
     * Creates an instance of ISIS interface type.
     *
     * @param value represents ISIS interface type
     */
    private IsisInterfaceState(int value) {
        this.value = value;
    }

    /**
     * Gets the enum instance from type value - reverse lookup purpose.
     *
     * @param interfaceStateTypeValue interface state type value
     * @return ISIS interface state type instance
     */
    public static IsisInterfaceState get(int interfaceStateTypeValue) {
        return LOOKUP.get(interfaceStateTypeValue);
    }

    /**
     * Gets the value representing interface state type.
     *
     * @return value represents interface state type
     */
    public int value() {
        return value;
    }
}