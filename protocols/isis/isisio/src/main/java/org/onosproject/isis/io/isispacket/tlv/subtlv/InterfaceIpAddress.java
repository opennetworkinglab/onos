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
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.util.IsisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of interface ip address TE value.
 */
public class InterfaceIpAddress extends TlvHeader implements TrafficEngineeringSubTlv {
    private static final Logger log =
            LoggerFactory.getLogger(NeighborIpAddress.class);
    private Ip4Address localInterfaceIPAddress;

    /**
     * Creates an instance of local interface ip address.
     *
     * @param header tlv header instance
     */
    public InterfaceIpAddress(TlvHeader header) {
        this.setTlvType(header.tlvType());
        this.setTlvLength(header.tlvLength());
    }

    /**
     * Adds local interface ip address.
     *
     * @param localAddress ip address
     */
    public void setIpAddress(Ip4Address localAddress) {
        this.localInterfaceIPAddress = localAddress;
    }

    /**
     * Gets local interface ip address.
     *
     * @return localAddress ip address
     */
    public Ip4Address localInterfaceIPAddress() {
        return localInterfaceIPAddress;
    }

    /**
     * Reads bytes from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     */
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() >= IsisUtil.FOUR_BYTES) {
            byte[] tempByteArray = new byte[IsisUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, IsisUtil.FOUR_BYTES);
            this.setIpAddress(Ip4Address.valueOf(tempByteArray));

        }
    }

    /**
     * Gets local interface ip address as byte array.
     *
     * @return local interface ip address as byte array
     */
    public byte[] asBytes() {
        byte[] linkSubType = null;

        byte[] linkSubTlvHeader = tlvHeaderAsByteArray();
        byte[] linkSubTlvBody = tlvBodyAsBytes();
        linkSubType = Bytes.concat(linkSubTlvHeader, linkSubTlvBody);

        return linkSubType;
    }

    /**
     * Gets byte array of local interface ip address.
     *
     * @return byte array of local interface ip address
     */
    public byte[] tlvBodyAsBytes() {

        List<Byte> linkSubTypeBody = new ArrayList<>();

        linkSubTypeBody.addAll(Bytes.asList(this.localInterfaceIPAddress.toOctets()));


        return Bytes.toArray(linkSubTypeBody);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("localInterfaceIPAddress", localInterfaceIPAddress)
                .toString();
    }
}