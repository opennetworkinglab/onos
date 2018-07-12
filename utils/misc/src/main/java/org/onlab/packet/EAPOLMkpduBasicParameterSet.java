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
import java.util.Arrays;

/**
 * Class representing EAPOL MKPDU Basic Parameter Set.
 * IEEE 802.1X Clause 11; Figure 11-8
 */
public class EAPOLMkpduBasicParameterSet extends BasePacket implements EAPOLMkpduParameterSet {

    // Parameter Set fields.
    private byte mkaVersion;
    private byte keyServerPriority;
    private boolean keyServer;
    private boolean macSecDesired;
    private byte capability;
    private short bodyLength;
    private SCI sci;
    private byte[] mi;
    private int mn;
    private byte[] algAgility;
    private byte[] ckn;
    private byte[] padding;

    // Various fixed parameter widths. IEEE 802.1X Table 11-6.
    public static final int FIELD_ALGAG_LENGTH = 4;
    public static final int TOTAL_BPS_BODY_LENGTH = 32;


    // Basic Parameter Set field masks.
    public static final byte KEYSERVER_MASK = (byte) 0x80;
    public static final byte KEYSERVER_OFFSET = (byte) 0x07;
    public static final byte MACSEC_DESIRED_MASK = (byte) 0x70;
    public static final byte MACSEC_DESIRED_OFFSET = (byte) 0x06;
    public static final byte MACSEC_CAPABILITY_MASK = (byte) 0x30;
    public static final byte MACSEC_CAPABILITY_OFFSET = (byte) 0x04;

    /**
     * MKA Secure Channel Identifier.
     */
    public static class SCI {
        private byte[] address;
        private short port;

        public static final int SYSTEM_IDENTIFIER_LENGTH = 6;
        public static final int PORT_OFFSET = 6;


        /**
         * Validate SCI has <MAC (6 bytes)><Port (2 bytes)> length.
         *
         * @param sci ,byte[]
         * @return true ,boolean
         */
        private boolean validateSCI(byte[] sci) {
            if (sci != null && sci.length < EAPOLMkpduBasicParameterSet.FIELD_SCI_LENGTH) {
                throw new IllegalArgumentException(
                        "Invalid SCI argument. Enough bytes are not provided."
                );
            }
            return true;
        }

        /**
         * Validate System Identifier.
         *
         * @param address , byte[]
         * @return true , boolean
         */
        private boolean validateAddress(byte[] address) {
            if (address != null && address.length < SCI.SYSTEM_IDENTIFIER_LENGTH) {
                throw new IllegalArgumentException(
                        "Invalid System Identifier argument. Expects 6 bytes eg. MAC address.");
            }
            return true;
        }

        /**
         * To set SCI from MAC address and port stream.
         *
         * @param sci , type byte[]
         */

        public SCI(byte[] sci) {
            validateSCI(sci);
            address = Arrays.copyOfRange(sci, 0, SYSTEM_IDENTIFIER_LENGTH);
            port = (short) ((((short) (sci[PORT_OFFSET] & 0xFF)) << 8)
                    | ((short) (sci[PORT_OFFSET + 1] & 0xFF)));
        }

        /**
         * To set SCI from MAC address and port number.
         *
         * @param address ,type byte[]
         * @param port    ,type short
         * @throws IllegalArgumentException Exceptions
         */
        public SCI(byte[] address, short port) throws IllegalArgumentException {
            validateAddress(address);
            this.address = address;
            this.port = port;
        }

        /**
         * To set address.
         *
         * @param address , type byte[]
         * @throws IllegalArgumentException if address is not set
         */
        public void setAdddress(byte[] address) throws IllegalArgumentException {
            validateAddress(address);
            this.address = address;
        }

        /**
         * To return address.
         *
         * @return address , type byte[]
         */
        public byte[] address() {
            return address;
        }

        /**
         * TO set Port.
         *
         * @param port , type short
         */
        public void setPort(short port) {
            this.port = port;
        }

        /**
         * To return Port.
         *
         * @return port , type short
         */
        public short port() {
            return port;
        }

        /**
         * Convert to byte array.
         *
         * @return bb.array() ,type byte[]
         */
        public byte[] array() {
            byte[] data = new byte[address.length + 2];
            ByteBuffer bb = ByteBuffer.wrap(data);
            bb.put(address);
            bb.putShort(port);
            return bb.array();
        }
    }

    // Basic Parameter Set fixed header portion size.
    public static final short BPS_FIXED_PART_SIZE_UPTO_LENGTH_FIELD = 4;
    public static final short BPS_FIXED_PART_TOTAL_SIZE = 32;

    /**
     * To set MKA Version.
     *
     * @param version , type byte
     */
    public void setMkaVersion(byte version) {
        this.mkaVersion = version;
    }

    /**
     * To get MKA Version.
     *
     * @return mkaVersion , type byte
     */
    public byte getMkaVersion() {
        return mkaVersion;
    }

    /**
     * To set Key Server Priority.
     *
     * @param priority  , type byte
     */

    public void setKeyServerPriority(byte priority) {
        this.keyServerPriority = priority;
    }

    /**
     * To get Key Server Priority.
     *
     * @return keyServerPriority, type byte
     */
    public byte getKeyServerPriority() {
        return keyServerPriority;
    }

    /**
     * To set Key Server.
     *
     * @param isKeyServer , type boolean
     */
    public void setKeyServer(boolean isKeyServer) {
        this.keyServer = isKeyServer;
    }

    /**
     * To get Key Server.
     *
     * @return keyServer, type boolean
     */
    public boolean getKeyServer() {
        return keyServer;
    }

    /**
     * To set MACSec Desired.
     *
     * @param desired , type boolean
     */
    public void setMacSecDesired(boolean desired) {
        this.macSecDesired = desired;
    }

    /**
     * To get MACSec Desired.
     *
     * @return macSecDesired , type boolean
     */
    public boolean getMacSecDesired() {
        return macSecDesired;
    }

    /**
     * To set MACSec Capacity.
     *
     * @param capability ,type byte
     */
    public void setMacSecCapability(byte capability) {
        this.capability = capability;
    }

    /**
     * To get MACSec Capacity.
     *
     * @return capability, type byte
     */
    public byte getMacSecCapacity() {
        return capability;
    }

    /**
     * To set body length.
     *
     * @param length , type short
     */
    public void setBodyLength(short length) {
        this.bodyLength = length;
    }

    public short getBodyLength() {
        return bodyLength;
    }

    /**
     * To set SCI.
     *
     * @param sci , byte[]
     */
    public void setSci(byte[] sci) {
        this.sci = new SCI(sci);
    }

    /**
     * To set SCI.
     *
     * @param sci , SCI
     */
    public void setSci(SCI sci) {
        // TODO: Ensure sci valid.
        this.sci = sci;
    }

    /**
     * To get SCI.
     *
     * @return sci, type SCI
     */
    public SCI getSci() {
        return sci;
    }

    /**
     * To set Member Identifier.
     *
     * @param mi , type byte[]
     * @throws IllegalArgumentException if mi is not set.
     */

    public void setActorMI(byte[] mi) throws IllegalArgumentException {
        if (mi != null && mi.length < EAPOLMkpduParameterSet.FIELD_MI_LENGTH) {
            throw new IllegalArgumentException("Actor Message Identifier doesn't have enough length.");
        }
        this.mi = mi;
    }

    /**
     * To get Member Identifier.
     *
     * @return mi, type byte[]
     */
    public byte[] getActorMI() {
        return mi;
    }

    /**
     * To set Member Identifier.
     *
     * @param mn , type byte[]
     * @throws IllegalArgumentException if mn is not set.
     */
    public void setActorMN(byte[] mn) throws IllegalArgumentException {
        if (mn != null && mn.length < EAPOLMkpduParameterSet.FIELD_MN_LENGTH) {
            throw new IllegalArgumentException("Actor Message Number doesn't have enough length.");
        }
        final ByteBuffer bf = ByteBuffer.wrap(mn);
        this.mn = bf.getInt();
    }

    /**
     * To set Member Identifier.
     *
     * @param mn , type int
     */
    public void setActorMN(int mn) {
        this.mn = mn;
    }

    /**
     * To get Member Identifier.
     *
     * @return mn, type int
     */
    public int getActorMN() {
        return mn;
    }

    /**
     * To set Algorithm Agility.
     *
     * @param algAgility , type byte[]
     * @throws IllegalArgumentException if algAgility is not set or in incorrect format
     */
    public void setAlgAgility(byte[] algAgility) throws IllegalArgumentException {
        if (algAgility != null && algAgility.length < EAPOLMkpduBasicParameterSet.FIELD_ALGAG_LENGTH) {
            throw new IllegalArgumentException("Algorithm Agility doesn't have enough length.");
        }
        this.algAgility = algAgility;
    }

    /**
     * To get Algorithm Agility.
     *
     * @return algAgility, type byte[]
     */
    public byte[] getAlgAgility() {
        return algAgility;
    }

    /**
     * To set CAK name.
     *
     * @param ckn , type byte[]
     */
    public void setCKN(byte[] ckn) {
        int cakNameLength = bodyLength - EAPOLMkpduBasicParameterSet.TOTAL_BPS_BODY_LENGTH;
        if (ckn != null && ckn.length < cakNameLength) {
            throw new IllegalArgumentException("CAK name doesn't have enough length.");
        }
        this.ckn = ckn;
    }

    /**
     * To get CAK name.
     *
     * @return ckn , type byte[]
     */
    public byte[] getCKN() {
        return ckn;
    }

    /**
     * To set padding.
     *
     * @param padding , type byte[]
     */
    public void setPadding(byte[] padding) {
        this.padding = padding;
    }

    /**
     * Deserializer function for Basic Parameter Set.
     *
     * @return deserializer function
     */
    public static Deserializer<EAPOLMkpduBasicParameterSet> deserializer() {
        return (data, offset, length) -> {

            // Ensure buffer has enough details.
            if (length < TOTAL_BPS_BODY_LENGTH) {
                return null;
            }

            // Various tools for deserialization.
            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            EAPOLMkpduBasicParameterSet basicParameterSet = new EAPOLMkpduBasicParameterSet();

            // Deserialize Basic Parameter Set fields.
            basicParameterSet.setMkaVersion(bb.get());
            basicParameterSet.setKeyServerPriority(bb.get());

            byte[] mbField = new byte[1];
            mbField[0] = bb.get();
            basicParameterSet.setKeyServer(((mbField[0] & EAPOLMkpduBasicParameterSet.KEYSERVER_MASK) > 0) ?
                    true : false);
            basicParameterSet.setMacSecDesired(((mbField[0]
                    & EAPOLMkpduBasicParameterSet.MACSEC_DESIRED_MASK) > 0) ?
                    true : false);
            basicParameterSet.setMacSecCapability((byte) ((mbField[0]
                    & EAPOLMkpduBasicParameterSet.MACSEC_CAPABILITY_MASK)
                    >> EAPOLMkpduBasicParameterSet.MACSEC_CAPABILITY_OFFSET));

            short bodyLength = (short) (((short) (mbField[0] & EAPOLMkpduParameterSet.BODY_LENGTH_MSB_MASK))
                    << EAPOLMkpduBasicParameterSet.BODY_LENGTH_MSB_SHIFT);
            bodyLength |= (short) (bb.get());
            basicParameterSet.setBodyLength(bodyLength);

            mbField = new byte[EAPOLMkpduParameterSet.FIELD_SCI_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduParameterSet.FIELD_SCI_LENGTH);
            basicParameterSet.setSci(mbField);

            mbField = new byte[EAPOLMkpduParameterSet.FIELD_MI_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduParameterSet.FIELD_MI_LENGTH);
            basicParameterSet.setActorMI(mbField);

            mbField = new byte[EAPOLMkpduParameterSet.FIELD_MN_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduParameterSet.FIELD_MN_LENGTH);
            basicParameterSet.setActorMN(mbField);

            mbField = new byte[EAPOLMkpduBasicParameterSet.FIELD_ALGAG_LENGTH];
            bb.get(mbField, 0, EAPOLMkpduBasicParameterSet.FIELD_ALGAG_LENGTH);
            basicParameterSet.setAlgAgility(mbField);

            int cakNameLength = basicParameterSet.getBodyLength() + EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET -
                    EAPOLMkpduBasicParameterSet.TOTAL_BPS_BODY_LENGTH;
            mbField = new byte[cakNameLength];
            bb.get(mbField, 0, cakNameLength);
            basicParameterSet.setCKN(mbField);

            int padLength = basicParameterSet.getBodyLength() + EAPOLMkpduParameterSet.BODY_LENGTH_OCTET_OFFSET -
                    (EAPOLMkpduBasicParameterSet.TOTAL_BPS_BODY_LENGTH + cakNameLength);
            if (padLength > 0) {
                mbField = new byte[padLength];
                bb.get(mbField, 0, padLength);
                basicParameterSet.setPadding(mbField);
            }
            return basicParameterSet;
        };
    }

    @Override
    public byte[] serialize() {
        short paddedLength = getTotalLength();

        // Serialize Basic Parameter Set. IEEE 802.1x, Figure 11.8
        ByteBuffer data = ByteBuffer.wrap(new byte[paddedLength]);

        // Octet 1,2
        data.put(mkaVersion);
        data.put(keyServerPriority);

        // Octet 3
        byte octet3 = (byte) ((byte) ((keyServer) ? 0x01 : 0x00) << KEYSERVER_OFFSET);
        octet3 |= (byte) ((byte) ((macSecDesired) ? 0x01 : 0x00) << MACSEC_DESIRED_OFFSET);
        octet3 |= capability << MACSEC_CAPABILITY_OFFSET;

        // Remove header length upto "Length" field from total packet length.
        paddedLength -= BPS_FIXED_PART_SIZE_UPTO_LENGTH_FIELD;
        octet3 |= (byte) (paddedLength >> BODY_LENGTH_MSB_SHIFT & BODY_LENGTH_MSB_MASK);
        data.put(octet3);

        // Octet 4
        data.put((byte) paddedLength);

        // Octet 5-12
        data.put(sci.array());

        // Octet 13-24
        data.put(mi);

        // Octet 25-28
        data.putInt(mn);

        // Octet 29-32
        data.put(algAgility);

        // Octet 33-
        data.put(ckn);

        // TODO: Filling Padding if needed.

        return data.array();
    }


    @Override
    public byte getParameterSetType() {
        return PARAMETERSET_TYPE_BASIC;
    }

    @Override
    public short getTotalLength() {
        /*
         *  Total size calculation.
         *    4 byte aligned padded length calculation.
         *    ie. padded_length = (length + 3) & ~3
         */
        short paddedLength = (short) (((BPS_FIXED_PART_TOTAL_SIZE + ckn.length) + 0x03) & ~0x03);
        return paddedLength;
    }

}
