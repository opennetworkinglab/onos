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
package org.onosproject.isis.io.isispacket.tlv.subtlv;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.util.IsisUtil;


/**
 * Representation of an administrative group.
 */
public class AdministrativeGroup extends TlvHeader implements TrafficEngineeringSubTlv {

    private int administrativeGroup;

    /**
     * Creates an administrative group instance.
     *
     * @param header Tlv Header instance
     */
    public AdministrativeGroup(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Returns administrative group value.
     *
     * @return administrative group value
     */
    public int administrativeGroup() {
        return administrativeGroup;
    }

    /**
     * Sets administrative group value.
     *
     * @param administrativeGroup value
     */
    public void setAdministrativeGroup(int administrativeGroup) {
        this.administrativeGroup = administrativeGroup;
    }

    /**
     * Returns administrative group value.
     *
     * @return administrativeGroup value
     */
    public int getAdministrativeGroupValue() {
        return this.administrativeGroup;
    }

    /**
     * Reads bytes from channel buffer.
     *
     * @param channelBuffer Channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        byte[] tempByteArray = new byte[tlvLength()];
        channelBuffer.readBytes(tempByteArray, 0, tlvLength());
        this.setAdministrativeGroup(IsisUtil.byteToInteger(tempByteArray));
    }

    /**
     * Returns administrative group as byte array.
     *
     * @return administrative group instance as byte array
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = tlvHeaderAsByteArray();
        byte[] linkSubTlvBody = tlvBodyAsBytes();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);

        return linkSubType;
    }

    /**
     * Returns administrative group body as byte array.
     *
     * @return byte array of sub tlv administrative group
     */
    public byte[] tlvBodyAsBytes() {

        byte[] linkSubTypeBody;
        linkSubTypeBody = IsisUtil.convertToFourBytes(this.administrativeGroup);

        return linkSubTypeBody;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("administrativeGroup", administrativeGroup)
                .toString();
    }
}