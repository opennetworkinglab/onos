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
package org.onlab.packet;

import java.nio.ByteBuffer;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Representation of an MPLS Packet.
 */
public class MPLS extends BasePacket {
    public static final int HEADER_LENGTH = 4;

    public static final byte PROTOCOL_IPV4 = 0x1;
    public static final byte PROTOCOL_IPV6 = 0x2;
    public static final byte PROTOCOL_MPLS = 0x6;
    // mutable for Testing
    static Map<Byte, Deserializer<? extends IPacket>> protocolDeserializerMap =
            ImmutableMap.<Byte, Deserializer<? extends IPacket>>builder()
                    .put(PROTOCOL_IPV6, IPv6.deserializer())
                    .put(PROTOCOL_IPV4, IPv4.deserializer())
                    .put(PROTOCOL_MPLS, MPLS.deserializer())
                    .build();

    protected int label; //20bits
    protected byte bos; //1bit
    protected byte ttl; //8bits
    protected byte protocol;

    /**
     * Default constructor that sets the version to 4.
     */
    public MPLS() {
        super();
        this.bos = 1;
        this.protocol = PROTOCOL_IPV4;
    }

    @Override
    public byte[] serialize() {
        byte[] payloadData = null;
        if (payload != null) {
            payload.setParent(this);
            payloadData = payload.serialize();
        }

        byte[] data = new byte[(4 + ((payloadData != null) ? payloadData.length : 0)) ];
        ByteBuffer bb = ByteBuffer.wrap(data);

        bb.putInt(((this.label & 0x000fffff) << 12) | ((this.bos & 0x1) << 8 | (this.ttl & 0xff)));
        if (payloadData != null) {
            bb.put(payloadData);
        }

        return data;
    }


    /**
     * Returns the MPLS label.
     *
     * @return MPLS label
     */
    public int getLabel() {
        return label;
    }

    /**
     * Sets the MPLS label.
     *
     * @param label MPLS label
     */
    public void setLabel(int label) {
        this.label = label;
    }

    /**
     * Returns the MPLS TTL of the packet.
     *
     * @return MPLS TTL of the packet
     */
    public byte getTtl() {
        return ttl;
    }

    /**
     * Sets the MPLS TTL of the packet.
     *
     * @param ttl MPLS TTL
     */
    public void setTtl(byte ttl) {
        this.ttl = ttl;
    }

    @Override
    public IPacket setPayload(final IPacket payload) {
        // We implicitly assume that traffic can be only of these three types
        if (payload instanceof MPLS) {
            this.bos = 0;
            this.protocol = PROTOCOL_MPLS;
        } else if (payload instanceof IPv6) {
            this.bos = 1;
            this.protocol = PROTOCOL_IPV6;
        } else {
            this.bos = 1;
            this.protocol = PROTOCOL_IPV4;
        }
        return super.setPayload(payload);
    }

    /**
     * Deserializer function for MPLS packets.
     *
     * @return deserializer function
     */
    public static Deserializer<MPLS> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            MPLS mpls = new MPLS();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            int mplsheader = bb.getInt();
            mpls.label = ((mplsheader & 0xfffff000) >>> 12);
            mpls.bos = (byte) ((mplsheader & 0x00000100) >> 8);
            mpls.ttl = (byte) (mplsheader & 0x000000ff);

            ByteBuffer duplicate = bb.duplicate();
            short protocol = (short) ((duplicate.get() & 0xf0) >> 4);
            mpls.protocol = (mpls.bos == 1) ? protocol == 4 ?
                    PROTOCOL_IPV4 : PROTOCOL_IPV6 : PROTOCOL_MPLS;

            Deserializer<? extends IPacket> deserializer;
            if (protocolDeserializerMap.containsKey(mpls.protocol)) {
                deserializer = protocolDeserializerMap.get(mpls.protocol);
            } else {
                deserializer = Data.deserializer();
            }
            mpls.payload = deserializer.deserialize(data, bb.position(), bb.limit() - bb.position());
            mpls.payload.setParent(mpls);

            return mpls;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("label", Integer.toString(label))
                .add("bos", Byte.toString(bos))
                .add("ttl", Byte.toString(ttl))
                .add("protocol", Byte.toString(protocol))
                .toString();
    }
}
