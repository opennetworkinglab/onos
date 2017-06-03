/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onlab.packet;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of an DHCPv6 Packet.
 * Base on RFC-3315.
 */
public class DHCP6 extends BasePacket {
    // size of different field of option
    private static final int OPT_CODE_SIZE = 2;
    private static final int OPT_LEN_SIZE = 2;

    // default size of DHCPv6 payload (without options)
    private static final int DHCP6_DEFAULT_SIZE = 4;

    // default size of DHCPv6 relay message payload (without options)
    private static final int DHCP6_RELAY_MSG_SIZE = 34;
    private static final int IPV6_ADDR_LEN = 16;

    // masks & offsets for default DHCPv6 header
    private static final int MSG_TYPE_OFFSET = 24;
    private static final int TRANSACTION_ID_MASK = 0x00ffffff;

    // Relay message types
    private static final Set<Byte> RELAY_MSG_TYPES =
            ImmutableSet.of(MsgType.RELAY_FORW.value,
                            MsgType.RELAY_REPL.value);

    /**
     * DHCPv6 message type.
     */
    public enum MsgType {
        SOLICIT((byte) 1), ADVERTISE((byte) 2), REQUEST((byte) 3),
        CONFIRM((byte) 4), RENEW((byte) 5), REBIND((byte) 6),
        REPLY((byte) 7), RELEASE((byte) 8), DECLINE((byte) 9),
        RECONFIGURE((byte) 10), INFORMATION_REQUEST((byte) 11),
        RELAY_FORW((byte) 12), RELAY_REPL((byte) 13);

        protected byte value;
        MsgType(final byte value) {
            this.value = value;
        }
        public byte value() {
            return this.value;
        }
    }

    /**
     * DHCPv6 option code.
     */
    public enum OptionCode {
        CLIENTID((short) 1), SERVERID((short) 2), IA_NA((short) 3), IA_TA((short) 4),
        IAADDR((short) 5), ORO((short) 6), PREFERENCE((short) 7), ELAPSED_TIME((short) 8),
        RELAY_MSG((short) 9), AUTH((short) 11), UNICAST((short) 12),
        STATUS_CODE((short) 13), RAPID_COMMIT((short) 14), USER_CLASS((short) 15),
        VENDOR_CLASS((short) 16), VENDOR_OPTS((short) 17), INTERFACE_ID((short) 18),
        RECONF_MSG((short) 19), RECONF_ACCEPT((short) 20);

        protected short value;
        OptionCode(final short value) {
            this.value = value;
        }
        public short value() {
            return this.value;
        }
    }

    // general field
    private byte msgType; // 1 byte
    private List<DHCP6Option> options;

    // non-relay field
    private int transactionId; // 3 bytes

    // relay field
    private byte hopCount; // 1 byte
    private byte[] linkAddress; // 16 bytes
    private byte[] peerAddress; // 16 bytes

    /**
     * Creates new DHCPv6 object.
     */
    public DHCP6() {
        options = Lists.newArrayList();
    }

    @Override
    public byte[] serialize() {
        int payloadLength = options.stream()
                .mapToInt(DHCP6Option::getLength)
                .sum();

        // 2 bytes code and 2 bytes length
        payloadLength += options.size() * (OPT_CODE_SIZE + OPT_LEN_SIZE);

        if (RELAY_MSG_TYPES.contains(msgType)) {
            payloadLength += DHCP6_RELAY_MSG_SIZE;
        } else {
            payloadLength += DHCP6_DEFAULT_SIZE;
        }

        ByteBuffer bb = ByteBuffer.allocate(payloadLength);

        if (RELAY_MSG_TYPES.contains(msgType)) {
            bb.put(msgType);
            bb.put(hopCount);
            bb.put(linkAddress);
            bb.put(peerAddress);
        } else {
            int defaultHeader = ((int) msgType) << MSG_TYPE_OFFSET | (transactionId & TRANSACTION_ID_MASK);
            bb.putInt(defaultHeader);
        }

        // serialize options
        options.forEach(option -> {
            bb.putShort(option.getCode());
            bb.putShort(option.getLength());
            bb.put(option.getData());
        });

        return bb.array();
    }

    /**
     * Returns a deserializer for DHCPv6.
     *
     * @return the deserializer for DHCPv6
     */
    public static Deserializer<DHCP6> deserializer() {
        return (data, offset, length) -> {
            DHCP6 dhcp6 = new DHCP6();

            checkNotNull(data);

            if (offset < 0 || length < 0 ||
                    length > data.length || offset >= data.length ||
                    offset + length > data.length) {
                throw new DeserializationException("Illegal offset or length");
            }

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            if (bb.remaining() < DHCP6.DHCP6_DEFAULT_SIZE) {
                throw new DeserializationException(
                        "Buffer underflow while reading DHCPv6 option");
            }

            // peek message type
            dhcp6.msgType = bb.array()[0];
            if (RELAY_MSG_TYPES.contains(dhcp6.msgType)) {
                bb.get(); // drop message type
                dhcp6.hopCount = bb.get();
                dhcp6.linkAddress = new byte[IPV6_ADDR_LEN];
                dhcp6.peerAddress = new byte[IPV6_ADDR_LEN];

                bb.get(dhcp6.linkAddress);
                bb.get(dhcp6.peerAddress);
            } else {
                // get msg type + transaction id (1 + 3 bytes)
                int defaultHeader = bb.getInt();
                dhcp6.transactionId = defaultHeader & TRANSACTION_ID_MASK;
            }

            dhcp6.options = Lists.newArrayList();
            while (bb.remaining() >= OPT_CODE_SIZE) {
                DHCP6Option option = new DHCP6Option();
                short code = bb.getShort();
                if (bb.remaining() < OPT_LEN_SIZE) {
                    throw new DeserializationException(
                            "Buffer underflow while reading DHCPv6 option");
                }

                short optionLen = bb.getShort();
                if (bb.remaining() < optionLen) {
                    throw new DeserializationException(
                            "Buffer underflow while reading DHCPv6 option");
                }

                byte[] optionData = new byte[optionLen];
                bb.get(optionData);

                option.setCode(code);
                option.setLength(optionLen);
                option.setData(optionData);
                dhcp6.options.add(option);
            }

            return dhcp6;
        };
    }

    @Override
    public IPacket deserialize(byte[] data, int offset, int length) {
        try {
            return deserializer().deserialize(data, offset, length);
        } catch (DeserializationException e) {
            return null;
        }
    }

    /**
     * Gets the message type of this DHCPv6 packet.
     *
     * @return the message type
     */
    public byte getMsgType() {
        return msgType;
    }

    /**
     * Gets options from this DHCPv6 packet.
     *
     * @return DHCPv6 options
     */
    public List<DHCP6Option> getOptions() {
        return options;
    }

    /**
     * Gets the transaction ID of this DHCPv6 packet.
     *
     * @return the transaction ID
     */
    public int getTransactionId() {
        return transactionId;
    }

    /**
     * Gets the hop count of this DHCPv6 relay message.
     *
     * @return the hop count
     */
    public byte getHopCount() {
        return hopCount;
    }

    /**
     * Gets the link address of this DHCPv6 relay message.
     *
     * @return the link address
     */
    public byte[] getLinkAddress() {
        return linkAddress;
    }

    /**
     * Gets the peer address of this DHCPv6 relay message.
     *
     * @return the link address
     */
    public byte[] getPeerAddress() {
        return peerAddress;
    }

    /**
     * Sets message type.
     *
     * @param msgType the message type
     */
    public void setMsgType(byte msgType) {
        this.msgType = msgType;
    }

    /**
     * Sets options.
     *
     * @param options the options
     */
    public void setOptions(List<DHCP6Option> options) {
        this.options = options;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Sets hop count.
     *
     * @param hopCount the hop count
     */
    public void setHopCount(byte hopCount) {
        this.hopCount = hopCount;
    }

    /**
     * Sets link address.
     *
     * @param linkAddress the link address
     */
    public void setLinkAddress(byte[] linkAddress) {
        this.linkAddress = linkAddress;
    }

    /**
     * Sets peer address.
     *
     * @param peerAddress the peer address
     */
    public void setPeerAddress(byte[] peerAddress) {
        this.peerAddress = peerAddress;
    }
}
