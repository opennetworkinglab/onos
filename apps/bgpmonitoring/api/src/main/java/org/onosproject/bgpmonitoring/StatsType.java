/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.bgpmonitoring;

/**
 * Enum to Provide the Different types of BMP stats.
 */
public enum StatsType {

    PREFIXES_REJECTED(0),

    DUPLICATE_PREFIX(1),

    DUPLICATE_WITHDRAW(2),

    CLUSTER_LIST(3),

    AS_PATH(4),

    ORIGINATOR_ID(5),

    AS_CONFED(6),

    ADJ_RIB_IN(7),

    LOC_RIB(8),

    ADJ_RIB_IN_AFI_SAFI(9),

    LOC_RIB_AFI_SAFI(10),

    UPDATES_SUBJECTED_WITHDRAW(11),

    PREFIXES_SUBJECTED_WITHDRAW(12),

    DUPLICATE_UPDATE_MESSAGES(13),

    JNX_ADJ_RIB_IN(17);


    private final int value;

    /**
     * Assign value with the value val as the types of BMP stats.
     *
     * @param val type of BMP stats
     */
    StatsType(int val) {
        value = val;
    }

    /**
     * Returns value as type of BMP stats.
     *
     * @return value type of BMP stats
     */
    public int getType() {
        return value;
    }
}
