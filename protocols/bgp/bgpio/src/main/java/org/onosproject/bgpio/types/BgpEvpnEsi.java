/*
 * Copyright 2017-present Open Networking Foundation
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

import java.util.Arrays;
import org.jboss.netty.buffer.ChannelBuffer;
import com.google.common.base.MoreObjects;

/**
 * Implementation of EthernetSegmentidentifier.
 */
public class BgpEvpnEsi
        implements Comparable<BgpEvpnEsi> {

    public static final int ESI_LENGTH = 10;
    private byte[] ethernetSegmentidentifier;

    /**
     * Resets fields.
     */
    public BgpEvpnEsi() {
        this.ethernetSegmentidentifier = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param ethernetSegmentidentifier Ethernet Segment identifier
     */
    public BgpEvpnEsi(byte[] ethernetSegmentidentifier) {
        this.ethernetSegmentidentifier = ethernetSegmentidentifier;
    }

    /**
     * Reads Ethernet Segment identifier from channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of EthernetSegmentidentifier
     */
    public static BgpEvpnEsi read(ChannelBuffer cb) {
        return new BgpEvpnEsi(cb.readBytes(10).array());
    }

    /**
     * writes Ethernet Segment identifier into channelBuffer.
     *
     * @param cb channelBuffer
     * @return length length of written data
     */
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeBytes(ethernetSegmentidentifier);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Returns Ethernet Segment identifier.
     *
     * @return Ethernet Segment identifier.
     */
    public byte[] getEthernetSegmentidentifier() {
        return this.ethernetSegmentidentifier;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ethernetSegmentidentifier);
    };

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpEvpnEsi) {
            BgpEvpnEsi that = (BgpEvpnEsi) obj;
            return Arrays.equals(this.ethernetSegmentidentifier, that.ethernetSegmentidentifier);
        }

        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ethernetSegmentidentifier", ethernetSegmentidentifier)
                .toString();
    }

    @Override
    public int compareTo(BgpEvpnEsi rd) {
        return 0;
    }
}
