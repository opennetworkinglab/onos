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
package org.onosproject.ospf.protocol.util;

import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.lsa.OpaqueLsaHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Representation of an OSPF constants and utility methods.
 */
public final class OspfUtil {
    public static final int OSPF_VERSION_2 = 2;
    public static final int OSPF_VERSION = OSPF_VERSION_2;
    public static final int PACKET_MINIMUM_LENGTH = 24;
    public static final int METADATA_LEN = 5;
    public static final int MINIMUM_FRAME_LEN = 1487;
    public static final int OSPF_HEADER_LENGTH = 24;
    public static final int LSA_HEADER_LENGTH = 20;
    public static final int DD_HEADER_LENGTH = OSPF_HEADER_LENGTH + 8;
    public static final int LSREQUEST_LENGTH = 12;
    public static final int OSPFPACKET_LENGTH_POS1 = 2;
    public static final int OSPFPACKET_LENGTH_POS2 = 3;
    public static final int OSPFPACKET_CHECKSUM_POS1 = 12;
    public static final int OSPFPACKET_CHECKSUM_POS2 = 13;
    public static final int LSAPACKET_CHECKSUM_POS1 = 16;
    public static final int LSAPACKET_CHECKSUM_POS2 = 17;
    public static final Ip4Address ALL_SPF_ROUTERS = Ip4Address.valueOf("224.0.0.5");
    public static final Ip4Address ALL_DROUTERS = Ip4Address.valueOf("224.0.0.6");
    public static final Ip4Address DEFAULTIP = Ip4Address.valueOf("0.0.0.0");
    public static final int RETRANSMITINTERVAL = 5;
    public static final int ONLY_ALL_SPF_ROUTERS = 1;
    public static final int JOIN_ALL_DROUTERS = 2;
    public static final int INITIALIZE_SET = 1;
    public static final int INITIALIZE_NOTSET = 0;
    public static final int MORE_SET = 1;
    public static final int MORE_NOTSET = 0;
    public static final int IS_MASTER = 1;
    public static final int NOT_MASTER = 0;
    public static final int NOT_ASSIGNED = 0;
    public static final int FOUR_BYTES = 4;
    public static final int FIVE_BYTES = 5;
    public static final int EIGHT_BYTES = 8;
    public static final int TWELVE_BYTES = 12;
    public static final int EXTERNAL_DESTINATION_LENGTH = 12;
    public static final String SHOST = "127.0.0.1";
    public static final int SPORT = 7000;
    public static final int MTU = 1500;
    public static final char CONFIG_LENGTH = 1498;
    public static final char ROUTER_PRIORITY = 0;
    public static final int HELLO_PACKET_OPTIONS = 2;
    private static final Logger log =
            LoggerFactory.getLogger(OspfUtil.class);

    /**
     * Creates an instance.
     */
    private OspfUtil() {

    }

    /**
     * Checks given IPs are in same network or not.
     *
     * @param ip1  IP address
     * @param ip2  IP address
     * @param mask network mask
     * @return true if both are in same network else false
     * @throws Exception might throws exception while parsing ip address
     */
    public static boolean sameNetwork(Ip4Address ip1, Ip4Address ip2, Ip4Address mask)
            throws Exception {

        byte[] a1 = ip1.toOctets();
        byte[] a2 = ip2.toOctets();
        byte[] m = mask.toOctets();

        for (int i = 0; i < a1.length; i++) {
            if ((a1[i] & m[i]) != (a2[i] & m[i])) {
                return false;
            }
        }

        return true;
    }

    /**
     * Converts IP address to long.
     *
     * @param ipAddress IP address
     * @return long value represents IP address
     */
    public static long ipAddressToLong(String ipAddress) {
        StringTokenizer st = new StringTokenizer(ipAddress, ".");
        long ipAsNumber = Long.parseLong(st.nextToken()) * (long) Math.pow(256, 3);
        ipAsNumber += Long.parseLong(st.nextToken()) * (long) Math.pow(256, 2);
        ipAsNumber += Long.parseLong(st.nextToken()) * 256;
        ipAsNumber += +Long.parseLong(st.nextToken());

        return ipAsNumber;
    }

    /**
     * Checks option field to see whether opaque enabled or not.
     * 2nd Bit in options field of DdPacket represents Opaque.
     * 7th bit is external capability.
     * This method checks Opaque bit is set in the options or not.
     *
     * @param options options value
     * @return true if opaque enabled else false.
     */
    public static boolean isOpaqueEnabled(int options) {
        Boolean[] bits = new Boolean[8];
        for (int i = 7; i >= 0; i--) {
            bits[i] = (options & (1 << i)) != 0;
        }

        List<Boolean> list = Arrays.asList(bits);
        Collections.reverse(list);

        //2nd bit is Opaque.
        return list.get(1);
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
        int num = Integer.parseInt(builder.toString(), 16);
        return num;
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
        long num = Long.parseLong(builder.toString(), 16);
        return num;
    }

    /**
     * Creates a random number.
     *
     * @return random number
     */
    public static int createRandomNumber() {
        Random rnd = new Random();
        int randomNumber = 10000000 + rnd.nextInt(90000000);
        return randomNumber;
    }

    /**
     * Reads the LSA header from channel buffer.
     *
     * @param channelBuffer channel buffer instance
     * @return LSA header instance.
     * @throws Exception might throws exception while parsing buffer
     */
    public static LsaHeader readLsaHeader(ChannelBuffer channelBuffer) throws Exception {
        //add all the LSA Headers - one header is of 20 bytes
        LsaHeader lsaHeader = null;
        if (channelBuffer.readableBytes() >= OspfUtil.LSA_HEADER_LENGTH) {
            byte[] byteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(byteArray, 0, OspfUtil.FOUR_BYTES);
            ChannelBuffer tempBuffer = ChannelBuffers.copiedBuffer(byteArray);
            int lsType = byteArray[3];
            if (lsType == OspfParameters.AREA_LOCAL_OPAQUE_LSA || lsType == OspfParameters.LINK_LOCAL_OPAQUE_LSA
                    || lsType == OspfParameters.AS_OPAQUE_LSA) {
                OpaqueLsaHeader header = new OpaqueLsaHeader();
                header.setAge(tempBuffer.readShort());
                header.setOptions(tempBuffer.readByte());
                header.setLsType(tempBuffer.readByte());
                header.setOpaqueType(channelBuffer.readByte());
                header.setOpaqueId(channelBuffer.readUnsignedMedium());
                byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                header.setAdvertisingRouter(Ip4Address.valueOf(tempByteArray));
                header.setLsSequenceNo(channelBuffer.readInt());
                header.setLsCheckSum(channelBuffer.readUnsignedShort());
                header.setLsPacketLen(channelBuffer.readShort());
                byte[] opaqueIdBytes = OspfUtil.convertToTwoBytes(header.opaqueId());
                header.setLinkStateId(header.opaqueType() + "." + "0" + "." +
                                              opaqueIdBytes[0] + "." + opaqueIdBytes[1]);
                lsaHeader = header;
            } else {
                LsaHeader header = new LsaHeader();
                header.setAge(tempBuffer.readShort());
                header.setOptions(tempBuffer.readByte());
                header.setLsType(tempBuffer.readByte());
                byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                header.setLinkStateId(InetAddress.getByAddress(tempByteArray).getHostName());
                tempByteArray = new byte[OspfUtil.FOUR_BYTES];
                channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
                header.setAdvertisingRouter(Ip4Address.valueOf(tempByteArray));
                header.setLsSequenceNo(channelBuffer.readInt());
                header.setLsCheckSum(channelBuffer.readUnsignedShort());
                header.setLsPacketLen(channelBuffer.readShort());
                lsaHeader = header;
            }
        }
        return lsaHeader;
    }


    /**
     * Converts an integer to two bytes.
     *
     * @param numberToConvert number to convert
     * @return given number as bytes
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
     * Converts a number to three bytes.
     *
     * @param numberToConvert number to convert
     * @return given number as bytes
     */
    public static byte[] convertToThreeBytes(int numberToConvert) {
        byte[] numInBytes = new byte[3];
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

    /**
     * Converts a number to four bytes.
     *
     * @param numberToConvert number to convert
     * @return given number as bytes
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
     * Converts a number to four bytes.
     *
     * @param numberToConvert number to convert
     * @return given number as bytes
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
     * Adds the checksum and length in packet.
     *
     * @param ospfPacket       ospf packet
     * @param lengthBytePos1   length byte position
     * @param lengthBytePos2   length byte position
     * @param checksumBytePos1 checksum byte position
     * @param checksumBytePos2 checksum byte position
     * @return byte array with checksum and length
     */
    public static byte[] addLengthAndCheckSum(byte[] ospfPacket, int lengthBytePos1, int lengthBytePos2,
                                              int checksumBytePos1, int checksumBytePos2) {
        //Set the length of the packet
        //Get the total length of the packet
        int length = ospfPacket.length;
        //Convert the lenth to two bytes as the length field is 2 bytes
        byte[] lenthInTwoBytes = OspfUtil.convertToTwoBytes(length);
        //ospf header 3rd and 4th position represents length
        ospfPacket[lengthBytePos1] = lenthInTwoBytes[0]; //assign 1st byte in lengthBytePos1
        ospfPacket[lengthBytePos2] = lenthInTwoBytes[1]; //assign 2st byte in lengthBytePos2

        //Get the checksum as two bytes.
        byte[] checkSumInTwoBytes = new ChecksumCalculator().calculateOspfCheckSum(ospfPacket,
                                                                                   checksumBytePos1, checksumBytePos2);
        ospfPacket[checksumBytePos1] = checkSumInTwoBytes[0]; //assign 1st byte in checksumBytePos1
        ospfPacket[checksumBytePos2] = checkSumInTwoBytes[1]; //assign 2st byte in checksumBytePos2

        return ospfPacket;
    }

    /**
     * Adds metadata to ospf packet like whether to join multi cast group and destination IP.
     *
     * @param interfaceIndex   interface index
     * @param ospfPacket       OSPF packet
     * @param allDroutersValue whether to join multi cast or not
     * @param destinationIp    destination ip address
     * @return byte array
     */
    public static byte[] addMetadata(int interfaceIndex, byte[] ospfPacket, int allDroutersValue,
                                     Ip4Address destinationIp) {
        byte[] packet;
        byte[] interfaceIndexByteVal = {(byte) interfaceIndex};
        byte[] allDroutersByteVal = {(byte) allDroutersValue};
        byte[] destIpAsBytes = destinationIp.toOctets();
        byte[] metadata = Bytes.concat(interfaceIndexByteVal, allDroutersByteVal);
        metadata = Bytes.concat(metadata, destIpAsBytes);
        packet = Bytes.concat(ospfPacket, metadata);

        return packet;
    }
}