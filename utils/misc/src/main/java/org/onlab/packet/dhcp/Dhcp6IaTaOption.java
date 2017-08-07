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

import com.google.common.collect.Lists;
import org.onlab.packet.DHCP6;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;

public final class Dhcp6IaTaOption extends Dhcp6Option {
    public static final int DEFAULT_LEN = 4;
    private int iaId;
    private List<Dhcp6Option> options;

    @Override
    public short getCode() {
        return DHCP6.OptionCode.IA_TA.value();
    }

    @Override
    public short getLength() {
        return (short) (DEFAULT_LEN + options.stream()
                .mapToInt(opt -> (int) opt.getLength() + Dhcp6Option.DEFAULT_LEN)
                .sum());
    }

    /**
     * Gets Identity Association ID.
     *
     * @return the Identity Association ID
     */
    public int getIaId() {
        return iaId;
    }

    /**
     * Sets Identity Association ID.
     *
     * @param iaId the Identity Association ID.
     */
    public void setIaId(int iaId) {
        this.iaId = iaId;
    }

    /**
     * Gets sub-options.
     *
     * @return sub-options of this option
     */
    public List<Dhcp6Option> getOptions() {
        return options;
    }

    /**
     * Sets sub-options.
     *
     * @param options the sub-options of this option
     */
    public void setOptions(List<Dhcp6Option> options) {
        this.options = options;
    }

    /**
     * Default constructor.
     */
    public Dhcp6IaTaOption() {
    }

    /**
     * Constructs a DHCPv6 IA TA option with DHCPv6 option.
     *
     * @param dhcp6Option the DHCPv6 option
     */
    public Dhcp6IaTaOption(Dhcp6Option dhcp6Option) {
        super(dhcp6Option);
    }

    /**
     * Gets deserializer.
     *
     * @return the deserializer
     */
    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, length) -> {
            Dhcp6Option dhcp6Option =
                    Dhcp6Option.deserializer().deserialize(data, offset, length);
            if (dhcp6Option.getLength() < DEFAULT_LEN) {
                throw new DeserializationException("Invalid IA NA option data");
            }
            Dhcp6IaTaOption iaTaOption = new Dhcp6IaTaOption(dhcp6Option);
            byte[] optionData = iaTaOption.getData();
            ByteBuffer bb = ByteBuffer.wrap(optionData);
            iaTaOption.iaId = bb.getInt();

            iaTaOption.options = Lists.newArrayList();
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
                } else {
                    option = Dhcp6Option.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                }
                iaTaOption.options.add(option);
            }
            return iaTaOption;
        };
    }

    @Override
    public byte[] serialize() {
        int payloadLen = DEFAULT_LEN + options.stream()
                .mapToInt(opt -> (int) opt.getLength())
                .sum();
        int len = Dhcp6Option.DEFAULT_LEN + payloadLen;
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.putShort(DHCP6.OptionCode.IA_TA.value());
        bb.putShort((short) payloadLen);
        bb.putInt(iaId);
        options.stream().map(Dhcp6Option::serialize).forEach(bb::put);
        return bb.array();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(iaId, options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Dhcp6IaTaOption other = (Dhcp6IaTaOption) obj;
        return Objects.equals(this.iaId, other.iaId)
                && Objects.equals(this.options, other.options);
    }

    @Override
    public String toString() {
        return getToStringHelper()
                .add("iaId", iaId)
                .add("options", options)
                .toString();
    }
}
