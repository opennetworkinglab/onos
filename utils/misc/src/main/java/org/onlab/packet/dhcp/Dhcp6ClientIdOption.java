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
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Deserializer;

import java.nio.ByteBuffer;

/**
 * DHCPv6 Client Identifier Option.
 */
public final class Dhcp6ClientIdOption extends Dhcp6Option {
    @Override
    public short getCode() {
        return DHCP6.OptionCode.CLIENTID.value();
    }

    @Override
    public short getLength() {
        return (short) payload.serialize().length;
    }

    @Override
    public byte[] getData() {
        return payload.serialize();
    }

    @Override
    public void setData(byte[] data) {
        try {
            Dhcp6Duid duid = Dhcp6Duid.deserializer().deserialize(data, 0, data.length);
            this.setDuid(duid);
        } catch (DeserializationException e) {
            throw new IllegalArgumentException("Invalid DUID");
        }

    }

    public Dhcp6Duid getDuid() {
        return (Dhcp6Duid) payload;
    }

    public void setDuid(Dhcp6Duid duid) {
        this.setPayload(duid);
        duid.setParent(this);
    }

    public static Deserializer<Dhcp6Option> deserializer() {
        return (data, offset, length) -> {
            Dhcp6Option dhcp6Option =
                    Dhcp6Option.deserializer().deserialize(data, offset, length);
            Dhcp6ClientIdOption clientIdentifier = new Dhcp6ClientIdOption();

            if (dhcp6Option.getLength() < DEFAULT_LEN) {
                throw new DeserializationException("Invalid length of Client Id option");
            }

            Dhcp6Duid duid =
                    Dhcp6Duid.deserializer().deserialize(dhcp6Option.getData(), 0, dhcp6Option.getLength());
            clientIdentifier.setPayload(duid);
            return clientIdentifier;
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
                .add("duid", getDuid().toString())
                .toString();
    }
}
