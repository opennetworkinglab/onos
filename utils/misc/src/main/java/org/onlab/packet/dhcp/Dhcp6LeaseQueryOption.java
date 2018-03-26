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
import org.onlab.packet.Deserializer;
import org.onlab.packet.Ip6Address;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

/**
 * DHCPv6 Lease Query Option.
 */
public final class Dhcp6LeaseQueryOption extends Dhcp6Option {

    public static final int DEFAULT_LEN = 1 + 16;
    //public short QueryType;
    public Ip6Address linkAddress;
    private List<Dhcp6Option> options;
    public byte queryType;

    public Dhcp6LeaseQueryOption(Dhcp6Option dhcp6Option) {
        super(dhcp6Option);
        options =  Lists.newArrayList();
    }

    @Override
    public short getCode() {
        return DHCP6.OptionCode.LEASE_QUERY.value();
    }

    @Override
    public short getLength() {
        //return (short) payload.serialize().length;
        return (short) (DEFAULT_LEN + options.stream()
                .mapToInt(opt -> (int) opt.getLength() + Dhcp6Option.DEFAULT_LEN)
                .sum());
    }

    @Override
    public byte[] getData() {
        return payload.serialize();
    }


    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, length) -> {
            Dhcp6Option dhcp6Option = Dhcp6Option.deserializer().deserialize(data, offset, length);
            Dhcp6LeaseQueryOption lQ6Option = new Dhcp6LeaseQueryOption(dhcp6Option);

            byte[] optionData = lQ6Option.getData();
            if (optionData.length >= DEFAULT_LEN) {
                ByteBuffer bb = ByteBuffer.wrap(optionData);
                // fetch the Query type - just pop the byte from the byte buffer for subsequent parsing...
                lQ6Option.queryType = bb.get();
                byte[] ipv6Addr = new byte[16];
                bb.get(ipv6Addr);
                lQ6Option.linkAddress = Ip6Address.valueOf(ipv6Addr);
                //int optionsLen = dhcp6Option.getLength() - 1 - 16; // query type (1) + link address (16)

                lQ6Option.options = Lists.newArrayList();

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
                    } else if (code == DHCP6.OptionCode.ORO.value()) {
                        option = Dhcp6Option.deserializer()
                                    .deserialize(subOptData, 0, subOptData.length);
                    } else {
                        option = Dhcp6Option.deserializer()
                                .deserialize(subOptData, 0, subOptData.length);
                    }
                    lQ6Option.options.add(option);
                }
            }
            return lQ6Option;
        };
    }

    @Override
    public byte[] serialize() {
        byte[] serializedPayload = payload.serialize();

        ByteBuffer bb = ByteBuffer.allocate(serializedPayload.length + Dhcp6Option.DEFAULT_LEN);
        bb.putShort(getCode());
        bb.putShort(getLength());
        bb.put(serializedPayload);

        return bb.array();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("code", getCode())
                .add("length", getLength())
                .toString();
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), linkAddress, options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Dhcp6LeaseQueryOption)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final Dhcp6LeaseQueryOption other = (Dhcp6LeaseQueryOption) obj;

        return Objects.equals(getCode(), other.getCode()) &&
                Objects.equals(getLength(), other.getLength()) &&
                Objects.equals(linkAddress, other.linkAddress) &&
                Objects.equals(options, other.options);
    }
}
