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
 * Class representing MKPDU MACSec SAK Use Parameter Set.
 * IEEE 802.1X Clause 11; Figure 11-10
 */
public class EAPOLMkpduMACSecUseParameterSet extends BasePacket implements EAPOLMkpduParameterSet {

    // Various Header Fields
    private boolean delayProtect = false;
    private boolean plainTX = false;
    private boolean plainRX = false;
    private byte[] latestKI;
    private int latestKN;
    private byte latestAN;
    private int latestLAPN;
    private boolean latestTX;
    private boolean latestRX;
    private byte[] oldKI;
    private int oldKN;
    private byte oldAN;
    private int oldLAPN;
    private boolean oldTX;
    private boolean oldRX;

    /* Body Length is fixed in SAK Use Parameter Set.
     * Still variable kept for de-serialization purpose.
     * */
    private short bodyLength;

    // Various constants.
    public static final short TOTAL_SUPS_BODY_LENGTH = 44;
    public static final short LATEST_KEY_AN_OFFSET = 6;
    public static final short OLD_KEY_AN_OFFSET = 2;

    public static final byte LATEST_KEY_RX_MASK = 0x10;
    public static final byte LATEST_KEY_TX_MASK = 0X20;
    public static final byte OLD_KEY_RX_MASK = 0x01;
    public static final byte OLD_KEY_TX_MASK = 0x02;
    public static final byte KEY_AN_MASK = 0x02;
    public static final byte PLAIN_TX_MASK = (byte) 0x80;
    public static final byte PLAIN_RX_MASK = 0x40;
    public static final byte DELAY_PROTECT_MASK = 0x10;
    public static final short SUPS_FIXED_PART_TOTAL_SIZE = 44;

    @Override
    public byte[] serialize() {
        short length = getTotalLength();

        // Serialize SAK Use Parameter Set. IEEE 802.1x, Figure 11.10
        ByteBuffer data = ByteBuffer.wrap(new byte[length]);

        /*
         *Populate fields
         * Octet 1
         * */
        data.put(EAPOLMkpduParameterSet.PARAMETERSET_TYPE_MACSEC_SAK_USE);

        // Octet 2
        byte octet = 0x00;
        octet = (byte) ((latestRX) ? octet | LATEST_KEY_RX_MASK : octet & (byte) (~LATEST_KEY_RX_MASK));
        octet = (byte) ((latestTX) ? octet | LATEST_KEY_TX_MASK : octet & (byte) (~LATEST_KEY_TX_MASK));
        octet = (byte) ((oldRX) ? octet | OLD_KEY_RX_MASK : octet & (byte) (~OLD_KEY_RX_MASK));
        octet = (byte) ((oldTX) ? octet | OLD_KEY_TX_MASK : octet & (byte) (~OLD_KEY_TX_MASK));
        octet |= latestAN << LATEST_KEY_AN_OFFSET;
        octet |= oldAN << OLD_KEY_AN_OFFSET;
        data.put(octet);

        // Octet 3
        octet = 0x00;
        octet = (byte) ((plainTX) ? octet | (byte) PLAIN_TX_MASK : octet & (byte) (~PLAIN_TX_MASK));
        octet = (byte) ((plainRX) ? octet | (byte) PLAIN_RX_MASK : octet & (byte) (~PLAIN_RX_MASK));
        octet = (byte) ((delayProtect) ? octet | DELAY_PROTECT_MASK : octet & (byte) (~DELAY_PROTECT_MASK));
        length -= EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET;
        octet |= (byte) (length >> BODY_LENGTH_MSB_SHIFT & BODY_LENGTH_MSB_MASK);
        data.put(octet);

        // Octet 4
        data.put((byte) length);

        // Latest & Old Key Server Details
        data.put(latestKI);
        data.putInt(latestKN);
        data.putInt(latestLAPN);
        data.put(oldKI);
        data.putInt(oldKN);
        data.putInt(oldLAPN);

        return data.array();
    }

    /**
     * Deserializer function for MACSec SAK Use Parameter Set.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpduMACSecUseParameterSet> deserializer() {
        return (data, offset, length) -> {

            // Needed components.
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpduMACSecUseParameterSet macSecUsePS = new EAPOLMkpduMACSecUseParameterSet();

            /*
             *Extract fields.
             *Octet 2
            **/
            byte[] mbField = new byte[1];
            mbField[0] = bb.get();
            macSecUsePS.setOldRX((mbField[0] & OLD_KEY_RX_MASK) != 0);
            macSecUsePS.setOldTX((mbField[0] & OLD_KEY_TX_MASK) != 0);
            macSecUsePS.setLatestRX((mbField[0] & OLD_KEY_RX_MASK) != 0);
            macSecUsePS.setLatestTX((mbField[0] & OLD_KEY_TX_MASK) != 0);
            macSecUsePS.setLatestAN((byte) ((mbField[0] >> LATEST_KEY_AN_OFFSET) & KEY_AN_MASK));
            macSecUsePS.setOldAN((byte) ((mbField[0] >> OLD_KEY_AN_OFFSET) & KEY_AN_MASK));

            // Octet 3 & 4
            mbField[0] = bb.get();
            macSecUsePS.setPlainRX((mbField[0] & PLAIN_RX_MASK) != 0);
            macSecUsePS.setPlainTX((mbField[0] & PLAIN_TX_MASK) != 0);
            macSecUsePS.setDelayProtect((mbField[0] & DELAY_PROTECT_MASK) != 0);

            short bodyLength = (short) (((short) (mbField[0] & EAPOLMkpduParameterSet.BODY_LENGTH_MSB_MASK))
                    << EAPOLMkpduParameterSet.BODY_LENGTH_MSB_SHIFT);
            bodyLength |= (short) (bb.get());
            macSecUsePS.setBodyLength(bodyLength);

            // Latest Key Server details.
            mbField = new byte[EAPOLMkpduParameterSet.FIELD_MI_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduParameterSet.FIELD_MI_LENGTH);
            macSecUsePS.setLatestKI(mbField);
            macSecUsePS.setLatestKN(bb.getInt());
            macSecUsePS.setLatestLAPN(bb.getInt());

            // Old Key Server details.
            mbField = new byte[EAPOLMkpduParameterSet.FIELD_MI_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduParameterSet.FIELD_MI_LENGTH);
            macSecUsePS.setOldKI(mbField);
            macSecUsePS.setOldKN(bb.getInt());
            macSecUsePS.setOldLAPN(bb.getInt());

            return macSecUsePS;
        };
    }

    @Override
    public byte getParameterSetType() {
        return PARAMETERSET_TYPE_MACSEC_SAK_USE;
    }

    @Override
    public short getTotalLength() {
        return TOTAL_SUPS_BODY_LENGTH;
    }

    @Override
    public short getBodyLength() {
        return bodyLength;
    }

    public void setBodyLength(short bodyLength) {
        this.bodyLength = bodyLength;
    }

    /**
     * To set Delay Protect.
     *
     * @param delayProtect , type boolean
     */
    public void setDelayProtect(boolean delayProtect) {
        this.delayProtect = delayProtect;
    }

    /**
     * To set Plain TX supported or not.
     *
     * @param plainTX , type boolean
     */
    public void setPlainTX(boolean plainTX) {
        this.plainTX = plainTX;
    }

    /**
     * Plain RX supported or not.
     *
     * @param plainRX , type boolean
     */
    public void setPlainRX(boolean plainRX) {
        this.plainRX = plainRX;
    }

    /**
     * Lowest Acceptable Packet Number for Latest Key Server.
     *
     * @param latestLAPN ,type int
     */
    public void setLatestLAPN(int latestLAPN) {
        this.latestLAPN = latestLAPN;
    }

    /**
     * Latest Key Server Association Number.
     *
     * @param latestAN , type byte
     */
    public void setLatestAN(byte latestAN) {
        this.latestAN = latestAN;
    }

    /**
     * Latest Key Server Identifier.
     *
     * @param latestKI ,type byte[]
     */
    public void setLatestKI(byte[] latestKI) {
        this.latestKI = latestKI;
    }

    /**
     * Latest Key Server Key Number.
     *
     * @param latestKN ,type int
     */
    public void setLatestKN(int latestKN) {
        this.latestKN = latestKN;
    }

    /**
     * Latest Key Server used for TX protection.
     *
     * @param latestTX ,type boolean
     */
    public void setLatestTX(boolean latestTX) {
        this.latestTX = latestTX;
    }

    /**
     * Latest Key Server used for RX protection .
     *
     * @param latestRX ,type boolean.
     */
    public void setLatestRX(boolean latestRX) {
        this.latestRX = latestRX;
    }

    /**
     * Lowest Acceptable Packet Number for Old Key Server.
     *
     * @param oldLAPN , type int
     */
    public void setOldLAPN(int oldLAPN) {
        this.oldLAPN = oldLAPN;
    }

    /**
     * Old Key Server Association Number.
     *
     * @param oldAN , type byte
     */
    public void setOldAN(byte oldAN) {
        this.oldAN = oldAN;
    }

    /**
     * Old Key Server Identifier.
     *
     * @param oldKI , type byte[]
     */
    public void setOldKI(byte[] oldKI) {
        this.oldKI = oldKI;
    }

    /**
     * Old Key Server Number.
     *
     * @param oldKN , type int
     */
    public void setOldKN(int oldKN) {
        this.oldKN = oldKN;
    }

    /**
     * Old Key Server used for TX protection.
     *
     * @param oldTX ,type boolean
     */
    public void setOldTX(boolean oldTX) {
        this.oldTX = oldTX;
    }

    /**
     * Old Key Server used for RX protection.
     *
     * @param oldRX , type boolean
     */
    public void setOldRX(boolean oldRX) {
        this.oldRX = oldRX;
    }
}
