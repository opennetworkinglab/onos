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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class representing MKPDU Live/Potential Peer List Parameter Set.
 * IEEE 802.1X Clause 11; Figure 11-9
 */
public class EAPOLMkpduPeerListParameterSet extends BasePacket implements EAPOLMkpduParameterSet {

    // Member Details
    public static class MemberDetails {
        byte[] memberID;
        int messageNo;

        public MemberDetails(byte[] memberID, int messageNo) {
            this.memberID = memberID;
            this.messageNo = messageNo;
        }

        public byte[] getMemberID() {
            return memberID;
        }

        public int getMessageNo() {
            return messageNo;
        }
    }

    // Peer List Types
    private static final byte PEER_LIST_TYPE_LIVE = 1;
    private static final byte PEER_LIST_TYPE_POTENTIAL = 2;

    // Type for distinguishing Live & Potential Lists.
    private byte peerListType = 1;
    private short bodyLength;

    //Members
    protected List<MemberDetails> members = new ArrayList<>();

    @Override
    public byte[] serialize() {

        // Don't Serialize if no members are available.
        if (members.size() == 0) {
            return null;
        }

        // Serialize PeerList Parameter Set. IEEE 802.1x, Figure 11.9
        short length = getTotalLength();
        ByteBuffer data = ByteBuffer.wrap(new byte[length]);

        /*
         *Populate fields
         * Octet 1
         */
        data.put(peerListType);

        // Octet 2. Reserved.
        byte octet = 0x00;
        data.put(octet);

        // Octet 3
        length -= EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET;
        octet |= (byte) (length >> BODY_LENGTH_MSB_SHIFT & BODY_LENGTH_MSB_MASK);
        data.put(octet);

        // Octet 4
        data.put((byte) length);

        // Member details.
        members.forEach(a -> {
                    data.put(a.getMemberID());
                    data.putInt(a.getMessageNo());
                }
        );

        return data.array();
    }


    /**
     * Deserializer function for Peer List Parameter Set.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpduPeerListParameterSet> deserializer() {
        return (data, offset, length) -> {

            // Ensure buffer has enough details.
            if (data == null) {
                return null;
            }

            // Deserialize Basic Parameter Set.
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpduPeerListParameterSet peerListParameterSet =
                    new EAPOLMkpduPeerListParameterSet();

            // Parse Peer List Fields/
            byte[] mbField = new byte[1];
            // mbField[0] = bb.get(); // Skip Type. Already processed in EAPOL-MKPDU de-serializer.
            bb.get(); // Skip Reserved.

            // Length
            mbField[0] = bb.get();
            short bodyLength = (short) (((short) (mbField[0] & EAPOLMkpduParameterSet.BODY_LENGTH_MSB_MASK))
                    << EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET);
            bodyLength |= (short) (bb.get());
            peerListParameterSet.setBodyLength(bodyLength);

            // Member details
            while (bodyLength > 0) {
                mbField = new byte[FIELD_MI_LENGTH];
                bb.get(mbField, 0, FIELD_MI_LENGTH);
                peerListParameterSet.addMember(mbField, bb.getInt());
                bodyLength -= FIELD_MI_LENGTH + FIELD_MN_LENGTH;
            }
            return peerListParameterSet;
        };
    }

    /**
     * Setting List Type.
     *
     * @param peerListType type - PEERLIST_TYPE_LIVE or PEERLIST_TYPE_POTENTIAL for live
     *                     and potential peer lists
     */
    public void setPeerListType(byte peerListType) {
        if ((peerListType != EAPOLMkpduPeerListParameterSet.PEER_LIST_TYPE_LIVE) &&
                (peerListType != EAPOLMkpduPeerListParameterSet.PEER_LIST_TYPE_POTENTIAL)) {
            throw new IllegalArgumentException("Unknown PeerList Type specified.");
        }
        this.peerListType = peerListType;
    }

    /**
     * Member details adding.
     *
     * @param mi ,type byte[]
     * @param mn , type int
     */
    public void addMember(byte[] mi, int mn) {
        if (mi != null) {
            members.add(new MemberDetails(mi, mn));
        }
        return;
    }

    /**
     * Searching Member details.
     *
     * @param mi ,type byte[]
     * @return boolean based on the value of member.
     */
    public boolean memberExists(byte[] mi) {
        MemberDetails member = members.stream()
                .filter(m -> Arrays.equals(m.getMemberID(), mi))
                .findAny()
                .orElse(null);
        return (member != null) ? true : false;
    }

    /**
     * Member details.
     *
     * @return members
     */
    public List<MemberDetails> getMembers() {
        return members;
    }

    @Override
    public byte getParameterSetType() {
        return peerListType;
    }

    @Override
    public short getTotalLength() {
        return (short) (EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET +
                members.size() * (EAPOLMkpduParameterSet.FIELD_MI_LENGTH +
                        EAPOLMkpduParameterSet.FIELD_MN_LENGTH));
    }

    @Override
    public short getBodyLength() {
        return bodyLength;
    }

    /**
     * Body Length.
     *
     * @param length ,type short
     */
    public void setBodyLength(short length) {
        this.bodyLength = length;
    }
}

