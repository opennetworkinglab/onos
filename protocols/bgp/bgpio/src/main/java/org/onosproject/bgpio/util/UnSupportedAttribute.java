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

package org.onosproject.bgpio.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides methods to handle UnSupportedAttribute.
 */
public final class UnSupportedAttribute {
    protected static final Logger log = LoggerFactory.getLogger(UnSupportedAttribute.class);

    private UnSupportedAttribute() {
    }

    /**
     * Reads channel buffer parses attribute header and skips specified length.
     *
     * @param cb channelBuffer
     */
    public static void read(ChannelBuffer cb) {
        Validation parseFlags = Validation.parseAttributeHeader(cb);
        cb.skipBytes(parseFlags.getLength());
    }

    /**
     * Skip specified bytes in channel buffer.
     *
     * @param cb channelBuffer
     * @param length to be skipped
     */
    public static void skipBytes(ChannelBuffer cb, short length) {
        cb.skipBytes(length);
    }
}