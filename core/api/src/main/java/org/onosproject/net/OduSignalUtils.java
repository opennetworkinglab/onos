/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of helper methods to convert various SignalTypes to OduSignalType, and to build OduSignalId.
 */
public final class OduSignalUtils {

    private static final Logger log = LoggerFactory.getLogger(OduSignalUtils.class);

    // prohibit instantiation
    private OduSignalUtils() {}

    /**
     * Maps from OduClt SignalType to OduSignalType.
     *
     * @param cltSignalType OduClt port signal type
     * @return OduSignalType the result of mapping CltSignalType to OduSignalType
     */
    public static OduSignalType mappingCltSignalTypeToOduSignalType(CltSignalType cltSignalType) {
        switch (cltSignalType) {
            case CLT_1GBE:
                return OduSignalType.ODU0;
            case CLT_10GBE:
                return OduSignalType.ODU2;
            case CLT_40GBE:
                return OduSignalType.ODU3;
            case CLT_100GBE:
                return OduSignalType.ODU4;
            default:
                log.error("Unsupported CltSignalType {}", cltSignalType);
                return OduSignalType.ODU0;
        }
    }

    /**
     * Maps from OtuPort SignalType to OduSignalType.
     *
     * @param otuSignalType Otu port signal type
     * @return OduSignalType the result of mapping OtuSignalType to OduSignalType
     */
    public static OduSignalType mappingOtuSignalTypeToOduSignalType(OtuSignalType otuSignalType) {
        switch (otuSignalType) {
        case OTU2:
            return OduSignalType.ODU2;
        case OTU4:
            return OduSignalType.ODU4;
        default:
            log.error("Unsupported OtuSignalType {}", otuSignalType);
            return OduSignalType.ODU0;
        }
    }

    /**
     * Creates OduSignalId from OduSignalType and TributarySlots.
     * @param oduSignalType - OduSignalType
     * @param slots - a set of TributarySlots
     * @return OduSignalId
     */
    public static OduSignalId buildOduSignalId(OduSignalType oduSignalType, Set<TributarySlot> slots) {
        int tributaryPortNumber = (int) slots.stream().findFirst().get().index();
        int tributarySlotLen = oduSignalType.tributarySlots();
        byte[] tributarySlotBitmap = new byte[OduSignalId.TRIBUTARY_SLOT_BITMAP_SIZE];

        slots.forEach(ts -> tributarySlotBitmap[(byte) (ts.index() - 1) / 8] |= 0x1 << ((ts.index() - 1) % 8));
        return OduSignalId.oduSignalId(tributaryPortNumber, tributarySlotLen, tributarySlotBitmap);
    }

}