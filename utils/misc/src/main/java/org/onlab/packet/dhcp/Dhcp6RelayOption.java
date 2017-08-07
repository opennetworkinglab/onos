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

import com.google.common.base.MoreObjects;
import org.onlab.packet.DHCP6;
import org.onlab.packet.Deserializer;
import org.onlab.packet.IPacket;

/**
 * Relay option for DHCPv6.
 * Based on RFC-3315.
 */
public final class Dhcp6RelayOption extends Dhcp6Option {
    @Override
    public short getCode() {
        return DHCP6.OptionCode.RELAY_MSG.value();
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
    public Dhcp6RelayOption() {
    }

    /**
     * Constructs a DHCPv6 relay option with DHCPv6 option.
     *
     * @param dhcp6Option the DHCPv6 option
     */
    public Dhcp6RelayOption(Dhcp6Option dhcp6Option) {
        super(dhcp6Option);
    }

    /**
     * Gets deserializer for DHCPv6 relay option.
     *
     * @return the deserializer
     */
    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, len) -> {
            Dhcp6Option dhcp6Option = Dhcp6Option.deserializer().deserialize(data, offset, len);
            IPacket payload = DHCP6.deserializer()
                    .deserialize(dhcp6Option.getData(), 0, dhcp6Option.getLength());
            Dhcp6RelayOption relayOption = new Dhcp6RelayOption(dhcp6Option);
            relayOption.setPayload(payload);
            payload.setParent(relayOption);
            return relayOption;
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
