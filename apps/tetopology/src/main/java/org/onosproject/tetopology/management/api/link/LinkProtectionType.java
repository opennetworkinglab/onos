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
 * Represents ENUM data of linkProtectionType.
 */
public enum LinkProtectionType {

    /**
     * Represents unprotected.
     */
    UNPROTECTED(0),

    /**
     * Represents extraTraffic.
     */
    EXTRA_TRAFFIC(1),

    /**
     * Represents shared.
     */
    SHARED(2),

    /**
     * Represents yangAutoPrefix1For1.
     */
    YANGAUTOPREFIX1_FOR_1(3),

    /**
     * Represents yangAutoPrefix1Plus1.
     */
    YANGAUTOPREFIX1_PLUS_1(4),

    /**
     * Represents enhanced.
     */
    ENHANCED(5);

    private int linkProtectionType;

    LinkProtectionType(int value) {
        linkProtectionType = value;
    }

    /**
     * Returns the attribute linkProtectionType.
     *
     * @return value of linkProtectionType
     */
    public int linkProtectionType() {
        return linkProtectionType;
    }

    /**
     * Returns the object of linkProtectionType from input String. Returns null
     * when string conversion fails or converted integer value is not recognized.
     *
     * @param valInString input String
     * @return Object of linkProtectionType
     */
    public static LinkProtectionType of(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (NumberFormatException e) {
        }
        return null;
    }

    /**
     * Returns the object of linkProtectionType from input integer. Returns null
     * when the integer value is not recognized.
     *
     * @param value value of linkProtectionTypeForTypeInt
     * @return Object of linkProtectionTypeForTypeInt
     */
    public static LinkProtectionType of(int value) {
        switch (value) {
            case 0:
                return LinkProtectionType.UNPROTECTED;
            case 1:
                return LinkProtectionType.EXTRA_TRAFFIC;
            case 2:
                return LinkProtectionType.SHARED;
            case 3:
                return LinkProtectionType.YANGAUTOPREFIX1_FOR_1;
            case 4:
                return LinkProtectionType.YANGAUTOPREFIX1_PLUS_1;
            case 5:
                return LinkProtectionType.ENHANCED;
            default :
                return null;
        }
    }


}
