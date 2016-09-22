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
package org.onlab.packet;

/**
 * Represents the DiffServ classes defined by the IPv4/IPv6 DSCP value.
 * DSCP occupies the 6 most-significant bits of the IPv4/IPv6 DS field
 */
public enum DscpClass {

    BE((short) 0b000000),
    AF11((short) 0b001010),
    AF12((short) 0b001100),
    AF13((short) 0b001110),
    AF21((short) 0b010010),
    AF22((short) 0b010100),
    AF23((short) 0b010110),
    AF31((short) 0b011010),
    AF32((short) 0b011100),
    AF33((short) 0b011110),
    AF41((short) 0b100010),
    AF42((short) 0b100100),
    AF43((short) 0b100110),
    CS1((short) 0b001000),
    CS2((short) 0b010000),
    CS3((short) 0b011000),
    CS4((short) 0b100000),
    CS5((short) 0b101000),
    CS6((short) 0b110000),
    CS7((short) 0b111000),
    EF((short) 0b101110);

    private static final short IP_PREC_MASK = 0b111000;
    private static final short IP_PREC_RSHIFT = 3;
    private static final short DROP_PREC_MASK = 0b000110;
    private static final short DROP_PREC_RSHIFT = 1;

    private short value;

    DscpClass(short value) {
        this.value = value;
    }

    /**
     * Returns the DSCP class Enum corresponding to the specified short.
     *
     * @param value the short value of the DSCP class
     * @return the DSCP class Enum corresponding to the specified short
     * @throws IllegalArgumentException if the short provided does not
     * correspond to an DSCP class Enum value
     */
    public static DscpClass fromShort(short value) {
        for (DscpClass b : DscpClass.values()) {
            if (value == b.value) {
                return b;
            }
        }
        throw new IllegalArgumentException("DSCP class " + value + " is not valid");
    }

    /**
     * Returns the short value of this DSCP class Enum.
     *
     * @return the short value of this DSCP class Enum
     */
    public short getValue() {
        return value;
    }

    /**
     * Returns the corresponding IP precedence.
     *
     * @return the corresponding IP precedence
     */
    public IPPrecedence getIPPrecedence() {
        return IPPrecedence.fromShort((short) ((value & IP_PREC_MASK) >> IP_PREC_RSHIFT));
    }

    /**
     * Returns the corresponding drop precedence.
     *
     * @return the corresponding drop precedence
     */
    public short getDropPrecedence() {
        return (short) ((value & DROP_PREC_MASK) >> DROP_PREC_RSHIFT);
    }
}
