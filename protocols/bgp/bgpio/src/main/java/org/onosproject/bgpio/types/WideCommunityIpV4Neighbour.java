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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpAddress;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Validation;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Provides implementation of BGP wide community IPV4 neighbour subtlv.
 */
public class WideCommunityIpV4Neighbour implements BgpValueType {
    public static final byte TYPE = 8;
    private List<IpV4Neighbour> ipv4Neighbour;
    public static final byte IPV4_NEIGHBOUR_SIZE = 8;

    /**
     * Creates an instance of wide community ipv4 neighbour subtlv.
     *
     */
    public WideCommunityIpV4Neighbour() {
        this.ipv4Neighbour = new ArrayList<>();
    }

    /**
     * Adds local and remote speakers.
     *
     * @param localSpeaker local speaker
     * @param remoteSpeaker remote speaker
     */
    public void add(IpAddress localSpeaker, IpAddress remoteSpeaker) {
        ipv4Neighbour.add(new IpV4Neighbour(localSpeaker, remoteSpeaker));
    }

    /**
     * Deletes local and remote speakers.
     *
     * @param localSpeaker local speaker
     * @param remoteSpeaker remote speaker
     */
    public void remove(IpAddress localSpeaker, IpAddress remoteSpeaker) {
        ipv4Neighbour.remove(new IpV4Neighbour(localSpeaker, remoteSpeaker));
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipv4Neighbour);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityIpV4Neighbour) {
            WideCommunityIpV4Neighbour other = (WideCommunityIpV4Neighbour) obj;
            return Objects.equals(ipv4Neighbour, other.ipv4Neighbour);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();

        Iterator<IpV4Neighbour> listIterator = ipv4Neighbour.iterator();
        c.writeByte(TYPE);

        int iLengthIndex = c.writerIndex();
        c.writeShort(0);

        while (listIterator.hasNext()) {
            IpV4Neighbour speaker = listIterator.next();
            c.writeBytes(speaker.localSpeaker().toOctets());
            c.writeBytes(speaker.remoteSpeaker().toOctets());
        }

        int length = c.writerIndex() - iLengthIndex;
        c.setShort(iLengthIndex, (short) (length - 2));

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityIpV4Neighbour.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityIpV4Neighbour
     * @throws BgpParseException on read error
     */
    public static WideCommunityIpV4Neighbour read(ChannelBuffer c) throws BgpParseException {
        WideCommunityIpV4Neighbour wideCommNeighbour = new WideCommunityIpV4Neighbour();
        short length;

        if (c.readableBytes() == 0) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   c.readableBytes());
        }

        length = c.readShort();
        if (c.readableBytes() == 0) {
            return wideCommNeighbour;
        }

        if (c.readableBytes() < length) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   c.readableBytes());
        }

        while (c.readableBytes() > 0) {
            if (c.readableBytes() < IPV4_NEIGHBOUR_SIZE) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       c.readableBytes());
            }

            IpAddress localSpeaker = IpAddress.valueOf(c.readInt());
            IpAddress remoteSpeaker = IpAddress.valueOf(c.readInt());

            wideCommNeighbour.add(localSpeaker, remoteSpeaker);

        }

        return wideCommNeighbour;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ipv4Neighbour", ipv4Neighbour)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public short getType() {
        // TODO Auto-generated method stub
        return 0;
    }

    /*
     * IpV4Neighbour class contain remote and local speaker.
     */
    private class IpV4Neighbour {
        private IpAddress localSpeaker;
        private IpAddress remoteSpeaker;

        /**
         * Creates an instance of ipv4 neighbour.
         *
         * @param localSpeaker ip address of local speaker
         * @param remoteSpeaker  ip address of remote speaker
         */
        public IpV4Neighbour(IpAddress localSpeaker, IpAddress remoteSpeaker) {
            this.localSpeaker = localSpeaker;
            this.remoteSpeaker = remoteSpeaker;
        }

        /**
         * Returns IPV4 neighbour local speaker.
         *
         * @return IPV4 neighbour local speaker
         */
        public IpAddress localSpeaker() {
            return localSpeaker;
        }

        /**
         * Returns IPV4 neighbour remote speaker.
         *
         * @return IPV4 neighbour remote speaker
         */
        public IpAddress remoteSpeaker() {
            return remoteSpeaker;
        }
    }
}