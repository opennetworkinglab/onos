/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onlab.packet.lacp;

import org.onlab.packet.Deserializer;
import org.onlab.packet.MacAddress;

import java.nio.ByteBuffer;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onlab.packet.PacketUtils.checkInput;

/**
 * Represents LACP ActorInfo or PartnerInfo information.
 */
public class LacpBaseTlv extends LacpTlv {
    public static final byte LENGTH = 20;
    private static final byte[] RESERVED = new byte[3];

    private short systemPriority;
    private MacAddress systemMac;
    private short key;
    private short portPriority;
    private short port;
    private LacpState state;

    /**
     * Gets system priority.
     *
     * @return system priority
     */
    public short getSystemPriority() {
        return systemPriority;
    }

    /**
     * Sets system priority.
     *
     * @param systemPriority system priority
     * @return this
     */
    public LacpBaseTlv setSystemPriority(short systemPriority) {
        this.systemPriority = systemPriority;
        return this;
    }

    /**
     * Gets system MAC address.
     *
     * @return system MAC address
     */
    public MacAddress getSystemMac() {
        return systemMac;
    }

    /**
     * Sets system MAC address.
     *
     * @param systemMac system MAC
     * @return this
     */
    public LacpBaseTlv setSystemMac(MacAddress systemMac) {
        this.systemMac = systemMac;
        return this;
    }

    /**
     * Gets key.
     *
     * @return key
     */
    public short getKey() {
        return key;
    }

    /**
     * Sets key.
     *
     * @param key key
     * @return this
     */
    public LacpBaseTlv setKey(short key) {
        this.key = key;
        return this;
    }

    /**
     * Gets port priority.
     *
     * @return port priority
     */
    public short getPortPriority() {
        return portPriority;
    }

    /**
     * Sets port priority.
     *
     * @param portPriority port priority
     * @return this
     */
    public LacpBaseTlv setPortPriority(short portPriority) {
        this.portPriority = portPriority;
        return this;
    }

    /**
     * Gets port.
     *
     * @return port
     */
    public short getPort() {
        return port;
    }

    /**
     * Sets port.
     *
     * @param port port
     * @return this
     */
    public LacpBaseTlv setPort(short port) {
        this.port = port;
        return this;
    }

    /**
     * Gets state.
     *
     * @return state
     */
    public LacpState getState() {
        return state;
    }

    /**
     * Sets state.
     *
     * @param state state
     * @return this
     */
    public LacpBaseTlv setState(byte state) {
        this.state = new LacpState(state);
        return this;
    }

    /**
     * Deserializer function for LacpBaseTlv packets.
     *
     * @return deserializer function
     */
    public static Deserializer<LacpBaseTlv> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, LENGTH - HEADER_LENGTH);

            LacpBaseTlv lacpBaseTlv = new LacpBaseTlv();
            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);

            lacpBaseTlv.setSystemPriority(bb.getShort());
            byte[] mac = new byte[6];
            bb.get(mac);
            lacpBaseTlv.setSystemMac(MacAddress.valueOf(mac));
            lacpBaseTlv.setKey(bb.getShort());
            lacpBaseTlv.setPortPriority(bb.getShort());
            lacpBaseTlv.setPort(bb.getShort());
            lacpBaseTlv.setState(bb.get());

            return lacpBaseTlv;
        };
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[LENGTH - HEADER_LENGTH];

        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.putShort(this.systemPriority);
        bb.put(this.systemMac.toBytes());
        bb.putShort(this.key);
        bb.putShort(this.portPriority);
        bb.putShort(this.port);
        bb.put(this.state.toByte());
        bb.put(RESERVED);

        return data;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!super.equals(obj)) {
            return false;
        }
        if (!(obj instanceof LacpBaseTlv)) {
            return false;
        }
        final LacpBaseTlv other = (LacpBaseTlv) obj;
        return systemPriority == other.systemPriority &&
                key == other.key &&
                portPriority == other.portPriority &&
                port == other.port &&
                Objects.equals(state, other.state) &&
                Objects.equals(systemMac, other.systemMac);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), systemPriority, systemMac, key,
                portPriority, port, state);
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("systemPriority", Short.toString(systemPriority))
                .add("systemMac", systemMac.toString())
                .add("key", Short.toString(key))
                .add("portPriority", Short.toString(portPriority))
                .add("port", Short.toString(port))
                .add("state", state.toString())
                .toString();
    }
}
