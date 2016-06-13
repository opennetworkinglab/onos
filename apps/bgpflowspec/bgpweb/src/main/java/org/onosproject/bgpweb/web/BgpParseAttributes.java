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
package org.onosproject.bgpweb.web;

import org.onosproject.flowapi.ExtOperatorValue;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

public class BgpParseAttributes {

    private final Logger log = getLogger(getClass());

    /** Bits as per flow spec rfc 5575.*/
    final byte endBit = (byte) 0x80;
    final byte andBit = 0x40;
    final byte twoByteLen = (byte) (0x01 << 4);
    final byte fourByteLen = (byte) (0x02 << 4);
    final byte lessThan = 0x04;
    final byte greaterThan = 0x02;
    final byte equal = 0x01;

    /** Protocol types.*/
    protected static final byte ICMP = 1;
    protected static final byte IGMP = 2;
    protected static final byte TCP = 6;
    protected static final byte UDP = 17;
    protected static final byte IPV4 = 4;
    protected static final byte IPV6 = 41;

    /** TCP Flags.*/
    static final byte FIN = (1 << 0);
    static final byte SYN = (1 << 1);
    static final byte RST = (1 << 2);
    static final byte PSH = (1 << 3);
    static final byte ACK = (1 << 4);
    static final byte URG = (1 << 5);

    final byte notBit = 0x02;
    final byte matchBit = 0x01;

    /** Fragment Flags.*/
    static final byte DF = (1 << 0);
    static final byte IF = (1 << 1);
    static final byte FF = (1 << 2);
    static final byte LF = (1 << 3);

    List<ExtOperatorValue> parsePort(String string) {
        List<ExtOperatorValue> operatorValue = null;

        operatorValue = createOperatorValue(string);

        return operatorValue;
    }

    List<ExtOperatorValue> parseIpProtocol(String string) {
        List<ExtOperatorValue> operatorValue  = null;

        string = string.replaceAll("ICMP", Byte.valueOf(ICMP).toString());
        string = string.replaceAll("icmp", Byte.valueOf(ICMP).toString());
        string = string.replaceAll("IPv4", Byte.valueOf(IPV4).toString());
        string = string.replaceAll("ipv4", Byte.valueOf(IPV4).toString());
        string = string.replaceAll("TCP", Byte.valueOf(TCP).toString());
        string = string.replaceAll("tcp", Byte.valueOf(TCP).toString());
        string = string.replaceAll("UDP", Byte.valueOf(UDP).toString());
        string = string.replaceAll("udp", Byte.valueOf(UDP).toString());
        string = string.replaceAll("IPv6", Byte.valueOf(IPV6).toString());
        string = string.replaceAll("ipv6", Byte.valueOf(IPV6).toString());
        string = string.replaceAll("igmp", Byte.valueOf(IGMP).toString());
        string = string.replaceAll("IGMP", Byte.valueOf(IGMP).toString());

        operatorValue = createOperatorValue(string);

        return operatorValue;
    }

    List<ExtOperatorValue> parseIcmpType(String string) {
        List<ExtOperatorValue> operatorValue  = null;

        operatorValue = createOperatorValue(string);

        return operatorValue;
    }

    List<ExtOperatorValue> parseIcmpCode(String string) {
        List<ExtOperatorValue> operatorValue  = null;

        operatorValue = createOperatorValue(string);

        return operatorValue;
    }


    List<ExtOperatorValue> parseTcpFlags(String string) {
        List<ExtOperatorValue> operatorValue  = new ArrayList<>();
        Token token;
        int index = 0;

        string = string.replaceAll("FIN", Byte.valueOf(FIN).toString());
        string = string.replaceAll("fin", Byte.valueOf(FIN).toString());
        string = string.replaceAll("SYN", Byte.valueOf(SYN).toString());
        string = string.replaceAll("syn", Byte.valueOf(SYN).toString());
        string = string.replaceAll("RST", Byte.valueOf(RST).toString());
        string = string.replaceAll("rst", Byte.valueOf(RST).toString());
        string = string.replaceAll("PSH", Byte.valueOf(PSH).toString());
        string = string.replaceAll("psh", Byte.valueOf(PSH).toString());
        string = string.replaceAll("ACK", Byte.valueOf(ACK).toString());
        string = string.replaceAll("ack", Byte.valueOf(ACK).toString());
        string = string.replaceAll("URG", Byte.valueOf(URG).toString());
        string = string.replaceAll("urg", Byte.valueOf(URG).toString());

        do {
            token = parseTcpTokenValue(string, index);
            if (token.error) {
                log.error("Error in parsing the TCP value list");
                return null;
            }
            operatorValue.add(new ExtOperatorValue(token.operator, token.value));
            index = token.index;
        } while ((token.operator & endBit) != endBit);

        return operatorValue;
    }

    List<ExtOperatorValue> parsePacketLength(String string) {
        List<ExtOperatorValue> operatorValue  = null;
        operatorValue = createOperatorValue(string);

        return operatorValue;
    }

    List<ExtOperatorValue> parseDscp(String string) {
        List<ExtOperatorValue> operatorValue  = null;

        operatorValue = createOperatorValue(string);

        return operatorValue;
    }

    List<ExtOperatorValue> parseFragment(String string) {

        List<ExtOperatorValue> operatorValue  = null;

        string = string.replaceAll("DF", Byte.valueOf(DF).toString());
        string = string.replaceAll("df", Byte.valueOf(DF).toString());
        string = string.replaceAll("IF", Byte.valueOf(IF).toString());
        string = string.replaceAll("if", Byte.valueOf(IF).toString());
        string = string.replaceAll("FF", Byte.valueOf(FF).toString());
        string = string.replaceAll("ff", Byte.valueOf(FF).toString());
        string = string.replaceAll("LF", Byte.valueOf(LF).toString());
        string = string.replaceAll("lf", Byte.valueOf(LF).toString());

        operatorValue = createOperatorValue(string);
        return operatorValue;
    }

    private class Token {
        byte operator;
        byte[] value;
        int index;
        boolean error;
    }

    List<ExtOperatorValue> createOperatorValue(String string) {
        List<ExtOperatorValue> operatorValue  = new ArrayList<>();
        Token token;
        int index = 0;

        do {
            token = parseMultiTokenValue(string, index);
            if (token.error) {
                log.error("Error in parsing the operator value list");
                return null;
            }
            operatorValue.add(new ExtOperatorValue(token.operator, token.value));
            index = token.index;
        } while ((token.operator & endBit) != endBit);

        return operatorValue;
    }


    Token parseMultiTokenValue(String str, int index) {
        Token token = new Token();
        token.error = true;
        int number = 0;
        int cur = 0;
        byte operator = 0;
        boolean prevNumber = false;
        boolean bLess = false;
        boolean bGreater = false;
        boolean bAnd = false;
        boolean bEqual = false;

        while (str.length() > index) {

            switch (str.charAt(index)) {
                case '=':
                    if (bEqual) {
                        return token;
                    }
                    bEqual = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | equal);
                    break;
                case '>':
                    if (bGreater) {
                        return token;
                    }
                    bGreater = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | greaterThan);
                    break;
                case '<':
                    if (bLess) {
                        return token;
                    }
                    bLess = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | lessThan);
                    break;
                case '&':
                    if (bAnd) {
                        return token;
                    }
                    bAnd = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | andBit);
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    prevNumber = true;
                    bLess = false;
                    bGreater = false;
                    bAnd = false;
                    bEqual = false;
                    cur =  str.charAt(index);
                    number = (number * 10) + cur - '0';
                    break;

                default:
                    log.error("Error in parsing the token character" + str.charAt(index));
                    return token;
            }
            index++;
        }

        if (prevNumber) {
            return windUp(str, index, number, operator);
        }

        return token;
    }

    Token windUp(String str, int index, int number, byte operator) {
        Token token = new Token();
        byte[] array = new byte[1];

        if (str.length() == index) {
            operator = (byte) (operator | endBit);
        }

        if (number <= 255) {
            array[0] = (byte) number;
        } else if (number > 255 && number <= Short.MAX_VALUE) {
            operator = (byte) (operator | twoByteLen);
            array = shortToByteStream((short) number);
        } else if (number > Short.MAX_VALUE) {
            operator = (byte) (operator | fourByteLen);
            array = intToByteStream(number);
        }

        token.value = array;
        token.operator = operator;
        token.index = index;
        token.error = false;
        return token;
    }

    byte[] intToByteStream(int val) {
        return new byte[] {
                (byte) (val >>> 24),
                (byte) (val >>> 16),
                (byte) (val >>> 8),
                (byte) val};
    }

    byte[] shortToByteStream(short val) {
        return new byte[] {
                (byte) (val >>> 8),
                (byte) val};
    }

    Token parseTcpTokenValue(String str, int index) {
        Token token = new Token();
        token.error = true;
        int number = 0;
        int cur = 0;
        byte operator = 0;
        boolean prevNumber = false;

        boolean bNotBit = false;
        boolean bAnd = false;
        boolean bMatchBit = false;

        while (str.length() > index) {

            switch (str.charAt(index)) {
                case '=':
                    if (bMatchBit) {
                        return token;
                    }
                    bMatchBit = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | matchBit);
                    break;
                case '!':
                    if (bNotBit) {
                        return token;
                    }
                    bNotBit = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | notBit);
                    break;
                case '&':
                    if (bAnd) {
                        return token;
                    }
                    bAnd = true;
                    if (prevNumber) {
                        return windUp(str, index, number, operator);
                    }
                    operator = (byte) (operator | andBit);
                    break;
                case '0':
                case '1':
                case '2':
                case '3':
                case '4':
                case '5':
                case '6':
                case '7':
                case '8':
                case '9':
                    prevNumber = true;
                    bNotBit = false;
                    bAnd = false;
                    bMatchBit = false;
                    cur =  str.charAt(index);
                    number = (number * 10) + cur - '0';
                    break;

                default:
                    log.error("Error in parsing the TCP token character" + str.charAt(index));
                    return token;
            }
            index++;
        }

        if (prevNumber) {
            return windUp(str, index, number, operator);
        }

        return token;
    }
}
