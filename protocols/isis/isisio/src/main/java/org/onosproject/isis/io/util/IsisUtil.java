/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.util;

import com.google.common.primitives.Bytes;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.io.isispacket.tlv.PaddingTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Representation of ISIS utils.
 */
public final class IsisUtil {
    public static final int ETHER_HEADER_LEN = 17;
    public static final int ID_SIX_BYTES = 6;
    public static final int ID_PLUS_ONE_BYTE = 7;
    public static final int ID_PLUS_TWO_BYTE = 8;
    public static final int THREE_BYTES = 3;
    public static final int SIX_BYTES = 6;
    public static final int EIGHT_BYTES = 8;
    public static final int FOUR_BYTES = 4;
    public static final int PADDING_FIXED_LENGTH = 255;
    public static final int TLVHEADERLENGTH = 2;
    public static final int INITIAL_BANDWIDTH = 12500000;
    private static final Logger log = LoggerFactory.getLogger(IsisUtil.class);

    /**
     * Creates an instance.
     */
    private IsisUtil() {

    }

    /**
     * Checks given IPs are in same network or not.
     *
     * @param ip1  IP address
     * @param ip2  IP address
     * @param mask network mask
     * @return true if both are in same network else false
     */
    public static boolean sameNetwork(Ip4Address ip1, Ip4Address ip2, byte[] mask) {
        try {
            byte[] a1 = ip1.toOctets();
            byte[] a2 = ip2.toOctets();
            for (int i = 0; i < a1.length; i++) {
                if ((a1[i] & mask[i]) != (a2[i] & mask[i])) {
                    return false;
                }
            }
        } catch (Exception e) {
            log.debug("Exception::IsisUtil::sameNetwork:: {}", e.getMessage());
        }
        return true;
    }

    /**
     * Parse byte array to string system ID.
     *
     * @param bytes system ID
     * @return systemId system ID
     */
    public static String systemId(byte[] bytes) {
        String systemId = "";
        for (Byte byt : bytes) {
            String hexa = Integer.toHexString(Byte.toUnsignedInt(byt));
            if (hexa.length() % 2 != 0) {
                hexa = "0" + hexa;
            }
            systemId = systemId + hexa;
            if (systemId.length() == 4 || systemId.length() == 9) {
                systemId = systemId + ".";
            }
        }
        return systemId;
    }

    /**
     * Parse byte array to LAN ID.
     *
     * @param bytes LAN ID
     * @return systemIdPlus system ID
     */
    public static String systemIdPlus(byte[] bytes) {
        String systemId = "";
        for (Byte byt : bytes) {
            String hexa = Integer.toHexString(Byte.toUnsignedInt(byt));
            if (hexa.length() % 2 != 0) {
                hexa = "0" + hexa;
            }
            systemId = systemId + hexa;
            if (systemId.length() == 4 || systemId.length() == 9
                    || systemId.length() == 14) {
                systemId = systemId + ".";
            }
        }
        return systemId;
    }

    /**
     * Parse byte array to area address.
     *
     * @param bytes area address
     * @return areaAddres area address
     */
    public static String areaAddres(byte[] bytes) {
        String areaAddres = "";
        for (Byte byt : bytes) {
            String hexa = Integer.toHexString(Byte.toUnsignedInt(byt));
            if (hexa.length() % 2 != 0) {
                hexa = "0" + hexa;
            }
            areaAddres = areaAddres + hexa;
        }
        return areaAddres;
    }

    /**
     * Parse area address to bytes.
     *
     * @param address area address
     * @return areaAddress area address
     */
    public static List<Byte> areaAddressToBytes(String address) {
        List<Byte> idList = new ArrayList<>();
        for (int i = 0; i < address.length(); i = i + 2) {
            Character c1 = address.charAt(i);
            Character c2 = address.charAt(i + 1);
            String str = c1.toString() + c2.toString();
            idList.add((byte) Integer.parseInt(str, 16));
        }
        return idList;
    }

    /**
     * Adds the PDU length in packet.
     *
     * @param isisPacket      ISIS packet
     * @param lengthBytePos1  length byte position
     * @param lengthBytePos2  length byte position
     * @param reservedBytePos reserved byte position
     * @return byte array with PDU length
     */
    public static byte[] addLengthAndMarkItInReserved(byte[] isisPacket, int lengthBytePos1,
                                                      int lengthBytePos2, int reservedBytePos) {
        //Set the length of the packet
        //Get the total length of the packet
        int length = isisPacket.length;
        //Convert the lenth to two bytes as the length field is 2 bytes
        byte[] lenthInTwoBytes = IsisUtil.convertToTwoBytes(length);
        //isis header 3rd and 4th position represents length
        isisPacket[lengthBytePos1] = lenthInTwoBytes[0]; //assign 1st byte in lengthBytePos1
        isisPacket[lengthBytePos2] = lenthInTwoBytes[1]; //assign 2st byte in lengthBytePos2
        isisPacket[reservedBytePos] = (byte) lengthBytePos1;
        return isisPacket;
    }

    /**
     * Adds the checksum in packet.
     *
     * @param isisPacket       ISIS packet
     * @param checksumBytePos1 checksum byte position
     * @param checksumBytePos2 checksum byte position
     * @return byte array with PDU length
     */
    public static byte[] addChecksum(byte[] isisPacket, int checksumBytePos1, int checksumBytePos2) {
        //Set the checksum for the packet
        //Convert the lenth to two bytes as the length field is 2 bytes
        byte[] checksumInTwoBytes = new ChecksumCalculator().calculateLspChecksum(
                isisPacket, checksumBytePos1, checksumBytePos2);
        //isis header 3rd and 4th position represents length
        isisPacket[checksumBytePos1] = checksumInTwoBytes[0];
        isisPacket[checksumBytePos2] = checksumInTwoBytes[1];
        return isisPacket;
    }

    /**
     * Adds frame a packet of 1498 of size.
     *
     * @param isisPacket     ISIS packet
     * @param interfaceIndex interface index
     * @return byte array with 1498 is the length
     */
    public static byte[] framePacket(byte[] isisPacket, int interfaceIndex) {
        //Set the length of the packet
        //Get the total length of the packet
        int length = isisPacket.length;
        //PDU_LENGTH + 1 byte for interface index
        if (length < IsisConstants.PDU_LENGTH + 1) {
            byte[] bytes = new byte[IsisConstants.PDU_LENGTH + 1];
            System.arraycopy(isisPacket, 0, bytes, 0, length);
            bytes[IsisConstants.PDU_LENGTH] = (byte) interfaceIndex;
            return bytes;
        }
        return isisPacket;
    }

    /**
     * Parse source and LAN ID.
     *
     * @param id source and LAN ID
     * @return source and LAN ID
     */
    public static List<Byte> sourceAndLanIdToBytes(String id) {
        List<Byte> idList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(id, "." + "-");
        while (tokenizer.hasMoreElements()) {
            int i = 0;
            String str = tokenizer.nextToken();
            idList.add((byte) Integer.parseInt(str.substring(0, i + 2), 16));
            if (str.length() > 2) {
                idList.add((byte) Integer.parseInt(str.substring(i + 2, str.length()), 16));
            }
        }
        return idList;
    }

    /**
     * Parse padding for PDU based on current length.
     *
     * @param currentLength current length
     * @return byteArray padding array
     */
    public static byte[] getPaddingTlvs(int currentLength) {
        List<Byte> bytes = new ArrayList<>();
        while (IsisConstants.PDU_LENGTH > currentLength) {
            int length = IsisConstants.PDU_LENGTH - currentLength;
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(TlvType.PADDING.value());
            if (length >= PADDING_FIXED_LENGTH) {
                tlvHeader.setTlvLength(PADDING_FIXED_LENGTH);
            } else {
                tlvHeader.setTlvLength(IsisConstants.PDU_LENGTH - (currentLength + TLVHEADERLENGTH));
            }
            PaddingTlv tlv = new PaddingTlv(tlvHeader);
            bytes.addAll(Bytes.asList(tlv.asBytes()));
            currentLength = currentLength + tlv.tlvLength() + TLVHEADERLENGTH;
        }
        byte[] byteArray = new byte[bytes.size()];
        int i = 0;
        for (byte byt : bytes) {
            byteArray[i++] = byt;
        }
        return byteArray;
    }

    /**
     * Converts an integer to two bytes.
     *
     * @param numberToConvert number to convert
     * @return numInBytes given number as bytes
     */
    public static byte[] convertToTwoBytes(int numberToConvert) {

        byte[] numInBytes = new byte[2];
        String s1 = Integer.toHexString(numberToConvert);
        if (s1.length() % 2 != 0) {
            s1 = "0" + s1;
        }
        byte[] hexas = DatatypeConverter.parseHexBinary(s1);
        if (hexas.length == 1) {
            numInBytes[0] = 0;
            numInBytes[1] = hexas[0];
        } else {
            numInBytes[0] = hexas[0];
            numInBytes[1] = hexas[1];
        }
        return numInBytes;
    }

    /**
     * Converts a number to four bytes.
     *
     * @param numberToConvert number to convert
     * @return numInBytes given number as bytes
     */
    public static byte[] convertToFourBytes(int numberToConvert) {

        byte[] numInBytes = new byte[4];
        String s1 = Integer.toHexString(numberToConvert);
        if (s1.length() % 2 != 0) {
            s1 = "0" + s1;
        }
        byte[] hexas = DatatypeConverter.parseHexBinary(s1);
        if (hexas.length == 1) {
            numInBytes[0] = 0;
            numInBytes[1] = 0;
            numInBytes[2] = 0;
            numInBytes[3] = hexas[0];
        } else if (hexas.length == 2) {
            numInBytes[0] = 0;
            numInBytes[1] = 0;
            numInBytes[2] = hexas[0];
            numInBytes[3] = hexas[1];
        } else if (hexas.length == 3) {
            numInBytes[0] = 0;
            numInBytes[1] = hexas[0];
            numInBytes[2] = hexas[1];
            numInBytes[3] = hexas[2];
        } else {
            numInBytes[0] = hexas[0];
            numInBytes[1] = hexas[1];
            numInBytes[2] = hexas[2];
            numInBytes[3] = hexas[3];
        }
        return numInBytes;
    }

    /**
     * Converts a byte to integer variable.
     *
     * @param bytesToConvert bytes to convert
     * @return integer representation of bytes
     */
    public static int byteToInteger(byte[] bytesToConvert) {
        final StringBuilder builder = new StringBuilder();
        for (byte eachByte : bytesToConvert) {
            builder.append(String.format("%02x", eachByte));
        }
        int number = Integer.parseInt(builder.toString(), 16);
        return number;
    }

    /**
     * Converts a byte to long variable.
     *
     * @param bytesToConvert bytes to convert
     * @return long representation of bytes
     */
    public static long byteToLong(byte[] bytesToConvert) {
        final StringBuilder builder = new StringBuilder();
        for (byte eachByte : bytesToConvert) {
            builder.append(String.format("%02x", eachByte));
        }
        long number = Long.parseLong(builder.toString(), 16);
        return number;
    }

    /**
     * Converts a number to four bytes.
     *
     * @param numberToConvert number to convert
     * @return numInBytes given number as bytes
     */
    public static byte[] convertToFourBytes(long numberToConvert) {
        byte[] numInBytes = new byte[4];
        String s1 = Long.toHexString(numberToConvert);
        if (s1.length() % 2 != 0) {
            s1 = "0" + s1;
        }
        if (s1.length() == 16) {
            s1 = s1.substring(8, s1.length());
        }
        byte[] hexas = DatatypeConverter.parseHexBinary(s1);
        if (hexas.length == 1) {
            numInBytes[0] = 0;
            numInBytes[1] = 0;
            numInBytes[2] = 0;
            numInBytes[3] = hexas[0];
        } else if (hexas.length == 2) {
            numInBytes[0] = 0;
            numInBytes[1] = 0;
            numInBytes[2] = hexas[0];
            numInBytes[3] = hexas[1];
        } else if (hexas.length == 3) {
            numInBytes[0] = 0;
            numInBytes[1] = hexas[0];
            numInBytes[2] = hexas[1];
            numInBytes[3] = hexas[2];
        } else {
            numInBytes[0] = hexas[0];
            numInBytes[1] = hexas[1];
            numInBytes[2] = hexas[2];
            numInBytes[3] = hexas[3];
        }
        return numInBytes;
    }

    /**
     * Converts a number to eight bit binary.
     *
     * @param binaryString string to binary
     * @return numInBytes given number as bytes
     */
    public static String toEightBitBinary(String binaryString) {
        String eightBit = binaryString;
        if (eightBit.length() % 8 != 0) {
            int numOfZero = 8 - eightBit.length();
            while (numOfZero > 0) {
                eightBit = "0" + eightBit;
                numOfZero--;
            }
        }
        return eightBit;
    }

    /**
     * Converts a number to four bit binary.
     *
     * @param binaryString string to binary
     * @return numInBytes given number as bytes
     */
    public static String toFourBitBinary(String binaryString) {
        String fourBit = binaryString;
        if (fourBit.length() % 4 != 0) {
            int numOfZero = 4 - fourBit.length();
            while (numOfZero > 0) {
                fourBit = "0" + fourBit;
                numOfZero--;
            }
        }
        return fourBit;
    }

    /**
     * Converts a number to three bytes.
     *
     * @param numberToConvert number to convert
     * @return given number as bytes
     */
    public static byte[] convertToThreeBytes(int numberToConvert) {
        byte[] numInBytes = new byte[4];
        String s1 = Integer.toHexString(numberToConvert);
        if (s1.length() % 2 != 0) {
            s1 = "0" + s1;
        }
        byte[] hexas = DatatypeConverter.parseHexBinary(s1);
        if (hexas.length == 1) {
            numInBytes[0] = 0;
            numInBytes[1] = 0;
            numInBytes[2] = hexas[0];
        } else if (hexas.length == 2) {
            numInBytes[0] = 0;
            numInBytes[1] = hexas[0];
            numInBytes[2] = hexas[1];
        } else {
            numInBytes[0] = hexas[0];
            numInBytes[1] = hexas[1];
            numInBytes[2] = hexas[2];
        }
        return numInBytes;
    }
}
