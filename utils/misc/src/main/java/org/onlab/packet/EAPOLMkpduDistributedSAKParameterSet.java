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

/**
 * Class representing MKPDU MACSec SAK Use Parameter Set (GCM-AES 128).
 * IEEE 802.1X Clause 11; Figure 11-11
 */
public class EAPOLMkpduDistributedSAKParameterSet extends BasePacket implements EAPOLMkpduParameterSet {

    // Various fields.
    private byte distributedAN;
    private byte confidentialityOffset;
    private int keyNumber;
    private byte[] sak;

    /* Body Length is fixed in Distribute SAK Parameter Set.
     * Still variable kept for de-serialization purpose.
     */
    private short bodyLength;

    // Various Constants.
    public static final short TOTAL_DSAKPS_BODY_LENGTH = 32;
    public static final short SAK_FIELD_LENGTH = 24;
    public static final byte DSAKPS_GENERAL_MASK = 0x03;
    public static final byte DISTRIBUTED_AN_OFFSET = (byte) 0x06;
    public static final byte CONFIDENTIALITY_OFFSET = (byte) 0x04;

    // Key wrapping support.
    @FunctionalInterface
    public interface KeyWrapper {
        byte[] wrap(byte[] message);
    }

    KeyWrapper sakWrapper;

    @Override
    public byte getParameterSetType() {
        return PARAMETERSET_TYPE_DISTRIBUTED_SAK;
    }

    @Override
    public short getTotalLength() {
        return TOTAL_DSAKPS_BODY_LENGTH;
    }

    @Override
    public short getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(short bodyLength) {
        this.bodyLength = bodyLength;
    }

    @Override
    public byte[] serialize() {
        short length = getTotalLength();

        // Serialize Distribute SAK Parameter Set. IEEE 802.1x, Figure 11.10
        ByteBuffer data = ByteBuffer.wrap(new byte[length]);

        /*
         *Populate fields
         * Octet 1
         */
        data.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_DISTRIBUTED_SAK);

        // Octet 2
        byte octet = 0x00;
        octet = (byte) ((DSAKPS_GENERAL_MASK & distributedAN) << DISTRIBUTED_AN_OFFSET);
        octet |= (byte) ((DSAKPS_GENERAL_MASK & confidentialityOffset) << CONFIDENTIALITY_OFFSET);
        data.put(octet);

        // Octet 3 & 4
        length -= EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET;
        octet |= (byte) (length >> BODY_LENGTH_MSB_SHIFT & BODY_LENGTH_MSB_MASK);
        data.put(octet);
        data.put((byte) length);

        // Octet 5
        data.putInt(keyNumber);

        // AES Key Wrap of SAK
        data.put(sakWrapper.wrap(sak));

        return data.array();
    }

    /**
     * Deserializer function for Distributed SAK Parameter Set.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpduDistributedSAKParameterSet> deserializer() {
        return (data, offset, length) -> {

            // Needed components.
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpduDistributedSAKParameterSet dps = new EAPOLMkpduDistributedSAKParameterSet();

            /*
             * Extract fields.
             * Octet 2
             */
            byte[] mbField = new byte[1];
            mbField[0] = bb.get();
            dps.setDistributedAN((byte) ((mbField[0] >> DISTRIBUTED_AN_OFFSET) & DSAKPS_GENERAL_MASK));
            dps.setConfidentialityOffset((byte) ((mbField[0] >> CONFIDENTIALITY_OFFSET) & DSAKPS_GENERAL_MASK));

            // Octet 3 & 4
            mbField[0] = bb.get();
            short bodyLength = (short) (((short) (mbField[0] & EAPOLMkpduParameterSet.BODY_LENGTH_MSB_MASK))
                    << EAPOLMkpduParameterSet.BODY_LENGTH_MSB_SHIFT);
            bodyLength |= (short) (bb.get());
            dps.setBodyLength(bodyLength);

            // Octet 5
            dps.setKeyNumber(bb.getInt());

            // SAK
            mbField = new byte[EAPOLMkpduDistributedSAKParameterSet.SAK_FIELD_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduDistributedSAKParameterSet.SAK_FIELD_LENGTH);
            dps.setSAK(mbField);

            return dps;
        };
    }

    // Distributed AN
    public void setDistributedAN(byte distributedAN) {
        this.distributedAN = distributedAN;
    }

    // Confidentiality Offset
    public void setConfidentialityOffset(byte confidentialityOffset) {
        this.confidentialityOffset = confidentialityOffset;
    }

    // Key Number
    public void setKeyNumber(int keyNumber) {
        this.keyNumber = keyNumber;
    }

    // SAK
    public void setSAK(byte[] sak) {
        this.sak = sak;
    }

    // Key Wrapper
    public void setKeyWrapper(KeyWrapper sakWrapper) {
        this.sakWrapper = sakWrapper;
    }

}

