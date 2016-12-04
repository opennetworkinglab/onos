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

package org.onosproject.tetopology.management.api;

/**
 * Represents ENUM data of teStatus.
 */
public enum TeStatus {

    /**
     * Represents up.
     */
    UP(0),

    /**
     * Represents down.
     */
    DOWN(1),

    /**
     * Represents testing.
     */
    TESTING(2),

    /**
     * Represents preparingMaintenance.
     */
    PREPARING_MAINTENANCE(3),

    /**
     * Represents maintenance.
     */
    MAINTENANCE(4),

    /**
     * Status cannot be determined for some reason.
     */
    UNKNOWN(5);

    private int teStatus;

    /**
     * Creates an instance of teStatus.
     *
     * @param value value of teStatus
     */
    TeStatus(int value) {
        teStatus = value;
    }

    /**
     * Returns the attribute teStatus.
     *
     * @return value of teStatus
     */
    public int teStatus() {
        return teStatus;
    }

    /**
     * Returns the object of teAdminStatusEnumForTypeInt.Returns null
     * when string conversion fails or converted integer value is not
     * recognized.
     *
     * @param value value of teAdminStatusEnumForTypeInt
     * @return Object of teAdminStatusEnumForTypeInt
     */
    public static TeStatus of(int value) {
        switch (value) {
            case 0:
                return TeStatus.UP;
            case 1:
                return TeStatus.DOWN;
            case 2:
                return TeStatus.TESTING;
            case 3:
                return TeStatus.PREPARING_MAINTENANCE;
            case 4:
                return TeStatus.MAINTENANCE;
            case 5:
                return TeStatus.UNKNOWN;
            default :
                return null;
        }
    }
}
