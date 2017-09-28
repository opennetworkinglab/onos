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

package org.onlab.packet;

import java.nio.ByteBuffer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Representation of a VXLAN(Virtual eXtensible Local Area Network) packet.
 */
public class VXLAN extends BasePacket {

    private static final short VXLAN_HEADER_LENGTH = 8;
    private static final int BYTE_SHIFT = 8;
    private static final int BYTE_MASK = 0xff;

    protected byte flags = 0;
    protected byte[] rsvd1 = new byte[] {0, 0, 0}; // reserved filed
    protected byte[] vni = new byte[] {0, 0, 0};
    protected byte rsvd2 = 0; // reserved field

    /**
     * Serializes the packet.
     */
    @Override
    public byte[] serialize() {

        byte[] payloadData = null;
        if (this.payload != null) {
            this.payload.setParent(this);
            payloadData = this.payload.serialize();
        }

        int length = VXLAN_HEADER_LENGTH + (payloadData == null ? 0 : payloadData.length);

        final byte[] data = new byte[length];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.flags);
        bb.put(this.rsvd1);
        bb.put(this.vni);
        bb.put(this.rsvd2);
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }

    /**
     * Returns VNI(VXLAN Network Identifier).
     *
     * @return the VNI
     */
    public int getVni() {
        return (vni[0] << (BYTE_SHIFT * 2)) + (vni[1] << BYTE_SHIFT) + vni[2];
    }

    /**
     * Set VNI.
     *
     * @param vni the VNI to set( 24 bits )
     * @return this
     */
    public VXLAN setVni(int vni) {
        this.vni[0] = (byte) ((vni >> (BYTE_SHIFT * 2)) & BYTE_MASK);
        this.vni[1] = (byte) ((vni >> BYTE_SHIFT) & BYTE_MASK);
        this.vni[2] = (byte) (vni & BYTE_MASK);
        return this;
    }

    /**
     * Return flags.
     *
     * @return the flags
     */
    public byte getFlag() {
        return this.flags;
    }

    /**
     * Set flags.
     *
     * @param flags the flags to set( 8 bits )
     * @return this
     */
    public VXLAN setFlag(byte flags) {
        this.flags = flags;
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 2521;
        int result = super.hashCode();
        result = prime * result + this.getVni();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof VXLAN)) {
            return false;
        }

        final VXLAN other = (VXLAN) obj;
        if (this.getVni() != other.getVni()) {
            return false;
        }
        return true;
    }


    /**
     * Returns the deserializer closure (used by upper layer deserializer).
     *
     * @return the deserializer closure
     */
    public static Deserializer<VXLAN> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, VXLAN_HEADER_LENGTH);

            VXLAN vxlan = new VXLAN();

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            vxlan.flags = bb.get();
            bb.get(vxlan.rsvd1);
            bb.get(vxlan.vni);
            vxlan.rsvd2 = bb.get();

            Deserializer<? extends IPacket> deserializer = Data.deserializer();

            vxlan.payload = deserializer.deserialize(data, bb.position(),
                    bb.limit() - bb.position());
            vxlan.payload.setParent(vxlan);

            return vxlan;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("vni", Integer.toString(getVni()))
                .add("flags", Byte.toString(getFlag()))
                .toString();
    }

}
