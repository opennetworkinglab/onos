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
import org.onlab.packet.MacAddress;
import com.google.common.base.MoreObjects;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Deserializer;
import org.onlab.packet.DeserializationException;


import java.nio.ByteBuffer;

/**
 * Relay option for DHCPv6.
 * Based on RFC-3315.
 */
public final class Dhcp6InterfaceIdOption extends Dhcp6Option {
    private MacAddress peerMacAddr;
    private byte[] inPort;
    @Override
    public short getCode() {
        return DHCP6.OptionCode.INTERFACE_ID.value();
    }

    @Override
    public short getLength() {
        return (short) payload.serialize().length;
    }

    @Override
    public byte[] getData() {
        return this.payload.serialize();
    }

    /**
     * Default constructor.
     */
    public Dhcp6InterfaceIdOption() {
    }

    /**
     * Constructs a DHCPv6 relay option with DHCPv6 option.
     *
     * @param dhcp6Option the DHCPv6 option
     */
    public Dhcp6InterfaceIdOption(Dhcp6Option dhcp6Option) {
        super(dhcp6Option);
    }

    /**
     * Sets MacAddress address.
     *
     * @param macAddress the client peer MacAddress
     */
    public void setMacAddress(MacAddress macAddress) {
        this.peerMacAddr = macAddress;
    }

    /**
     * Gets Mac address.
     *
     * @return the client peer mac address
     */
    public MacAddress getMacAddress() {
        return peerMacAddr;
    }

    /**
     * Sets inPort string.
     *
     * @param port the port from which client packet is received
     */
    public void setInPort(byte[] port) {
        this.inPort = port;
    }

    /**
     * Gets inPort string.
     *
     * @return the port from which client packet is received
     */
    public byte[] getInPort() {
        return inPort;
    }

    /**
     * Gets deserializer for DHCPv6 relay option.
     *
     * @return the deserializer
     */
    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, len) -> {
            Dhcp6Option dhcp6Option = Dhcp6Option.deserializer().deserialize(data, offset, len);
            if (dhcp6Option.getLength() < DEFAULT_LEN) {
                throw new DeserializationException("Invalid InterfaceIoption data");
            }
            Dhcp6InterfaceIdOption interfaceIdOption = new Dhcp6InterfaceIdOption(dhcp6Option);
            byte[] optionData = interfaceIdOption.getData();
            if (optionData.length >= 28) {
                ByteBuffer bb = ByteBuffer.wrap(optionData);

                byte[] macAddr = new byte[MacAddress.MAC_ADDRESS_LENGTH];
                byte[] port = new byte[21];
                bb.get(macAddr);
                bb.get();  // separator
                bb.get(port);
                interfaceIdOption.setMacAddress(MacAddress.valueOf(macAddr));
                interfaceIdOption.setInPort(port);
            }
            return interfaceIdOption;
        };
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("code", getCode())
                .add("length", getLength())
                .add("data", payload.toString())
                .toString();
    }
}
