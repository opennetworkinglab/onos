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


import com.google.common.base.MoreObjects;
import com.google.common.primitives.Bytes;
import org.onosproject.isis.io.isispacket.tlv.PaddingTlv;
import org.onosproject.isis.io.isispacket.tlv.TlvHeader;

import javax.xml.bind.DatatypeConverter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Represents ISIS utils.
 */
public final class IsisUtil {
    public static final int AREAADDRESS = 1;
    public static final int ISREACHABILITY = 2;
    public static final int ISNEIGHBORS = 6;
    public static final int PADDING = 8;
    public static final int LSPENTRY = 9;
    public static final int AUTHENTICATION = 10;
    public static final int CHECKSUM = 12;
    public static final int EXTENDEDISREACHABILITY = 22;
    public static final int ISALIAS = 24;
    public static final int IPINTERNALREACHABILITY = 128;
    public static final int PROTOCOLSUPPORTED = 129;
    public static final int IPEXTERNALREACHABILITY = 130;
    public static final int IDRPINFORMATION = 131;
    public static final int IPINTERFACEADDRESS = 132;
    public static final int L1HELLOPDU = 1;
    public static final int L2HELLOPDU = 2;
    public static final int P2PHELLOPDU = 3;
    public static final int L1LSPDU = 18;
    public static final int L2LSPDU = 20;
    public static final int L1CSNP = 24;
    public static final int L2CSNP = 25;
    public static final int L1PSNP = 26;
    public static final int L2PSNP = 27;
    public static final int L1L2_LS_PDUHEADERLENGTH = 27;
    public static final int P2PPDUHEADERLENGTH = 20;
    public static final int PSNPPDUHEADERLENGTH = 17;
    public static final char ETHER_FRAME_LEN = 1514;
    public static final int ID_SIX_BYTES = 6;
    public static final int ID_PLUS_ONE_BYTE = 7;
    public static final int ID_PLUS_TWO_BYTE = 8;
    public static final int THREE_BYTES = 3;
    public static final int SIX_BYTES = 6;
    public static final int FOUR_BYTES = 4;
    public static final int PADDING_FIXED_LENGTH = 255;

    /**
     * Creates an instance of this class.
     */
    private IsisUtil() {

    }

    /**
     * Parse byte array to string system ID.
     *
     * @param bytes system ID
     * @return systemId system ID.
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
     * @return systemId system ID.
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
     * @return areaAddress area address
     */
    public static String areaAddres(byte[] bytes) {
        int count = 0;
        String areaAddress = "";
        for (Byte byt : bytes) {
            String hexa = Integer.toHexString(Byte.toUnsignedInt(byt));
            if (hexa.length() % 2 != 0) {
                hexa = "0" + hexa;
            }
            if (count == 0) {
                hexa = hexa + ".";
            }
            areaAddress = areaAddress + hexa;
            count++;
        }
        return areaAddress;
    }

    /**
     * Parse area address to bytes.
     *
     * @param address area address
     * @return areaAddress area address
     */
    public static List<Byte> areaAddresToBytes(String address) {
        List<Byte> idLst = new ArrayList();
        StringTokenizer tokenizer = new StringTokenizer(address, ".");
        int count = 0;
        while (tokenizer.hasMoreElements()) {
            String str = tokenizer.nextToken();
            if (str.length() % 2 != 0) {
                str = "0" + str;
            }
            if (count > 0) {

                for (int i = 0; i < str.length(); i = i + 2) {
                    idLst.add((byte) Integer.parseInt(str.substring(i, i + 2), 16));
                }
            } else {
                idLst.add((byte) Integer.parseInt(str, 16));
            }
            count++;
        }
        return idLst;
    }

    /**
     * Gets PDU header length.
     *
     * @param pduType PDU type
     * @return headerLength header length
     */
    public static int getPduHeaderLength(int pduType) {
        int headerLength = 0;
        switch (pduType) {
            case L1HELLOPDU:
            case L2HELLOPDU:
            case L1LSPDU:
            case L2LSPDU:
                headerLength = L1L2_LS_PDUHEADERLENGTH;
                break;
            case P2PHELLOPDU:
                headerLength = P2PPDUHEADERLENGTH;
                break;
            case L1PSNP:
            case L2PSNP:
                headerLength = PSNPPDUHEADERLENGTH;
                break;
            default:
                break;
        }
        return headerLength;
    }

    /**
     * Parse source and LAN ID.
     *
     * @param id source and LAN ID
     * @return sourceAndLanIdToBytes source and LAN ID
     */
    public static List<Byte> sourceAndLanIdToBytes(String id) {
        List<Byte> idLst = new ArrayList();

        StringTokenizer tokenizer = new StringTokenizer(id, ".");
        while (tokenizer.hasMoreElements()) {
            int i = 0;
            String str = tokenizer.nextToken();
            idLst.add((byte) Integer.parseInt(str.substring(0, i + 2), 16));
            if (str.length() > 2) {
                idLst.add((byte) Integer.parseInt(str.substring(i + 2, str.length()), 16));
            }

        }
        return idLst;
    }

    /**
     * Parse padding for PDU based on current length.
     *
     * @param currentLength current length
     * @return byteArray padding array
     */
    public static byte[] paddingForPdu(int currentLength) {
        List<Byte> bytes = new ArrayList<>();
        while (ETHER_FRAME_LEN > currentLength) {
            int length = ETHER_FRAME_LEN - currentLength;
            TlvHeader tlvHeader = new TlvHeader();
            tlvHeader.setTlvType(PADDING);
            if (length >= PADDING_FIXED_LENGTH) {
                tlvHeader.setTlvLength(PADDING_FIXED_LENGTH);
            } else {
                tlvHeader.setTlvLength(ETHER_FRAME_LEN - currentLength);
            }
            PaddingTlv tlv = new PaddingTlv(tlvHeader);
            bytes.addAll(Bytes.asList(tlv.asBytes()));
            currentLength = currentLength + tlv.tlvLength();
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .toString();
    }
}