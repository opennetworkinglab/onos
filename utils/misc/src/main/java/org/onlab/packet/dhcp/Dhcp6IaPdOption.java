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

/**
 * DHCPv6 Identity Association for Prefix Delegation Option.
 * Based on RFC-3633
 */
public final class Dhcp6IaPdOption extends Dhcp6Option {
    public static final int DEFAULT_LEN = 12;
    private int iaId;
    private int t1;
    private int t2;
    private List<Dhcp6Option> options;

    @Override
    public short getCode() {
        return DHCP6.OptionCode.IA_PD.value();
    }

    @Override
    public short getLength() {
        return (short) (DEFAULT_LEN + options.stream()
                        .mapToInt(opt -> (int) opt.getLength() + Dhcp6Option.DEFAULT_LEN)
                        .sum());

    }

    /**
     * Gets Identity Association ID.
     * The unique identifier for this IA_PD; the IAID must
     * be unique among the identifiers for all of this
     * requesting router's IA_PDs.
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
     * Gets time 1.
     * The time at which the requesting router should
     * contact the delegating router from which the
     * prefixes in the IA_PD were obtained to extend the
     * lifetimes of the prefixes delegated to the IA_PD;
     * T1 is a time duration relative to the current time
     * expressed in units of seconds.
     *
     * @return the value of time 1
     */
    public int getT1() {
        return t1;
    }

    /**
     * Sets time 1.
     *
     * @param t1 the value of time 1
     */
    public void setT1(int t1) {
        this.t1 = t1;
    }

    /**
     * Gets time 2.
     * The time at which the requesting router should
     * contact any available delegating router to extend
     * the lifetimes of the prefixes assigned to the
     * IA_PD; T2 is a time duration relative to the
     * current time expressed in units of seconds.
     *
     * @return the value of time 2
     */
    public int getT2() {
        return t2;
    }

    /**
     * Sets time 2.
     *
     * @param t2 the value of time 2
     */
    public void setT2(int t2) {
        this.t2 = t2;
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
    public Dhcp6IaPdOption() {
    }

    /**
     * Constructs a DHCPv6 IA PD option with DHCPv6 option.
     *
     * @param dhcp6Option the DHCPv6 option
     */
    public Dhcp6IaPdOption(Dhcp6Option dhcp6Option) {
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
                throw new DeserializationException("Invalid IA PD option data");
            }
            Dhcp6IaPdOption iaPdOption = new Dhcp6IaPdOption(dhcp6Option);
            byte[] optionData = iaPdOption.getData();
            ByteBuffer bb = ByteBuffer.wrap(optionData);
            iaPdOption.iaId = bb.getInt();
            iaPdOption.t1 = bb.getInt();
            iaPdOption.t2 = bb.getInt();

            iaPdOption.options = Lists.newArrayList();
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
                if (code == DHCP6.OptionCode.IAPREFIX.value()) {
                    option = Dhcp6IaPrefixOption.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                } else {
                    option = Dhcp6Option.deserializer()
                            .deserialize(subOptData, 0, subOptData.length);
                }
                iaPdOption.options.add(option);
            }
            return iaPdOption;
        };
    }

    @Override
    public byte[] serialize() {
        int payloadLen = DEFAULT_LEN + options.stream()
                .mapToInt(opt -> (int) opt.getLength() + Dhcp6Option.DEFAULT_LEN)
                .sum();
        int len = Dhcp6Option.DEFAULT_LEN + payloadLen;
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.putShort(DHCP6.OptionCode.IA_PD.value());
        bb.putShort((short) payloadLen);
        bb.putInt(iaId);
        bb.putInt(t1);
        bb.putInt(t2);

        options.stream().map(Dhcp6Option::serialize).forEach(bb::put);
        return bb.array();
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hash(iaId, t1, t2, options);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final Dhcp6IaPdOption other = (Dhcp6IaPdOption) obj;
        return Objects.equals(this.iaId, other.iaId)
                && Objects.equals(this.t1, other.t1)
                && Objects.equals(this.t2, other.t2)
                && Objects.equals(this.options, other.options);
    }

    @Override
    public String toString() {
        return getToStringHelper()
                .add("iaId", iaId)
                .add("t1", t1)
                .add("t2", t2)
                .add("options", options)
                .toString();
    }
}
