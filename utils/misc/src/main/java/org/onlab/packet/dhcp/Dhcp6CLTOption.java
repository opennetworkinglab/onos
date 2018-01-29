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
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;
import java.util.Objects;

public final class Dhcp6CLTOption extends Dhcp6Option {
    public static final int DEFAULT_LEN = 4;
    private int clt; // client last transaction time

    @Override
    public short getCode() {
        return DHCP6.OptionCode.CLIENT_LT.value();
    }

    @Override
    public short getLength() {
        return (short) (DEFAULT_LEN);
    }

    /**
     * Gets Client Last Transaction Time.
     *
     * @return Client Last Transaction Time
     */
    public int getClt() {
        return clt;
    }

    /**
     * Sets Identity Association ID.
     *
     * @param clt the Client Last Transaction Time.
     */
    public void setClt(int clt) {
        this.clt = clt;
    }


    /**
     * Default constructor.
     */
    public Dhcp6CLTOption() {
    }

    /**
     * Constructs a DHCPv6  Client Last Transaction Time option.
     *
     * @param dhcp6Option the DHCPv6 option
     */
    public Dhcp6CLTOption(Dhcp6Option dhcp6Option) {
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
                throw new DeserializationException("Invalid CLT option data");
            }
            Dhcp6CLTOption cltOption = new Dhcp6CLTOption(dhcp6Option);
            byte[] optionData = cltOption.getData();
            ByteBuffer bb = ByteBuffer.wrap(optionData);
            cltOption.clt = bb.getInt();

            return cltOption;
        };
    }

    @Override
    public byte[] serialize() {
        int payloadLen = DEFAULT_LEN;
        int len = Dhcp6Option.DEFAULT_LEN + payloadLen;
        ByteBuffer bb = ByteBuffer.allocate(len);
        bb.putShort(DHCP6.OptionCode.CLIENT_LT.value());
        bb.putShort((short) payloadLen);
        bb.putInt(clt);
        return bb.array();
    }


    @Override
    public String toString() {
        return getToStringHelper()
                .add("clt", clt)
                .toString();
    }


    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), clt);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Dhcp6CLTOption)) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final Dhcp6CLTOption other = (Dhcp6CLTOption) obj;

        return Objects.equals(getCode(), other.getCode()) &&
                Objects.equals(getLength(), other.getLength()) &&
                Objects.equals(clt, other.clt);
    }
}
