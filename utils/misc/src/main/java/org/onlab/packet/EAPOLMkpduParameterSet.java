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

/**
 * Class representing EAPOL MKPDU Parameter Set.
 * IEEE 802.1X Clause 11; Figure 11-7, Table 11-6
 */
public interface EAPOLMkpduParameterSet {
    // Parameter Set Types.
    public static final byte PARAMETERSET_TYPE_BASIC = 0;
    public static final byte PARAMETERSET_TYPE_LIVE_PEER_LIST = 1;
    public static final byte PARAMETERSET_TYPE_POTENTIAL_PEER_LIST = 2;
    public static final byte PARAMETERSET_TYPE_MACSEC_SAK_USE = 3;
    public static final byte PARAMETERSET_TYPE_DISTRIBUTED_SAK = 4;
    public static final byte PARAMETERSET_TYPE_ICV_INDICATOR = (byte) 255;

    // Member Identifier & Number fields.
    public static final int FIELD_MI_LENGTH = 12;
    public static final int FIELD_MN_LENGTH = 4;

    // SCI field details.
    public static final int FIELD_SCI_LENGTH = 8;

    // Body Length field details.
    public static final byte BODY_LENGTH_MSB_MASK = (byte) 0x0F;
    public static final byte BODY_LENGTH_MSB_SHIFT = (byte) 0x08;
    public static final byte BODY_LENGTH_OCTET_OFFSET = (byte) 0x04;

    /**
     * Retrieve Type of Parameter Set.
     *
     * @return parameter set type.
     */
    public byte getParameterSetType();

    /**
     * Total length; ie. including header and body length.
     *
     * @return short value.
     */
    public short getTotalLength();

    /**
     * Retrieve Body Length field of Parameter Set.
     *
     * @return body length of parameter set.
     */
    public short getBodyLength();

    /**
     * Utility function for Serializing Parameter Set.
     *
     * @return byte[] value
     */
    public byte[] serialize();
}
