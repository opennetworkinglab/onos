/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The class representing MAC address.
 */
public class MacAddress {

    private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:]){5}([0-9A-Fa-f]{2})$");
    /**
     * First MAC address in ONOS OUI range.
     */
    public static final MacAddress ONOS = valueOf("a4:23:05:00:00:00");
    /**
     * Dummy MAC address. We use the first MAC address in ONOS OUI range as the dummy MAC address.
     */
    public static final MacAddress NONE = ONOS;
    /**
     * ONOS LLDP MAC address with slow protocol destination address.
     */
    public static final MacAddress ONOS_LLDP = valueOf("01:80:C2:00:00:0E");
    /**
     * All-zero MAC address.
     */
    public static final MacAddress ZERO = valueOf("00:00:00:00:00:00");
    /**
     * Broadcast MAC address.
     */
    public static final MacAddress BROADCAST = valueOf("ff:ff:ff:ff:ff:ff");
    /**
     * Mask that indicates exact match on the MacAddress.
     */
    public static final MacAddress EXACT_MASK = valueOf("ff:ff:ff:ff:ff:ff");
    /**
     * IPv4 multicast MAC address.
     */
    public static final MacAddress IPV4_MULTICAST = valueOf("01:00:5e:00:00:00");
    /**
     * IPv4 multicast MAC mask.
     */
    public static final MacAddress IPV4_MULTICAST_MASK = valueOf("ff:ff:ff:80:00:00");
    /**
     * IPv6 multicast MAC address.
     */
    public static final MacAddress IPV6_MULTICAST = valueOf("33:33:00:00:00:00");
    /**
     * IPv6 multicast MAC mask.
     */
    public static final MacAddress IPV6_MULTICAST_MASK = valueOf("FF:FF:00:00:00:00");
    /**
     * A set of LLDP MAC addresses.
     */
    public static final Set<MacAddress> LLDP = ImmutableSet.of(
            MacAddress.valueOf("01:80:c2:00:00:00"),
            MacAddress.valueOf("01:80:c2:00:00:03"),
            MacAddress.valueOf("01:80:c2:00:00:0e"));
    /**
     * LACP MAC address.
     */
    public static final MacAddress LACP = valueOf("01:80:C2:00:00:02");

    public static final int MAC_ADDRESS_LENGTH = 6;
    private byte[] address = new byte[MacAddress.MAC_ADDRESS_LENGTH];

    public MacAddress(final byte[] address) {
        this.address = Arrays.copyOf(address, MacAddress.MAC_ADDRESS_LENGTH);
    }

    /**
     * Returns a MAC address instance representing the value of the specified
     * {@code String}.
     *
     * @param address the String representation of the MAC Address to be parsed.
     * @return a MAC Address instance representing the value of the specified
     * {@code String}.
     * @throws IllegalArgumentException if the string cannot be parsed as a MAC address.
     */
    public static MacAddress valueOf(final String address) {
        if (!isValid(address)) {
            throw new IllegalArgumentException(
                    "Specified MAC Address must contain 12 hex digits"
                            + " separated pairwise by :'s.");
        }
        final String[] elements = address.split(":");
        final byte[] addressInBytes = new byte[MacAddress.MAC_ADDRESS_LENGTH];
        for (int i = 0; i < MacAddress.MAC_ADDRESS_LENGTH; i++) {
            final String element = elements[i];
            addressInBytes[i] = (byte) Integer.parseInt(element, 16);
        }

        return new MacAddress(addressInBytes);
    }

    /**
     * Returns a MAC address instance representing the specified {@code byte}
     * array.
     *
     * @param address the byte array to be parsed.
     * @return a MAC address instance representing the specified {@code byte}
     * array.
     * @throws IllegalArgumentException if the byte array cannot be parsed as a MAC address.
     */
    public static MacAddress valueOf(final byte[] address) {
        if (address.length != MacAddress.MAC_ADDRESS_LENGTH) {
            throw new IllegalArgumentException("the length is not "
                                                       + MacAddress.MAC_ADDRESS_LENGTH);
        }

        return new MacAddress(address);
    }

    /**
     * Returns a MAC address instance representing the specified {@code long}
     * value. The lower 48 bits of the long value are used to parse as a MAC
     * address.
     *
     * @param address the long value to be parsed. The lower 48 bits are used for a
     *                MAC address.
     * @return a MAC address instance representing the specified {@code long}
     * value.
     * @throws IllegalArgumentException if the long value cannot be parsed as a MAC address.
     */
    public static MacAddress valueOf(final long address) {
        final byte[] addressInBytes = new byte[]{
                (byte) (address >> 40 & 0xff), (byte) (address >> 32 & 0xff),
                (byte) (address >> 24 & 0xff), (byte) (address >> 16 & 0xff),
                (byte) (address >> 8 & 0xff), (byte) (address >> 0 & 0xff)};

        return new MacAddress(addressInBytes);
    }

    /**
     * Returns the length of the {@code MACAddress}.
     *
     * @return the length of the {@code MACAddress}.
     */
    public int length() {
        return this.address.length;
    }

    /**
     * Returns the value of the {@code MACAddress} as a {@code byte} array.
     *
     * @return the numeric value represented by this object after conversion to
     * type {@code byte} array.
     */
    public byte[] toBytes() {
        return Arrays.copyOf(this.address, this.address.length);
    }

    /**
     * Returns the value of the {@code MACAddress} as a {@code long}.
     *
     * @return the numeric value represented by this object after conversion to
     * type {@code long}.
     */
    public long toLong() {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            final long t = (this.address[i] & 0xffL) << (5 - i) * 8;
            mac |= t;
        }
        return mac;
    }

    /**
     * Returns {@code true} if the MAC address is the broadcast address.
     *
     * @return {@code true} if the MAC address is the broadcast address.
     */
    public boolean isBroadcast() {
        for (final byte b : this.address) {
            if (b != -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns {@code true} if the MAC address is the multicast address.
     *
     * @return {@code true} if the MAC address is the multicast address.
     */
    public boolean isMulticast() {
        if (this.isBroadcast()) {
            return false;
        }
        return (this.address[0] & 0x01) != 0;
    }

    /**
     * Returns true if this MAC address is used by link layer discovery protocol.
     *
     * @return true if this MAC is LLDP MAC.
     */
    public boolean isLldp() {
        return LLDP.contains(this);
    }

    /**
     * Returns true if the Organizationally Unique Identifier (OUI) of this MAC
     * address matches ONOS OUI.
     *
     * @return true if the OUI of this MAC address matches ONOS OUI.
     */
    public boolean isOnos() {
        return Arrays.equals(this.oui(), ONOS.oui());
    }

    /**
     * Returns the Organizationally Unique Identifier (OUI) of this MAC address.
     *
     * @return the OUI of this MAC address.
     */
    public byte[] oui() {
        return Arrays.copyOfRange(this.address, 0, 3);
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof MacAddress)) {
            return false;
        }

        final MacAddress other = (MacAddress) o;
        return Arrays.equals(this.address, other.address);
    }

    @Override
    public int hashCode() {
        return Long.hashCode(toLong());
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder(2 * 6 + 5);
        for (final byte b : this.address) {
            if (builder.length() > 0) {
                builder.append(':');
            }
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }

    /**
     * @return MAC address in string representation without colons (useful for
     * radix tree storage)
     */
    public String toStringNoColon() {
        final StringBuilder builder = new StringBuilder();
        for (final byte b : this.address) {
            builder.append(String.format("%02X", b & 0xFF));
        }
        return builder.toString();
    }

    private static boolean isValid(final String mac) {
        Matcher matcher = MAC_PATTERN.matcher(mac);
        return matcher.matches();
    }

    /**
     * Checks if the Mac Address is inside a range defined by macAddr and mask.
     *
     * @param macAddr the mac address
     * @param maskAddr the mask
     * @return true if in range, false otherwise.
     */
    public boolean inRange(MacAddress macAddr, MacAddress maskAddr) {
        byte[] min = macAddr.toBytes();
        byte[] mask = maskAddr.toBytes();
        boolean inRange = true;

        int i = 0;

        //if mask is 00 stop
        while (inRange && i < mask.length && (mask[i] & 0xFF) != 0) {
            int ibmac = this.address[i] & 0xFF;
            int ibmin = min[i] & 0xFF;
            int ibmask = mask[i] & 0xFF;
            if (ibmask == 255) {
                inRange = ibmac == ibmin;
            } else if (ibmac < ibmin || ibmac >= ibmask) {
                inRange = false;
                break;
            }
            i++;
        }

        return inRange;
    }
}
