/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcepio.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Hex Dump for debugging.
 */
public final class HexDump {
    protected static final Logger log = LoggerFactory.getLogger(HexDump.class);

    private HexDump() {
    }

    public static void pcepHexDump(ChannelBuffer buff) {

        log.debug("==================== HEX DUMP ======================");
        try {
            byte[] yTemp;
            yTemp = buff.array();

            int iStartIndex = buff.readerIndex();
            int iEndIndex = buff.writerIndex();
            do {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; (k < 16) && (iStartIndex < iEndIndex); ++k) {
                    if (0 == k % 4) {
                        sb.append(String.format(" ")); //blank after 4 bytes
                    }
                    sb.append(String.format("%02X ", yTemp[iStartIndex++]));
                }
                log.debug(sb.toString());
            } while (iStartIndex < iEndIndex);
        } catch (Exception e) {
            log.error("[HexDump] Invalid buffer: " + e.toString());
        }

        log.debug("===================================================");
    }
}
