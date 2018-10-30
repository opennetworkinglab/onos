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
 * Class representing MKPDU ICV Indicator.
 * IEEE 802.1X Clause 11; Figure 11-16
 */
public class EAPOLMkpduICVIndicatorParameterSet extends BasePacket implements EAPOLMkpduParameterSet {

    // Variables for Hash Generation.
    private byte[] icv;

    /*
     * Body Length is fixed in SAK Use Parameter Set.
     * Still variable kept for de-serialization purpose.
     */
    private short bodyLength;

    // Total packet length. Currently only 128bit ICV is supported.
    public static final short TOTAL_ICVPS_BODY_LENGTH = 20;


    @Override
    public byte[] serialize() {
        short length = getTotalLength();

        // Serialize ICV Indicator Parameter Set. IEEE 802.1x, Figure 11.16 .
        ByteBuffer data = ByteBuffer.wrap(new byte[length]);

        /*
         * Populate fields
         * Octet 1
         */
        data.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_ICV_INDICATOR);

        // Octet 2. Reserved.
        byte octet = 0x00;
        data.put(octet);

        // Octet 3
        length -= EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET;
        octet |= (byte) (length >> BODY_LENGTH_MSB_SHIFT & BODY_LENGTH_MSB_MASK);
        data.put(octet);

        // Octet 4
        data.put((byte) length);

        // Note : ICV generation excluded from serialization.
        //        Should be done at the level where ethernet packet is being created.

        return data.array();
    }

    /**
     * Deserializer function for ICV Indicator Parameter Set.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpduICVIndicatorParameterSet> deserializer() {
        return (data, offset, length) -> {
            // TODO : Ensure buffer has enough details.

            // Deserialize Basic Parameter Set.
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpduICVIndicatorParameterSet icvps = new EAPOLMkpduICVIndicatorParameterSet();

            // Extract fields. Skip type, reserved fields.
            byte[] mbField = new byte[2];
            bb.get(mbField, 0, 2);
            short bodyLength = (short) (((short) (mbField[1] & EAPOLMkpduParameterSet.BODY_LENGTH_MSB_MASK))
                    << EAPOLMkpduParameterSet.BODY_LENGTH_MSB_SHIFT);
            bodyLength |= (short) (bb.get());
            icvps.setBodyLength(bodyLength);

            // SAK
            byte[] icv = new byte[bodyLength];
            bb.get(icv);
            icvps.setICV(icv);

            return icvps;
        };
    }

    @Override
    public byte getParameterSetType() {
        return PARAMETERSET_TYPE_ICV_INDICATOR;
    }

    @Override
    public short getTotalLength() {
        return TOTAL_ICVPS_BODY_LENGTH;
    }

    @Override
    public short getBodyLength() {
        return TOTAL_ICVPS_BODY_LENGTH - BODY_LENGTH_OCTET_OFFSET;
    }

    /**
     * To set body length.
     *
     * @param bodyLength  ,type short
     */
    public void setBodyLength(short bodyLength) {
        this.bodyLength = bodyLength;
    }

    /**
     * To set ICV.
     *
     * @param icv , type byte[]
     */
    public void setICV(byte[] icv) {
        this.icv = icv;
    }

}

