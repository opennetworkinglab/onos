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
 * Representation of ISIS router types.
 */
public enum IsisRouterType {
    /**
     * Represents ISIS L1 router.
     */
    L1(1),
    /**
     * Represents ISIS L2 router.
     */
    L2(2),
    /**
     * Represents ISIS L1/L2 router.
     */
    L1L2(3);
    // Reverse lookup table
    private static final Map<Integer, IsisRouterType> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (IsisRouterType isisRouterType : EnumSet.allOf(IsisRouterType.class)) {
            LOOKUP.put(isisRouterType.value(), isisRouterType);
        }
    }

    private int value;

    /**
     * Creates an instance of ISIS router type.
     *
     * @param value represents ISIS router type
     */
    private IsisRouterType(int value) {
        this.value = value;
    }

    /**
     * Gets the enum instance from type value - reverse lookup purpose.
     *
     * @param routerTypeValue router type value
     * @return ISIS router type instance
     */
    public static IsisRouterType get(int routerTypeValue) {
        return LOOKUP.get(routerTypeValue);
    }

    /**
     * Gets the value representing router type.
     *
     * @return value represents router type
     */
    public int value() {
        return value;
    }
}