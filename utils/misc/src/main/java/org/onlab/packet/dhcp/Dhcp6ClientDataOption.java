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
import com.google.common.collect.Lists;
import org.onlab.packet.DHCP6;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;
import org.onlab.packet.Ip6Address;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * DHCPv6 Client Data Option.
 */
public final class Dhcp6ClientDataOption extends Dhcp6Option {
    private List<Dhcp6Option> options;
    private Ip6Address clientIaAddress;
    public static final int DEFAULT_LEN = 1 + 16;

    public Dhcp6ClientDataOption(Dhcp6Option dhcp6Option) {
        super(dhcp6Option);
    }

    @Override
    public short getCode() {
        return DHCP6.OptionCode.CLIENT_DATA.value();
    }

    @Override
    public short getLength() {
        //return (short) (DEFAULT_LEN + options.stream()
        //        .mapToInt(opt -> (int) opt.getLength() + Dhcp6Option.DEFAULT_LEN)
        //        .sum());
        return (short) payload.serialize().length;
    }

    @Override
    public byte[] getData() {
        return payload.serialize();
    }

    public List<Dhcp6Option> getOptions() {
        return options;
    }

    public Ip6Address getIaAddress() {
        return clientIaAddress;
    }

    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, length) -> {
            Dhcp6Option dhcp6Option = Dhcp6Option.deserializer().deserialize(data, offset, length);
            Dhcp6ClientDataOption clientData = new Dhcp6ClientDataOption(dhcp6Option);

            if (dhcp6Option.getLength() < DEFAULT_LEN) {
                throw new DeserializationException("Invalid length of Client Id option");
            }

            byte[] optionData = clientData.getData();

            clientData.options = Lists.newArrayList();

            ByteBuffer bb = ByteBuffer.wrap(optionData);

            while (bb.remaining() >= Dhcp6Option.DEFAULT_LEN) {
                Dhcp6Option option;
                ByteBuffer optByteBuffer = ByteBuffer.wrap(optionData,
                                                           bb.position(),
                                                           optionData.length - bb.position());
                short code = optByteBuffer.getShort();
                short len = optByteBuffer.getShort();
                int optLen = UNSIGNED_SHORT_MASK & len;
                byte[] subOptData = new byte[Dhcp6Option.DEFAULT_LEN + optLen];
                bb.get(subOptData);

                // TODO: put more sub-options?
                if (code == DHCP6.OptionCode.IAADDR.value()) {
                    option = Dhcp6IaAddressOption.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                    clientData.clientIaAddress  = ((Dhcp6IaAddressOption) option).getIp6Address();
                } else if (code == DHCP6.OptionCode.CLIENTID.value()) {
                    option = Dhcp6ClientIdOption.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                } else if (code == DHCP6.OptionCode.CLIENT_LT.value()) {
                    option = Dhcp6CLTOption.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                } else {
                    option = Dhcp6Option.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                }
                clientData.options.add(option);
            }
            return clientData;
        };
    }

    @Override
    public byte[] serialize() {
        ByteBuffer bb = ByteBuffer.allocate(this.getLength() + Dhcp6Option.DEFAULT_LEN);
        bb.putShort(getCode());
        bb.putShort(getLength());
        bb.put(payload.serialize());
        return bb.array();
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("code", getCode())
                .add("length", getLength())
                .add("clientAddr", getIaAddress())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clientIaAddress, options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Dhcp6ClientDataOption)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final Dhcp6ClientDataOption other = (Dhcp6ClientDataOption) obj;

        return Objects.equals(getCode(), other.getCode()) &&
                Objects.equals(getLength(), other.getLength()) &&
                Objects.equals(clientIaAddress, other.clientIaAddress) &&
                Objects.equals(options, other.options);
    }
}
