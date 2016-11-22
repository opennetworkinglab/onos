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

package org.onosproject.yms.app.ych.defaultcodecs;

import org.onosproject.yms.app.ych.defaultcodecs.xml.DefaultXmlCodec;
import org.onosproject.yms.ych.YangDataTreeCodec;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.onosproject.yms.ych.YangProtocolEncodingFormat.XML;

/**
 * Default implementation of YANG codec registry.
 */
public final class YangCodecRegistry {

    // no instantiation
    private YangCodecRegistry() {
    }

    /**
     * Default codec map.
     */
    private static final Map<YangProtocolEncodingFormat, YangDataTreeCodec>
            DEFAULT_CODECS = new HashMap<>();

    /**
     * Initialise the default codec map.
     */
    public static void initializeDefaultCodec() {
        DEFAULT_CODECS.put(XML, new DefaultXmlCodec());
    }

    /**
     * Returns the default codec map.
     *
     * @return the default codec map
     */
    public static Map<YangProtocolEncodingFormat, YangDataTreeCodec> getDefaultCodecs() {
        return Collections.unmodifiableMap(DEFAULT_CODECS);
    }

    /**
     * Registers a default codec for the specified data format.
     *
     * @param defaultCodec registered data tree codec
     * @param dataFormat   protocol encoding data format
     */
    public static void registerDefaultCodec(
            YangDataTreeCodec defaultCodec,
            YangProtocolEncodingFormat dataFormat) {
        DEFAULT_CODECS.put(dataFormat, defaultCodec);
    }
}
