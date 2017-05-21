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
package org.onosproject.bgp.controller.impl;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HexDump class an utility to dump buffer in hex format.
 */
public final class HexDump {
    protected static final Logger log = LoggerFactory.getLogger(HexDump.class);

    private HexDump() {
    }

    /**
     * Dump the buffer content in hex format.
     *
     * @param buff buffer content to dump in hex format
     */
    public static void dump(ChannelBuffer buff) {
        buff.markReaderIndex();
        try {
            do {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; (k < 16) && (buff.readableBytes() != 0); ++k) {
                    if (0 == k % 4) {
                        sb.append(String.format(" ")); // blank after 4 bytes
                    }
                    sb.append(String.format("%02X ", buff.readByte()));
                }
                log.debug(sb.toString());
            } while (buff.readableBytes() != 0);
        } catch (Exception e) {
            log.error("[HexDump] Invalid buffer: " + e.toString());
        }
        buff.resetReaderIndex();
    }
}
