/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.packet.lacp;

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.BasePacket;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

public class Lacp extends BasePacket {
    public static final int HEADER_LENGTH = 1;
    public static final byte TYPE_ACTOR = 1;
    public static final byte TYPE_PARTNER = 2;
    public static final byte TYPE_COLLECTOR = 3;
    public static final byte TYPE_TERMINATOR = 0;

    private static final Map<Byte, Deserializer<? extends LacpTlv>> PROTOCOL_DESERIALIZER_MAP =
            ImmutableMap.<Byte, Deserializer<? extends LacpTlv>>builder()
                    .put(TYPE_ACTOR, LacpBaseTlv.deserializer())
                    .put(TYPE_PARTNER, LacpBaseTlv.deserializer())
                    .put(TYPE_COLLECTOR, LacpCollectorTlv.deserializer())
                    .put(TYPE_TERMINATOR, LacpTerminatorTlv.deserializer())
                    .build();

    private byte lacpVersion;
    private Map<Byte, LacpTlv> tlv = new ConcurrentHashMap<>();

    /**
     * Gets LACP version.
     *
     * @return LACP version
     */
    public byte getLacpVersion() {
        return this.lacpVersion;
    }

    /**
     * Sets LACP version.
     *
     * @param lacpVersion LACP version
     * @return this
     */
    public Lacp setLacpVersion(byte lacpVersion) {
        this.lacpVersion = lacpVersion;
        return this;
    }

    /**
     * Gets LACP TLV.
     *
     * @return LACP TLV
     */
    public Map<Byte, LacpTlv> getTlv() {
        return this.tlv;
    }

    /**
     * Sets LACP TLV.
     *
     * @param tlv LACP TLV
     * @return this
     */
    public Lacp setTlv(Map<Byte, LacpTlv> tlv) {
        this.tlv = tlv;
        return this;
    }

    /**
     * Deserializer function for LACP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<Lacp> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            Lacp lacp = new Lacp();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            lacp.setLacpVersion(bb.get());

            while (bb.limit() - bb.position() > 0) {
                byte nextType = bb.get();
                int nextLength = Byte.toUnsignedInt(bb.get());
                int parseLength = nextLength == 0 ?
                        LacpTerminatorTlv.PADDING_LENGTH : // Special case for LacpTerminatorTlv
                        nextLength - 2;

                Deserializer<? extends LacpTlv> deserializer;
                if (Lacp.PROTOCOL_DESERIALIZER_MAP.containsKey(nextType)) {
                    deserializer = Lacp.PROTOCOL_DESERIALIZER_MAP.get(nextType);
                } else {
                    throw new DeserializationException("Unsupported LACP subtype " + Byte.toString(nextType));
                }

                LacpTlv tlv = deserializer.deserialize(data, bb.position(), parseLength);
                LacpTlv previousTlv = lacp.tlv.put(nextType, tlv);
                if (previousTlv != null) {
                    throw new DeserializationException("Duplicated type " + Byte.toString(nextType)
                            + "in LACP TLV");
                }

                bb.position(bb.position() + parseLength);
            }

            return lacp;
        };
    }

    @Override
    public byte[] serialize() {
        byte[] actorInfo = Optional.ofNullable(tlv.get(TYPE_ACTOR)).map(LacpTlv::serialize)
                .orElse(new byte[0]);
        byte[] partnerInfo = Optional.ofNullable(tlv.get(TYPE_PARTNER)).map(LacpTlv::serialize)
                .orElse(new byte[0]);
        byte[] collectorInfo = Optional.ofNullable(tlv.get(TYPE_COLLECTOR)).map(LacpTlv::serialize)
                .orElse(new byte[0]);
        byte[] terminatorInfo = Optional.ofNullable(tlv.get(TYPE_TERMINATOR)).map(LacpTlv::serialize)
                .orElse(new byte[0]);

        final byte[] data = new byte[HEADER_LENGTH
                + LacpTlv.HEADER_LENGTH + actorInfo.length
                + LacpTlv.HEADER_LENGTH + partnerInfo.length
                + LacpTlv.HEADER_LENGTH + collectorInfo.length
                + LacpTlv.HEADER_LENGTH + terminatorInfo.length];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(lacpVersion);
        bb.put(TYPE_ACTOR);
        bb.put(LacpBaseTlv.LENGTH);
        bb.put(actorInfo);
        bb.put(TYPE_PARTNER);
        bb.put(LacpBaseTlv.LENGTH);
        bb.put(partnerInfo);
        bb.put(TYPE_COLLECTOR);
        bb.put(LacpCollectorTlv.LENGTH);
        bb.put(collectorInfo);
        bb.put(TYPE_TERMINATOR);
        bb.put(LacpTerminatorTlv.LENGTH);
        bb.put(terminatorInfo);

        return data;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), lacpVersion, tlv);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof Lacp)) {
            return false;
        }
        final Lacp other = (Lacp) obj;

        return this.lacpVersion == other.lacpVersion &&
                Objects.equals(this.tlv, other.tlv);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("lacpVersion", Byte.toString(lacpVersion))
                .add("tlv", tlv)
                .toString();
    }
}
