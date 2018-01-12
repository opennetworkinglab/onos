/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.bgpio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.protocol.ver4.BgpFactoryVer4;
import org.onosproject.bgpio.types.BgpHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction to provide the version for BGP.
 */
public final class BgpFactories {

    private static final Logger log = LoggerFactory.getLogger(BgpFactories.class);

    private static final GenericReader GENERIC_READER = new GenericReader();

    private BgpFactories() {
    }

    /**
     * Returns the instance of BGP Version.
     *
     * @param version BGP version
     * @return BGP version
     */
    public static BgpFactory getFactory(BgpVersion version) {
        switch (version) {
        case BGP_4:
            return BgpFactoryVer4.INSTANCE;
        default:
            throw new IllegalArgumentException("[BgpFactory:]Unknown version: " + version);
        }
    }

    /**
     * Reader class for reading BGP messages from channel buffer.
     *
     */
    private static class GenericReader implements BgpMessageReader<BgpMessage> {

        @Override
        public BgpMessage readFrom(ChannelBuffer bb, BgpHeader bgpHeader)
                throws BgpParseException {
            BgpFactory factory;

            if (!bb.readable()) {
                log.error("Empty message received");
                throw new BgpParseException("Empty message received");
            }
            // TODO: Currently only BGP version 4 is supported
            factory = org.onosproject.bgpio.protocol.ver4.BgpFactoryVer4.INSTANCE;
            return factory.getReader().readFrom(bb, bgpHeader);
        }
    }

    /**
     * Returns BGP messsage generic reader.
     *
     * @return bgp message generic reader
     */
    public static BgpMessageReader<BgpMessage> getGenericReader() {
        return GENERIC_READER;
    }
}
