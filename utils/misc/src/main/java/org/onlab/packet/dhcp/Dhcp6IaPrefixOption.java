/*
 * Copyright 2017-present Open Networking Foundation
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
 *
 */

package org.onlab.packet.dhcp;

import org.onlab.packet.DHCP6;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;
import org.onlab.packet.Ip6Address;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * IA Address option for DHCPv6.
 * Based on RFC-3633.
 */
public final class Dhcp6IaPrefixOption extends Dhcp6Option {
    public static final int DEFAULT_LEN = 25;

    private Ip6Address ip6Prefix;
    private byte prefixLength;
    private int preferredLifetime;
    private int validLifetime;
    private IPacket options;

    @Override
    public short getCode() {
        return DHCP6.OptionCode.IAPREFIX.value();
    }

    @Override
    public short getLength() {
        return (short) (options == null ? DEFAULT_LEN : DEFAULT_LEN + options.serialize().length);
    }

    /**
     * Sets IPv6 prefix.
     *
     * @param ip6Prefix the IPv6 prefix
     */
    public void setIp6Prefix(Ip6Address ip6Prefix) {
        this.ip6Prefix = ip6Prefix;
    }

    /**
     * Sets prefix length.
     *
     * @param prefixLength the prefix length
     */
    public void setPrefixLength(byte prefixLength) {
        this.prefixLength = prefixLength;
    }

    /**
     * Sets preferred lifetime.
     *
     * @param preferredLifetime the preferred lifetime
     */
    public void setPreferredLifetime(int preferredLifetime) {
        this.preferredLifetime = preferredLifetime;
    }

    /**
     * Sets valid lifetime.
     *
     * @param validLifetime the valid lifetime
     */
    public void setValidLifetime(int validLifetime) {
        this.validLifetime = validLifetime;
    }

    /**
     * Sets options data.
     *
     * @param options the options data
     */
    public void setOptions(IPacket options) {
        this.options = options;
    }

    /**
     * Gets IPv6 address.
     *
     * @return the IPv6 address
     */
    public Ip6Address getIp6Prefix() {
        return ip6Prefix;
    }

    /**
     * Gets prefix length.
     *
     * @return the prefix length
     */
    public byte getPrefixLength() {
        return prefixLength;
    }

    /**
     * Gets preferred lifetime.
     *
     * @return the preferred lifetime
     */
    public int getPreferredLifetime() {
        return preferredLifetime;
    }

    /**
     * Gets valid lifetime.
     *
     * @return the valid lifetime
     */
    public int getValidLifetime() {
        return validLifetime;
    }

    /**
     * Gets options of IA Address option.
     *
     * @return the options data
     */
    public IPacket getOptions() {
        return options;
    }

    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, length) -> {
            Dhcp6IaPrefixOption iaPrefixOption = new Dhcp6IaPrefixOption();
            Dhcp6Option dhcp6Option =
                    Dhcp6Option.deserializer().deserialize(data, offset, length);
            iaPrefixOption.setPayload(dhcp6Option.getPayload());
            if (dhcp6Option.getLength() < DEFAULT_LEN) {
                throw new DeserializationException("Invalid length of IA prefix option");
            }
            ByteBuffer bb = ByteBuffer.wrap(dhcp6Option.getData());
            iaPrefixOption.preferredLifetime = bb.getInt();
            iaPrefixOption.validLifetime = bb.getInt();
            iaPrefixOption.prefixLength = bb.get();
            byte[] ipv6Pref = new byte[Ip6Address.BYTE_LENGTH];
            bb.get(ipv6Pref);
            iaPrefixOption.ip6Prefix = Ip6Address.valueOf(ipv6Pref);

            // options length of IA Address option
            int optionsLen = dhcp6Option.getLength() - DEFAULT_LEN;
            if (optionsLen > 0) {
                byte[] optionsData = new byte[optionsLen];
                bb.get(optionsData);
                iaPrefixOption.options =
                        Data.deserializer().deserialize(optionsData, 0, optionsLen);
            }
            return iaPrefixOption;
        };
    }

    @Override
    public byte[] serialize() {
        int payloadLen = options == null ? DEFAULT_LEN : DEFAULT_LEN + options.serialize().length;
        ByteBuffer bb = ByteBuffer.allocate(payloadLen + Dhcp6Option.DEFAULT_LEN);
        bb.putShort(DHCP6.OptionCode.IAPREFIX.value());
        bb.putShort((short) payloadLen);
        bb.putInt(preferredLifetime);
        bb.putInt(validLifetime);
        bb.put(prefixLength);
        bb.put(ip6Prefix.toOctets());
        if (options != null) {
            bb.put(options.serialize());
        }
        return bb.array();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), ip6Prefix, prefixLength, preferredLifetime,
                            validLifetime, options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Dhcp6IaPrefixOption)) {
            return false;
        }
        final Dhcp6IaPrefixOption other = (Dhcp6IaPrefixOption) obj;

        return Objects.equals(getCode(), other.getCode()) &&
                Objects.equals(getLength(), other.getLength()) &&
                Objects.equals(preferredLifetime, other.preferredLifetime) &&
                Objects.equals(validLifetime, other.validLifetime) &&
                Objects.equals(prefixLength, other.prefixLength) &&
                Objects.equals(ip6Prefix, other.ip6Prefix) &&
                Objects.equals(options, other.options);
    }

    @Override
    public String toString() {
        return getToStringHelper()
                .add("preferredLifetime", preferredLifetime)
                .add("validLifetime", validLifetime)
                .add("prefixLength", prefixLength)
                .add("ip6Address", ip6Prefix)
                .add("options", options)
                .toString();
    }
}
