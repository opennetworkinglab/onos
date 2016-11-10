/*
 * Copyright 2016 Open Networking Laboratory
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
 * Represents ENUM data of teLinkAccessType.
 */
public enum TeLinkAccessType {
    /**
     * Represents pointToPoint.
     */
    POINT_TO_POINT(0),

    /**
     * Represents multiAccess.
     */
    MULTI_ACCESS(1);

    private int teLinkAccessType;

    TeLinkAccessType(int value) {
        teLinkAccessType = value;
    }

    /**
     * Returns the attribute teLinkAccessType.
     *
     * @return value of teLinkAccessType
     */
    public int teLinkAccessType() {
        return teLinkAccessType;
    }

    /**
     * Returns the object of teLinkAccessType from input String. Returns null
     * when string conversion fails or converted integer value is not recognized.
     *
     * @param valInString input String
     * @return Object of teLinkAccessType
     */
    public static TeLinkAccessType of(String valInString) {
        try {
            int tmpVal = Integer.parseInt(valInString);
            return of(tmpVal);
        } catch (NumberFormatException e) {
        }
        return null;
    }

    /**
     * Returns the object of teLinkAccessTypeForTypeInt. Returns null
     * when the integer value is not recognized.
     *
     * @param value value of teLinkAccessTypeForTypeInt
     * @return Object of teLinkAccessTypeForTypeInt
     */
    public static TeLinkAccessType of(int value) {
        switch (value) {
            case 0:
                return TeLinkAccessType.POINT_TO_POINT;
            case 1:
                return TeLinkAccessType.MULTI_ACCESS;
            default :
                return null;
        }
    }

}
