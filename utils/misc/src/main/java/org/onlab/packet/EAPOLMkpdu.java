
/*
 * Copyright 2017-present Open Networking Laboratory
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
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * EAPOL MKA (EAPOL MAC Key Agreement Protocol) header.
 */
public class EAPOLMkpdu extends BasePacket {

    // Parameter Sets.
    private Map<Byte, IPacket> parameterSets = new LinkedHashMap<>();

    /*
     * Parameter Serialization Order.
     * IEEE 802.1x Clause 11.11.3.
     */
    private static byte[] parametersetSerializerKeyList = new byte[]{
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC,
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_LIVE_PEER_LIST,
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_POTENTIAL_PEER_LIST,
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_MACSEC_SAK_USE,
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_DISTRIBUTED_SAK,
            // TODO: Fill other types.
            EAPOLMkpduParameterSet.PARAMETERSET_TYPE_ICV_INDICATOR
    };


     // Various Parameter Set Deserializers.

    private static final Map<Byte, Deserializer<? extends IPacket>> PARAMETERSET_DESERIALIZER_MAP =
            new LinkedHashMap<>();

    static {
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC,
                EAPOLMkpduBasicParameterSet.deserializer());
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_LIVE_PEER_LIST,
                EAPOLMkpduPeerListParameterSet.deserializer());
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_POTENTIAL_PEER_LIST,
                EAPOLMkpduPeerListParameterSet.deserializer());
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_MACSEC_SAK_USE,
                EAPOLMkpduMACSecUseParameterSet.deserializer());
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_DISTRIBUTED_SAK,
                EAPOLMkpduDistributedSAKParameterSet.deserializer());
        EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_ICV_INDICATOR,
                EAPOLMkpduICVIndicatorParameterSet.deserializer());
    }

    @Override
    public byte[] serialize() {
        int payloadLength = packetLength();
        ByteBuffer payload = ByteBuffer.wrap(new byte[payloadLength]);


        //Serialize Parameter Sets.
        for (byte b : parametersetSerializerKeyList) {
            IPacket packet = parameterSets.get(b);
            if (packet != null) {
                byte[] data = packet.serialize();
                if (data != null) {
                    payload.put(data);
                }
            }
        }
        return payload.array();
    }

    /**
     * Static deserializer for EAPOL-MKA packets.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpdu> deserializer() {
        return (data, offset, length) -> {
            byte parameterSetType;
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpdu mkpdu = new EAPOLMkpdu();

                /* Extract Basic ParameterSet;
                   Special care needed, MKA Version & Peer Type difficult to distinguish. */
            Deserializer<? extends IPacket> psDeserializer =
                    EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.get(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC);
            EAPOLMkpduParameterSet ps = (EAPOLMkpduParameterSet) (psDeserializer.deserialize(bb.array(),
                    bb.position(), bb.remaining()));
            if (!mkpdu.addParameterSet(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC, ps)) {
                throw new DeserializationException("Error in deserializing packets");
            }
                // Update buffer position.
            bb.position(bb.position() + ps.getTotalLength());

                // Extract various remaining Parameter Sets.
            while (bb.hasRemaining()) {
                parameterSetType = bb.get();
                psDeserializer = EAPOLMkpdu.PARAMETERSET_DESERIALIZER_MAP.get(parameterSetType);
                ps = (EAPOLMkpduParameterSet) (psDeserializer.deserialize(bb.array(), bb.position(),
                        bb.remaining()));
                    // Specially handle Peer List Parameter Sets .
                if ((parameterSetType == EAPOLMkpduParameterSet.PARAMETERSET_TYPE_LIVE_PEER_LIST) ||
                        (parameterSetType == EAPOLMkpduParameterSet.PARAMETERSET_TYPE_POTENTIAL_PEER_LIST)) {
                    EAPOLMkpduPeerListParameterSet peerList =
                            (EAPOLMkpduPeerListParameterSet) ps;
                    peerList.setPeerListType(parameterSetType);
                }
                if (!mkpdu.addParameterSet(parameterSetType, ps)) {
                    throw new DeserializationException("Error in deserializing packets");
                }

                    // Update buffer.
                short consumed = ps.getTotalLength();
                short remaining = (short) bb.remaining();
                    // Already one byte shifted, only "consumed-1" is to be shifted.
                bb.position(bb.position() + ((remaining > consumed) ? (consumed - 1) : remaining));
            }
            return mkpdu;
        };
    }

    /**
     * Populate various Parameter Sets to store.
     *
     * @param type short
     * @param ps EAPOLMkpduParameterSet
     * @return boolean
     */
    public boolean addParameterSet(short type, EAPOLMkpduParameterSet ps) {
        if (ps == null) {
            return false;
        }

        // Ensure type is valid.
        if (!((EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC <= type &&
                type <= EAPOLMkpduParameterSet.PARAMETERSET_TYPE_DISTRIBUTED_SAK) ||
                (type == EAPOLMkpduParameterSet.PARAMETERSET_TYPE_ICV_INDICATOR))) {
            return false;
        }


        // Update store.
        parameterSets.put((byte) type, (IPacket) ps);

        return true;
    }

    /**
     * Provide Basic Parameter Set details.
     *
     * @return EAPOLMkpduBasicParameterSet
     */
    public EAPOLMkpduBasicParameterSet getBasicParameterSet() {
        IPacket parameterSet = null;
        if (parameterSets.containsKey(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC)) {
            parameterSet = parameterSets.get(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_BASIC);
        }
        return (EAPOLMkpduBasicParameterSet) parameterSet;
    }

    /**
     * Provide Live/Potential Peer List details.
     *
     * @return EAPOLMkpduPeerListParameterSet
     */
    public EAPOLMkpduPeerListParameterSet getPeerListParameterSet() {
        IPacket parameterSet;
        if (parameterSets.containsKey(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_LIVE_PEER_LIST)) {
            parameterSet = parameterSets.get(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_LIVE_PEER_LIST);
        } else {
            parameterSet = parameterSets.get(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_POTENTIAL_PEER_LIST);
        }
        return (EAPOLMkpduPeerListParameterSet) parameterSet;
    }

    /**
     * Total EAPOL-MKPDU packet length. Cumulative length of Parameter Sets.
     *
     * @return length
     */
    public short packetLength() {
        short length = 0;
        for (byte k : parameterSets.keySet()) {
            EAPOLMkpduParameterSet p = (EAPOLMkpduParameterSet) parameterSets.get(k);
            length += p.getTotalLength();
        }
        return length;
    }

    /**
     * Retrieve Parameter Set based on type.
     *
     * @param type byte
     * @return EAPOLMkpduParameterSet
     */
    public EAPOLMkpduParameterSet getParameterSet(byte type) {
        EAPOLMkpduParameterSet ps = null;
        Map.Entry<Byte, IPacket> entry = parameterSets.entrySet().stream()
                .filter((i) -> {
                    return i.getKey().equals(new Byte(type));
                })
                .findFirst()
                .orElse(null);
        if (entry != null) {
            ps = (EAPOLMkpduParameterSet) entry.getValue();
        }
        return ps;
    }

}

