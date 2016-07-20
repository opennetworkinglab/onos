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
package org.onosproject.isis.io.isispacket.tlv;

import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.isis.io.util.IsisUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of IS extended reachability TLV.
 */
public class IsExtendedReachability extends TlvHeader implements IsisTlv {

    private List<NeighborForExtendedIs> neighbors = new ArrayList<>();

    /**
     * Creates an instance of IP external reachability TLV.
     *
     * @param tlvHeader TLV header
     */
    public IsExtendedReachability(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns neighbor list.
     *
     * @return neighbor list
     */
    public List<NeighborForExtendedIs> neighbours() {
        return neighbors;
    }

    /**
     * Adds the neighbor for extended IS instance to IS extended reachability TLV.
     *
     * @param neighbor neighbor for extended IS instance
     */
    public void addNeighbor(NeighborForExtendedIs neighbor) {
        this.neighbors.add(neighbor);
    }


    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        while (channelBuffer.readableBytes() >= (IsisUtil.EIGHT_BYTES + IsisUtil.THREE_BYTES)) {
            NeighborForExtendedIs extendedIs = new NeighborForExtendedIs();
            extendedIs.readFrom(channelBuffer);
            this.addNeighbor(extendedIs);
        }
    }

    @Override
    public byte[] asBytes() {
        byte[] bytes = null;
        byte[] tlvHeader = tlvHeaderAsByteArray();
        byte[] tlvBody = tlvBodyAsBytes();
        tlvHeader[1] = (byte) tlvBody.length;
        bytes = Bytes.concat(tlvHeader, tlvBody);
        return bytes;
    }

    /**
     * Returns TLV body of IS extended reachability TLV.
     *
     * @return byteArray TLV body of IS extended reachability TLV.
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> byteList = new ArrayList<>();
        for (NeighborForExtendedIs neighbor : this.neighbors) {
            byteList.addAll(Bytes.asList(neighbor.neighborBodyAsbytes()));
        }
        return Bytes.toArray(byteList);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("neighbors", neighbors)
                .toString();
    }
}