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
package org.onosproject.pcepio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Local and remote Link Identifiers.
 */
public class LinkLocalRemoteIdentifiersSubTlv implements PcepValueType {

    /* Reference :RFC5307
     * 0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type=4      |             Length=8               |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |               Link Local Identifier                           |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |               Link Remote Identifier                          |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

     */
    protected static final Logger log = LoggerFactory.getLogger(LinkLocalRemoteIdentifiersSubTlv.class);

    public static final short TYPE = 6;
    public static final short LENGTH = 8;
    private final int iLinkLocalIdentifier;
    private final int iLinkRemoteIdentifier;

    /**
     * Constructor to initialize iLinkLocalIdentifier , iLinkRemoteIdentifier.
     *
     * @param iLinkLocalIdentifier Link Local identifier
     * @param iLinkRemoteIdentifier Link Remote identifier
     */
    public LinkLocalRemoteIdentifiersSubTlv(int iLinkLocalIdentifier, int iLinkRemoteIdentifier) {
        this.iLinkLocalIdentifier = iLinkLocalIdentifier;
        this.iLinkRemoteIdentifier = iLinkRemoteIdentifier;
    }

    /**
     * Retruns an object of Link Local Remote Identifiers Tlv.
     *
     * @param iLinkLocalIdentifier Link Local identifier
     * @param iLinkRemoteIdentifier Link Remote identifier
     * @return object of LinkLocalRemoteIdentifiersTlv
     */
    public static LinkLocalRemoteIdentifiersSubTlv of(int iLinkLocalIdentifier, int iLinkRemoteIdentifier) {
        return new LinkLocalRemoteIdentifiersSubTlv(iLinkLocalIdentifier, iLinkRemoteIdentifier);
    }

    /**
     * Returns Link-Local-Identifier.
     *
     * @return iLinkLocalIdentifier Link Local Identifier
     */
    public int getLinkLocalIdentifier() {
        return iLinkLocalIdentifier;
    }

    /**
     * Returns Link-Remote-Identifier.
     *
     * @return iLinkRemoteIdentifier Link Remote Identifier.
     */
    public int getLinkRemoteIdentifier() {
        return iLinkRemoteIdentifier;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getLength() {
        return LENGTH;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(iLinkLocalIdentifier, iLinkRemoteIdentifier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LinkLocalRemoteIdentifiersSubTlv) {
            LinkLocalRemoteIdentifiersSubTlv other = (LinkLocalRemoteIdentifiersSubTlv) obj;
            return Objects.equals(iLinkLocalIdentifier, other.iLinkLocalIdentifier)
                    && Objects.equals(iLinkRemoteIdentifier, other.iLinkRemoteIdentifier);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(iLinkLocalIdentifier);
        c.writeInt(iLinkRemoteIdentifier);
        return c.writerIndex() - iStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of LinkLocalRemoteIdentifiersTlv.
     *
     * @param c input channel buffer
     * @return object of LinkLocalRemoteIdentifiersTlv
     */
    public static PcepValueType read(ChannelBuffer c) {
        int iLinkLocalIdentifier = c.readInt();
        int iLinkRemoteIdentifier = c.readInt();
        return new LinkLocalRemoteIdentifiersSubTlv(iLinkLocalIdentifier, iLinkRemoteIdentifier);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("LinkLocalIdentifier", iLinkLocalIdentifier)
                .add("LinkRemoteIdentifier", iLinkRemoteIdentifier)
                .toString();
    }
}
