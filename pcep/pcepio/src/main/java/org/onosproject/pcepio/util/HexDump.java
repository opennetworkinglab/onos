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
