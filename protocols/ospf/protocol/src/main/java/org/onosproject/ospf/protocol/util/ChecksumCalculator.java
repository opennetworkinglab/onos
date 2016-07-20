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

import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.types.AsbrSummaryLsa;
import org.onosproject.ospf.protocol.lsa.types.ExternalLsa;
import org.onosproject.ospf.protocol.lsa.types.NetworkLsa;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa10;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa11;
import org.onosproject.ospf.protocol.lsa.types.OpaqueLsa9;
import org.onosproject.ospf.protocol.lsa.types.RouterLsa;
import org.onosproject.ospf.protocol.lsa.types.SummaryLsa;
import org.onosproject.ospf.controller.OspfMessage;
import org.onosproject.ospf.protocol.ospfpacket.types.DdPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.HelloPacket;
import org.onosproject.ospf.protocol.ospfpacket.types.LsAcknowledge;
import org.onosproject.ospf.protocol.ospfpacket.types.LsRequest;
import org.onosproject.ospf.protocol.ospfpacket.types.LsUpdate;

import java.util.Arrays;

/**
 * Calculates checksum for different types of OSPF packets.
 */
public class ChecksumCalculator {

    /**
     * Converts given string to sixteen bits integer.
     * If hexasum is more than 16 bit value, needs to be reduced to 16 bit value.
     *
     * @param strToConvert hexasum value to convert
     * @return 16 bit integer value
     */
    public static int convertToSixteenBits(String strToConvert) {
        StringBuilder sb = new StringBuilder(strToConvert);
        sb = sb.reverse();
        StringBuilder s1 = new StringBuilder(sb.substring(0, 4));
        s1 = s1.reverse();
        StringBuilder s2 = new StringBuilder(sb.substring(4, sb.length()));
        s2 = s2.reverse();
        int num = Integer.parseInt(s1.toString(), 16) + Integer.parseInt(s2.toString(), 16);
        return num;
    }

    /**
     * Checks whether checksum is valid or not in the given OSPF message.
     *
     * @param ospfMessage  ospf message instance
     * @param checksumPos1 position of checksum bit in packet
     * @param checksumPos2 position of checksum bit in packet
     * @return true if valid else false
     */
    public boolean isValidOspfCheckSum(OspfMessage ospfMessage, int checksumPos1, int checksumPos2) {

        switch (ospfMessage.ospfMessageType().value()) {
            case OspfParameters.HELLO:
                ospfMessage = (HelloPacket) ospfMessage;
                break;
            case OspfParameters.DD:
                ospfMessage = (DdPacket) ospfMessage;
                break;
            case OspfParameters.LSREQUEST:
                ospfMessage = (LsRequest) ospfMessage;
                break;
            case OspfParameters.LSUPDATE:
                ospfMessage = (LsUpdate) ospfMessage;
                break;
            case OspfParameters.LSACK:
                ospfMessage = (LsAcknowledge) ospfMessage;
                break;
            default:
                break;
        }

        byte[] messageAsBytes = ospfMessage.asBytes();
        return validateOspfCheckSum(messageAsBytes, checksumPos1, checksumPos2);
    }

    /**
     * Checks whether checksum is valid or not in the given OSPF LSA.
     *
     * @param ospfLsa         lsa instance
     * @param lsType          lsa type
     * @param lsaChecksumPos1 lsa checksum position in packet
     * @param lsaChecksumPos2 lsa checksum position in packet
     * @return true if valid else false
     * @throws Exception might throw exception while processing
     */
    public boolean isValidLsaCheckSum(OspfLsa ospfLsa, int lsType, int lsaChecksumPos1,
                                      int lsaChecksumPos2) throws Exception {
        if (lsType == OspfLsaType.ROUTER.value()) {
            RouterLsa lsa = (RouterLsa) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.NETWORK.value()) {
            NetworkLsa lsa = (NetworkLsa) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.SUMMARY.value()) {
            SummaryLsa lsa = (SummaryLsa) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.ASBR_SUMMARY.value()) {
            AsbrSummaryLsa lsa = (AsbrSummaryLsa) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.EXTERNAL_LSA.value()) {
            ExternalLsa lsa = (ExternalLsa) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value()) {
            OpaqueLsa9 lsa = (OpaqueLsa9) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value()) {
            OpaqueLsa10 lsa = (OpaqueLsa10) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        } else if (lsType == OspfLsaType.AS_OPAQUE_LSA.value()) {
            OpaqueLsa11 lsa = (OpaqueLsa11) ospfLsa;
            return validateLsaCheckSum(lsa.asBytes(), lsaChecksumPos1, lsaChecksumPos2);
        }

        return false;
    }

    /**
     * Verifies the checksum is valid in given LSA packet bytes.
     *
     * @param lsaPacket       lsa as byte array
     * @param lsaChecksumPos1 position of checksum bit in packet
     * @param lsaChecksumPos2 position of checksum bit in packet
     * @return true if valid else false
     */
    public boolean validateLsaCheckSum(byte[] lsaPacket, int lsaChecksumPos1, int lsaChecksumPos2) {

        byte[] checksum = calculateLsaChecksum(lsaPacket, lsaChecksumPos1, lsaChecksumPos2);

        if (lsaPacket[lsaChecksumPos1] == checksum[0] && lsaPacket[lsaChecksumPos2] == checksum[1]) {
            return true;
        }

        return false;
    }

    /**
     * Verifies the checksum is valid in given OSPF packet bytes.
     *
     * @param ospfPacket   as byte array
     * @param checksumPos1 position of checksum bit in packet
     * @param checksumPos2 position of checksum bit in packet
     * @return true if valid else false
     */
    public boolean validateOspfCheckSum(byte[] ospfPacket, int checksumPos1, int checksumPos2) {

        byte[] checkSum = calculateOspfCheckSum(ospfPacket, checksumPos1, checksumPos2);

        if (ospfPacket[checksumPos1] == checkSum[0] && ospfPacket[checksumPos2] == checkSum[1]) {
            return true;
        }

        return false;
    }

    /**
     * Calculates the LSA checksum.
     *
     * @param lsaBytes        as byte array
     * @param lsaChecksumPos1 position of checksum bit in packet
     * @param lsaChecksumPos2 position of checksum bit in packet
     * @return checksum bytes
     */
    public byte[] calculateLsaChecksum(byte[] lsaBytes, int lsaChecksumPos1, int lsaChecksumPos2) {

        byte[] tempLsaByte = Arrays.copyOf(lsaBytes, lsaBytes.length);

        int[] checksumOut = {0, 0};
        tempLsaByte[lsaChecksumPos1] = 0;
        tempLsaByte[lsaChecksumPos2] = 0;
        byte[] byteCheckSum = {0, 0};
        if (lsaBytes != null) {
            for (int i = 2; i < tempLsaByte.length; i++) {
                checksumOut[0] = checksumOut[0] + ((int) tempLsaByte[i] & 0xFF);
                checksumOut[1] = checksumOut[1] + checksumOut[0];
            }
            checksumOut[0] = checksumOut[0] % 255;
            checksumOut[1] = checksumOut[1] % 255;
        }
        int byte1 = (int) ((tempLsaByte.length - lsaChecksumPos1 - 1) * checksumOut[0] - checksumOut[1]) % 255;
        if (byte1 <= 0) {
            byte1 += 255;
        }
        int byte2 = 510 - checksumOut[0] - byte1;
        if (byte2 > 255) {
            byte2 -= 255;
        }

        byteCheckSum[0] = (byte) byte1;
        byteCheckSum[1] = (byte) byte2;

        return byteCheckSum;
    }

    /**
     * Calculate checksum from hexasum.
     *
     * @param hexasum total of 16 bits hexadecimal values
     * @return checksum value
     */
    private int calculateChecksum(int hexasum) {

        char[] tempZeros = {'0', '0', '0', '0'};
        StringBuffer hexaAsBinaryStr = new StringBuffer(Integer.toBinaryString(hexasum));
        int length = hexaAsBinaryStr.length();
        while (length > 16) {
            if (hexaAsBinaryStr.length() % 4 != 0) {
                int offset = hexaAsBinaryStr.length() % 4;
                hexaAsBinaryStr.insert(0, tempZeros, 0, 4 - offset);
            }
            StringBuffer hexaStr1 = new StringBuffer(hexaAsBinaryStr.reverse().substring(0, 16));
            String revHexaStr1 = hexaStr1.reverse().toString();
            StringBuffer hexaStr2 = new StringBuffer(hexaAsBinaryStr.reverse());
            StringBuffer hexaStr3 = new StringBuffer(hexaStr2.reverse().substring(16, hexaStr2.length()));
            String revHexaStr3 = hexaStr3.reverse().toString();
            int lastSixteenHexaBits = Integer.parseInt(revHexaStr1, 2);
            int remainingHexaBits = Integer.parseInt(revHexaStr3, 2);
            int totalCheckSum = lastSixteenHexaBits + remainingHexaBits;
            hexaAsBinaryStr = new StringBuffer(Integer.toBinaryString(totalCheckSum));
            length = hexaAsBinaryStr.length();
        }
        if (hexaAsBinaryStr.length() < 16) {
            int count = 16 - hexaAsBinaryStr.length();
            String s = hexaAsBinaryStr.toString();
            for (int i = 0; i < count; i++) {
                s = "0" + s;
            }

            hexaAsBinaryStr = new StringBuffer(s);

        }
        StringBuffer checksum = negate(hexaAsBinaryStr);
        return Integer.parseInt(checksum.toString(), 2);
    }

    /**
     * Negates given hexasum.
     *
     * @param binaryString binary form of hexasum
     * @return binary from of calculateChecksum
     */
    private StringBuffer negate(StringBuffer binaryString) {
        for (int i = 0; i < binaryString.length(); i++) {
            if (binaryString.charAt(i) == '1') {
                binaryString.replace(i, i + 1, "0");
            } else {
                binaryString.replace(i, i + 1, "1");
            }
        }

        return binaryString;
    }

    /**
     * Calculates the OSPF checksum for the given packet.
     *
     * @param packet       as byte array
     * @param checksumPos1 position of checksum bit in packet
     * @param checksumPos2 position of checksum bit in packet
     * @return checksum bytes
     */
    public byte[] calculateOspfCheckSum(byte[] packet, int checksumPos1, int checksumPos2) {

        int hexasum = 0;
        for (int i = 0; i < packet.length; i = i + 2) {
            if (i != 12) {
                byte b1 = packet[i];
                String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
                b1 = packet[i + 1];
                String s2 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
                String hexa = s1 + s2;
                int num1 = Integer.parseInt(hexa, 2);
                hexasum = hexasum + num1;
                String convertTo16 = Integer.toHexString(hexasum);
                if (convertTo16.length() > 4) {
                    hexasum = convertToSixteenBits(convertTo16);
                }
            }
        }
        StringBuilder sb = new StringBuilder(Integer.toHexString(hexasum));
        if (sb.length() > 4) {
            sb = sb.reverse();
            StringBuilder s1 = new StringBuilder(sb.substring(0, 4));
            s1 = s1.reverse();
            StringBuilder s2 = new StringBuilder(sb.substring(4, sb.length()));
            s2 = s2.reverse();
            hexasum = Integer.parseInt(s1.toString(), 16) + Integer.parseInt(s2.toString(), 16);
        }
        int finalChecksum = calculateChecksum(hexasum);
        return OspfUtil.convertToTwoBytes(finalChecksum);
    }
}