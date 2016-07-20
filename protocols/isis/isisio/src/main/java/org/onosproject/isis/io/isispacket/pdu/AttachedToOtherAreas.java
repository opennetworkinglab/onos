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
package org.onosproject.isis.io.isispacket.pdu;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Representation of attached to other areas.
 */
public enum AttachedToOtherAreas {
    DEFAULTMETRIC(1),
    DELAYMETRIC(2),
    EXPENSEMETRIC(4),
    ERRORMETRIC(8),
    NONE(0);
    // Reverse lookup table
    private static final Map<Integer, AttachedToOtherAreas> LOOKUP = new HashMap<>();

    // Populate the lookup table on loading time
    static {
        for (AttachedToOtherAreas attachedToOtherAreas :
                EnumSet.allOf(AttachedToOtherAreas.class)) {
            LOOKUP.put(attachedToOtherAreas.value(), attachedToOtherAreas);
        }
    }

    private int value;

    /**
     * Returns the attached to other areas value.
     *
     * @param value attached to other areas value
     */
    AttachedToOtherAreas(int value) {
        this.value = value;
    }

    /**
     * Returns the value for attached to other areas from pdu type value.
     *
     * @param pduTypeValue to get attached areas value
     * @return attachedToOtherAreas value of the enum
     */
    public static AttachedToOtherAreas get(int pduTypeValue) {
        return LOOKUP.get(pduTypeValue);
    }

    /**
     * Returns the value representing PDU type.
     *
     * @return value represents PDU type
     */
    public int value() {
        return value;
    }
}