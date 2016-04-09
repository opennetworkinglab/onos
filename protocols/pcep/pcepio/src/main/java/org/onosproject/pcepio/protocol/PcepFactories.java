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

package org.onosproject.pcepio.protocol;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepOutOfBoundMessageException;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.ver1.PcepFactoryVer1;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstraction to provide the version for PCEP.
 */
public final class PcepFactories {

    protected static final Logger log = LoggerFactory.getLogger(PcepFactories.class);

    private static final GenericReader GENERIC_READER = new GenericReader();

    public static final byte SHIFT_FLAG = 5;

    private PcepFactories() {
    }

    /**
     * Returns the instance of PCEP Version.
     *
     * @param version PCEP version
     * @return PCEP version
     */
    public static PcepFactory getFactory(PcepVersion version) {
        switch (version) {
        case PCEP_1:
            return PcepFactoryVer1.INSTANCE;
        default:
            throw new IllegalArgumentException("Unknown version: " + version);
        }
    }

    private static class GenericReader implements PcepMessageReader<PcepMessage> {

        @Override
        public PcepMessage readFrom(ChannelBuffer bb) throws PcepParseException, PcepOutOfBoundMessageException {

            if (!bb.readable()) {
                throw new PcepParseException("Empty message received");
            }

            /*
             * 0                   1                   2                   3
             * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
             * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             * | Ver |  Flags  |                                               |
             * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
             *
             * Currently Version 1 is supported
             * Currently no flags are used, it is all ignored
             */

            byte packetVersion = bb.getByte(bb.readerIndex());
            packetVersion = (byte) (packetVersion >> SHIFT_FLAG);
            PcepFactory factory;

            switch (packetVersion) {

            case 1:
                factory = org.onosproject.pcepio.protocol.ver1.PcepFactoryVer1.INSTANCE;
                break;
            default:
                throw new PcepParseException("Unknown Packet version: " + packetVersion);
            }
            return factory.getReader().readFrom(bb);
        }
    }

    /**
     * Returns GENERIC_READER.
     *
     * @return GENERIC_READER
     */
    public static PcepMessageReader<PcepMessage> getGenericReader() {
        return GENERIC_READER;
    }
}
