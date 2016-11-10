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

package org.onosproject.tetopology.management.api.link;

/**
 * Represents the tunnel protection type.
 */
public enum TunnelProtectionType {

    /**
     * Represents unprotected.
     */
    UNPROTECTED(0),

    /**
     * Represents extra traffic.
     */
    EXTRA_TRAFFIC(1),

    /**
     * Represents shared.
     */
    SHARED(2),

    /**
     * Represents one-for-one.
     */
    ONE_FOR_ONE(3),

    /**
     * Represents one-plus-one.
     */
    ONE_PLUS_ONE(4),

    /**
     * Represents enhanced.
     */
    ENHANCED(5);

    private int value;

    TunnelProtectionType(int value) {
        this.value = value;
    }

    /**
     * Returns the value of the tunnel protection type.
     *
     * @return value of tunnel protection type
     */
    public int value() {
        return value;
    }

    /**
     * Returns the tunnel protection type constant corresponding to the given
     * string. Returns null when string conversion fails or converted integer
     * value is not recognized.
     *
     * @param s input string
     * @return corresponding protection type constant
     */
    public static TunnelProtectionType of(String s) {
        try {
            int tmpVal = Integer.parseInt(s);
            return of(tmpVal);
        } catch (NumberFormatException ignored) {
        }
        return null;
    }

    /**
     * Returns the tunnel protection type constant corresponding to the
     * given integer. Returns null when the integer value is not recognized.
     *
     * @param value integer value
     * @return corresponding protection type constant
     */
    public static TunnelProtectionType of(int value) {
        switch (value) {
            case 0:
                return UNPROTECTED;
            case 1:
                return EXTRA_TRAFFIC;
            case 2:
                return SHARED;
            case 3:
                return ONE_FOR_ONE;
            case 4:
                return ONE_PLUS_ONE;
            case 5:
                return ENHANCED;
            default:
                return null;
        }
    }
}
