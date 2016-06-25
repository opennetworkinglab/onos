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
 * Representation of adjacency state TLV of P2P neighbor.
 */
public class AdjacencyStateTlv extends TlvHeader implements IsisTlv {

    private byte adjacencyType;
    private int localCircuitId;
    private String neighborSystemId;
    private int neighborLocalCircuitId;

    /**
     * Creates an instance of adjacency state TLV..
     *
     * @param tlvHeader TLV header
     */
    public AdjacencyStateTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns local circuit ID for adjacency state TLV.
     *
     * @return local circuit ID
     */
    public int localCircuitId() {
        return localCircuitId;
    }

    /**
     * Sets local circuit ID for adjacency state TLV.
     *
     * @param localCircuitId local circuit Id
     */
    public void setLocalCircuitId(int localCircuitId) {
        this.localCircuitId = localCircuitId;
    }

    /**
     * Returns neighbor system ID for adjacency state TLV.
     *
     * @return neighbor system ID
     */
    public String neighborSystemId() {
        return neighborSystemId;
    }

    /**
     * Sets neighbor system ID for adjacency state TLV.
     *
     * @param neighborSystemId neighbor system ID
     */
    public void setNeighborSystemId(String neighborSystemId) {
        this.neighborSystemId = neighborSystemId;
    }

    /**
     * Returns neighbor local circuit ID for adjacency state TLV.
     *
     * @return neighbor local circuit ID
     */
    public int neighborLocalCircuitId() {
        return neighborLocalCircuitId;
    }

    /**
     * Sets neighbor local circuit ID for adjacency state TLV.
     *
     * @param neighborLocalCircuitId neighbor local circuit ID
     */
    public void setNeighborLocalCircuitId(int neighborLocalCircuitId) {
        this.neighborLocalCircuitId = neighborLocalCircuitId;
    }

    /**
     * Returns adjacency type of adjacency state TLV.
     *
     * @return adjacency type
     */
    public byte adjacencyType() {
        return adjacencyType;
    }

    /**
     * Sets adjacency type for adjacency state TLV.
     *
     * @param adjacencyType adjacency type
     */
    public void setAdjacencyType(byte adjacencyType) {
        this.adjacencyType = adjacencyType;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        this.setAdjacencyType(channelBuffer.readByte());
        this.setLocalCircuitId(channelBuffer.readInt());
        if (channelBuffer.readableBytes() > 0) {
            byte[] tempByteArray = new byte[IsisUtil.ID_SIX_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, IsisUtil.ID_SIX_BYTES);
            this.setNeighborSystemId(IsisUtil.systemId(tempByteArray));
            this.setNeighborLocalCircuitId(channelBuffer.readInt());
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
     * Returns adjacency type TLV body as byte array.
     *
     * @return byteArray TLV body of area address TLV
     */
    private byte[] tlvBodyAsBytes() {
        List<Byte> bytes = new ArrayList<>();
        bytes.add(this.adjacencyType);
        bytes.addAll(Bytes.asList(IsisUtil.convertToFourBytes(this.localCircuitId)));
        if (this.neighborSystemId != null) {
        bytes.addAll(IsisUtil.sourceAndLanIdToBytes(this.neighborSystemId));
        bytes.addAll(Bytes.asList(IsisUtil.convertToFourBytes(this.neighborLocalCircuitId)));
        }
        return Bytes.toArray(bytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("adjacencyType", adjacencyType)
                .add("localCircuitId", localCircuitId)
                .add("neighborSystemId", neighborSystemId)
                .add("neighborLocalCircuitId", neighborLocalCircuitId)
                .toString();
    }
}
