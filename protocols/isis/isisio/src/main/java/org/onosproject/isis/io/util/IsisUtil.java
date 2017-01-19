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
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.io.isispacket.IsisHeader;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.P2PHelloPdu;
import org.onosproject.isis.io.isispacket.tlv.AdjacencyStateTlv;
import org.onosproject.isis.io.isispacket.tlv.AreaAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.IpInterfaceAddressTlv;
import org.onosproject.isis.io.isispacket.tlv.IsisNeighborTlv;
import org.onosproject.isis.io.isispacket.tlv.PaddingTlv;
import org.onosproject.isis.io.isispacket.tlv.ProtocolSupportedTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;
import org.onosproject.isis.io.isispacket.tlv.TlvType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    public static final int TWO_BYTES = 2;
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
        int count = 1;
        String systemId = "";
        for (Byte byt : bytes) {
            String hexa = Integer.toHexString(Byte.toUnsignedInt(byt));
            if (hexa.length() % 2 != 0) {
                hexa = "0" + hexa;
            }
            if (count == 7 && bytes.length == 8) {
                systemId = systemId + hexa + "-";
            } else {
                systemId = systemId + hexa;
            }
            if (systemId.length() == 4 || systemId.length() == 9
                    || systemId.length() == 14) {
                systemId = systemId + ".";
            }
            count++;
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
     * Returns PDU headaer length.
     *
     * @param pduType PDU type
     * @return headerLength header length
     */
    public static int getPduHeaderLength(int pduType) {
        int headerLength = 0;
        switch (IsisPduType.get(pduType)) {
            case L1HELLOPDU:
            case L2HELLOPDU:
            case L1LSPDU:
            case L2LSPDU:
                headerLength = IsisConstants.HELLOHEADERLENGTH;
                break;
            case P2PHELLOPDU:
                headerLength = IsisConstants.P2PHELLOHEADERLENGTH;
                break;
            case L1PSNP:
            case L2PSNP:
                headerLength = IsisConstants.PSNPDUHEADERLENGTH;
                break;
            case L1CSNP:
            case L2CSNP:
                headerLength = IsisConstants.CSNPDUHEADERLENGTH;
                break;
            default:
                break;
        }
        return headerLength;
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
        //Convert the length to two bytes as the length field is 2 bytes
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
     * Returns the P2P hello PDU.
     *
     * @param isisInterface  ISIS interface instance
     * @param paddingEnabled padding enabled or not
     * @return hello PDU
     */
    public static byte[] getP2pHelloPdu(IsisInterface isisInterface, boolean paddingEnabled) {
        IsisHeader isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator((byte) IsisConstants.IRPDISCRIMINATOR);
        isisHeader.setPduHeaderLength((byte) IsisConstants.P2PHELLOHEADERLENGTH);
        isisHeader.setVersion((byte) IsisConstants.ISISVERSION);
        isisHeader.setIdLength((byte) IsisConstants.SYSTEMIDLENGTH);
        isisHeader.setIsisPduType(IsisPduType.P2PHELLOPDU.value());
        isisHeader.setVersion2((byte) IsisConstants.ISISVERSION);
        isisHeader.setReserved((byte) IsisConstants.PDULENGTHPOSITION);
        isisHeader.setMaximumAreaAddresses((byte) IsisConstants.MAXAREAADDRESS);
        P2PHelloPdu p2pHelloPdu = new P2PHelloPdu(isisHeader);
        p2pHelloPdu.setCircuitType((byte) isisInterface.reservedPacketCircuitType());
        p2pHelloPdu.setSourceId(isisInterface.systemId());
        p2pHelloPdu.setHoldingTime(isisInterface.holdingTime());
        p2pHelloPdu.setPduLength(IsisConstants.PDU_LENGTH);
        p2pHelloPdu.setLocalCircuitId((byte) IsisConstants.LOCALCIRCUITIDFORP2P);

        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        tlvHeader.setTlvLength(0);
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(isisInterface.areaAddress());
        p2pHelloPdu.addTlv(areaAddressTlv);

        tlvHeader.setTlvType(TlvType.PROTOCOLSUPPORTED.value());
        tlvHeader.setTlvLength(0);
        ProtocolSupportedTlv protocolSupportedTlv = new ProtocolSupportedTlv(tlvHeader);
        protocolSupportedTlv.addProtocolSupported((byte) IsisConstants.PROTOCOLSUPPORTED);
        p2pHelloPdu.addTlv(protocolSupportedTlv);

        tlvHeader.setTlvType(TlvType.ADJACENCYSTATE.value());
        tlvHeader.setTlvLength(0);
        AdjacencyStateTlv adjacencyStateTlv = new AdjacencyStateTlv(tlvHeader);
        adjacencyStateTlv.setAdjacencyType((byte) IsisInterfaceState.DOWN.value());
        adjacencyStateTlv.setLocalCircuitId(Integer.parseInt(isisInterface.circuitId()));
        Set<MacAddress> neighbors = isisInterface.neighbors();
        if (!neighbors.isEmpty()) {
            IsisNeighbor neighbor = isisInterface.lookup(neighbors.iterator().next());
            adjacencyStateTlv.setAdjacencyType((byte) neighbor.interfaceState().value());
            adjacencyStateTlv.setNeighborSystemId(neighbor.neighborSystemId());
            adjacencyStateTlv.setNeighborLocalCircuitId(neighbor.localExtendedCircuitId());
        }
        p2pHelloPdu.addTlv(adjacencyStateTlv);

        tlvHeader.setTlvType(TlvType.IPINTERFACEADDRESS.value());
        tlvHeader.setTlvLength(0);
        IpInterfaceAddressTlv ipInterfaceAddressTlv = new IpInterfaceAddressTlv(tlvHeader);
        ipInterfaceAddressTlv.addInterfaceAddres(isisInterface.interfaceIpAddress());
        p2pHelloPdu.addTlv(ipInterfaceAddressTlv);

        byte[] beforePadding = p2pHelloPdu.asBytes();
        byte[] helloMessage;
        if (paddingEnabled) {
            byte[] paddingTlvs = getPaddingTlvs(beforePadding.length);
            helloMessage = Bytes.concat(beforePadding, paddingTlvs);
        } else {
            helloMessage = beforePadding;
        }
        return helloMessage;
    }

    /**
     * Returns the L1 hello PDU.
     *
     * @param isisInterface  ISIS interface instance
     * @param paddingEnabled padding enabled or not
     * @return helloMessage hello PDU
     */
    public static byte[] getL1HelloPdu(IsisInterface isisInterface, boolean paddingEnabled) {
        return getL1OrL2HelloPdu(isisInterface, IsisPduType.L1HELLOPDU, paddingEnabled);
    }

    /**
     * Returns the L2 hello PDU.
     *
     * @param isisInterface  ISIS interface instance
     * @param paddingEnabled padding enabled or not
     * @return helloMessage hello PDU
     */
    public static byte[] getL2HelloPdu(IsisInterface isisInterface, boolean paddingEnabled) {
        return getL1OrL2HelloPdu(isisInterface, IsisPduType.L2HELLOPDU, paddingEnabled);
    }

    /**
     * Returns the hello PDU.
     *
     * @param isisInterface  ISIS interface instance
     * @param paddingEnabled padding enabled or not
     * @return helloMessage hello PDU
     */
    private static byte[] getL1OrL2HelloPdu(IsisInterface isisInterface, IsisPduType isisPduType,
                                            boolean paddingEnabled) {
        String lanId = "";
        IsisHeader isisHeader = new IsisHeader();
        isisHeader.setIrpDiscriminator((byte) IsisConstants.IRPDISCRIMINATOR);
        isisHeader.setPduHeaderLength((byte) IsisConstants.HELLOHEADERLENGTH);
        isisHeader.setVersion((byte) IsisConstants.ISISVERSION);
        isisHeader.setIdLength((byte) IsisConstants.SYSTEMIDLENGTH);
        if (isisPduType == IsisPduType.L1HELLOPDU) {
            isisHeader.setIsisPduType(IsisPduType.L1HELLOPDU.value());
            lanId = isisInterface.l1LanId();
        } else if (isisPduType == IsisPduType.L2HELLOPDU) {
            isisHeader.setIsisPduType(IsisPduType.L2HELLOPDU.value());
            lanId = isisInterface.l2LanId();
        }
        isisHeader.setVersion2((byte) IsisConstants.ISISVERSION);
        isisHeader.setReserved((byte) IsisConstants.PDULENGTHPOSITION);
        isisHeader.setMaximumAreaAddresses((byte) IsisConstants.MAXAREAADDRESS);
        L1L2HelloPdu l1L2HelloPdu = new L1L2HelloPdu(isisHeader);
        l1L2HelloPdu.setCircuitType((byte) isisInterface.reservedPacketCircuitType());
        l1L2HelloPdu.setSourceId(isisInterface.systemId());
        l1L2HelloPdu.setHoldingTime(isisInterface.holdingTime());
        l1L2HelloPdu.setPduLength(IsisConstants.PDU_LENGTH);
        l1L2HelloPdu.setPriority((byte) isisInterface.priority());
        l1L2HelloPdu.setLanId(lanId);
        TlvHeader tlvHeader = new TlvHeader();
        tlvHeader.setTlvType(TlvType.AREAADDRESS.value());
        tlvHeader.setTlvLength(0);
        AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
        areaAddressTlv.addAddress(isisInterface.areaAddress());
        l1L2HelloPdu.addTlv(areaAddressTlv);
        Set<MacAddress> neighbors = isisInterface.neighbors();
        if (!neighbors.isEmpty()) {
            List<MacAddress> neighborMacs = new ArrayList<>();
            for (MacAddress neighbor : neighbors) {
                IsisNeighbor isisNeighbor = isisInterface.lookup(neighbor);
                if (isisPduType == IsisPduType.L1HELLOPDU) {
                    if (isisNeighbor.routerType() == IsisRouterType.L1 ||
                            isisNeighbor.routerType() == IsisRouterType.L1L2) {
                        neighborMacs.add(neighbor);
                    }
                } else if (isisPduType == IsisPduType.L2HELLOPDU) {
                    if (isisNeighbor.routerType() == IsisRouterType.L2 ||
                            isisNeighbor.routerType() == IsisRouterType.L1L2) {
                        neighborMacs.add(neighbor);
                    }
                }
            }

            tlvHeader.setTlvType(TlvType.ISNEIGHBORS.value());
            tlvHeader.setTlvLength(0);

            IsisNeighborTlv isisNeighborTlv = new IsisNeighborTlv(tlvHeader);
            for (MacAddress neighbor : neighborMacs) {
                isisNeighborTlv.addNeighbor(neighbor);
            }
            l1L2HelloPdu.addTlv(isisNeighborTlv);
        }

        tlvHeader.setTlvType(TlvType.PROTOCOLSUPPORTED.value());
        tlvHeader.setTlvLength(0);
        ProtocolSupportedTlv protocolSupportedTlv = new ProtocolSupportedTlv(tlvHeader);
        protocolSupportedTlv.addProtocolSupported((byte) IsisConstants.PROTOCOLSUPPORTED);
        l1L2HelloPdu.addTlv(protocolSupportedTlv);

        tlvHeader.setTlvType(TlvType.IPINTERFACEADDRESS.value());
        tlvHeader.setTlvLength(0);
        IpInterfaceAddressTlv ipInterfaceAddressTlv = new IpInterfaceAddressTlv(tlvHeader);
        ipInterfaceAddressTlv.addInterfaceAddres(isisInterface.interfaceIpAddress());
        l1L2HelloPdu.addTlv(ipInterfaceAddressTlv);

        byte[] beforePadding = l1L2HelloPdu.asBytes();
        byte[] helloMessage;
        if (paddingEnabled) {
            byte[] paddingTlvs = getPaddingTlvs(beforePadding.length);
            helloMessage = Bytes.concat(beforePadding, paddingTlvs);
        } else {
            helloMessage = beforePadding;
        }
        return helloMessage;
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
     * Converts the bytes of prefix to string type value.
     *
     * @param bytes array of prefix
     * @return string value of prefix
     */
    public static String prefixConversion(byte[] bytes) {
        String prefix = "";
        for (int i = 0; i < bytes.length; i++) {
            if (i < (bytes.length - 1)) {
                prefix = prefix + bytes[i] + ".";
            } else {
                prefix = prefix + bytes[i];
            }
        }
        return prefix;
    }

    /**
     * Converts the prefix to bytes.
     *
     * @param prefix prefix
     * @return prefix to bytes
     */
    public static byte[] prefixToBytes(String prefix) {
        List<Byte> byteList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(prefix, ".");
        while (tokenizer.hasMoreTokens()) {
            byteList.add((byte) Integer.parseInt(tokenizer.nextToken()));
        }
        return Bytes.toArray(byteList);
    }

    /**
     * Return the DIS value from the systemId.
     *
     * @param systemId system Id.
     * @return return true if DIS else false
     */
    public static boolean checkIsDis(String systemId) {
        StringTokenizer stringTokenizer = new StringTokenizer(systemId, "." + "-");
        int count = 0;
        while (stringTokenizer.hasMoreTokens()) {
            String str = stringTokenizer.nextToken();
            if (count == 3) {
                int x = Integer.parseInt(str);
                if (x > 0) {
                    return true;
                }
            }
            count++;
        }
        return false;
    }

    /**
     * Return the systemId.
     *
     * @param systemId system Id.
     * @return return system ID
     */
    public static String removeTailingZeros(String systemId) {
        StringTokenizer stringTokenizer = new StringTokenizer(systemId, "-");
        return stringTokenizer.nextToken();
    }
}