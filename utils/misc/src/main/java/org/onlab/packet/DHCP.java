/*
 * Copyright 2014-present Open Networking Foundation
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

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.dhcp.DhcpOption;
import org.onlab.packet.dhcp.DhcpRelayAgentOption;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onlab.packet.PacketUtils.checkInput;
import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Representation of an DHCP Packet.
 */
public class DHCP extends BasePacket {
    /**
     * Dynamic Host Configuration Protocol packet.
     * ------------------------------------------ |op (1) | htype(1) | hlen(1) |
     * hops(1) | ------------------------------------------ | xid (4) |
     * ------------------------------------------ | secs (2) | flags (2) |
     * ------------------------------------------ | ciaddr (4) |
     * ------------------------------------------ | yiaddr (4) |
     * ------------------------------------------ | siaddr (4) |
     * ------------------------------------------ | giaddr (4) |
     * ------------------------------------------ | chaddr (16) |
     * ------------------------------------------ | sname (64) |
     * ------------------------------------------ | file (128) |
     * ------------------------------------------ | options (312) |
     * ------------------------------------------
     *
     */
    // Header + magic without options
    public static final int MIN_HEADER_LENGTH = 240;
    public static final byte OPCODE_REQUEST = 0x1;
    public static final byte OPCODE_REPLY = 0x2;
    public static final byte HWTYPE_ETHERNET = 0x1;

    private static final Map<Byte, Deserializer<? extends DhcpOption>> OPTION_DESERIALIZERS =
            ImmutableMap.of(DHCPOptionCode.OptionCode_CircuitID.value, DhcpRelayAgentOption.deserializer());
    private static final int UNSIGNED_BYTE_MASK = 0xff;
    private static final int BASE_OPTION_LEN = 60;
    private static final int MIN_DHCP_LEN = 240;
    private static final int BASE_HW_ADDR_LEN = 16;
    private static final byte PAD_BYTE = 0;
    private static final int BASE_SERVER_NAME_LEN = 64;
    private static final int BASE_BOOT_FILE_NAME_LEN = 128;
    private static final int MAGIC_COOKIE = 0x63825363;

    public enum DHCPOptionCode {
        OptionCode_Pad((byte) 0), OptionCode_SubnetMask((byte) 1),
        OptionCode_RouterAddress((byte) 3), OptionCode_DomainServer((byte) 6),
        OptionCode_HostName((byte) 12), OptionCode_DomainName((byte) 15),
        OptionCode_MTU((byte) 26), OptionCode_BroadcastAddress((byte) 28),
        OptionCode_RequestedIP((byte) 50), OptionCode_LeaseTime((byte) 51),
        OptionCode_MessageType((byte) 53), OptionCode_DHCPServerIp((byte) 54),
        OptionCode_RequestedParameters((byte) 55), OptionCode_RenewalTime((byte) 58),
        OPtionCode_RebindingTime((byte) 59), OptionCode_ClientID((byte) 61),
        OptionCode_CircuitID((byte) 82), OptionCode_Classless_Static_Route((byte) 121),
        OptionCode_END((byte) 255);

        protected byte value;

        DHCPOptionCode(final byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
        }
    }

    public enum MsgType {
        // From RFC 1533
        DHCPDISCOVER(1), DHCPOFFER(2), DHCPREQUEST(3), DHCPDECLINE(4), DHCPACK(5),
        DHCPNAK(6), DHCPRELEASE(7),

        // From RFC2132
        DHCPINFORM(8),

        // From RFC3203
        DHCPFORCERENEW(9),

        // From RFC4388
        DHCPLEASEQUERY(10), DHCPLEASEUNASSIGNED(11), DHCPLEASEUNKNOWN(12),
        DHCPLEASEACTIVE(13);

        protected int value;

        MsgType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }

        public static MsgType getType(final int value) {
            switch (value) {
                case 1:
                    return DHCPDISCOVER;
                case 2:
                    return DHCPOFFER;
                case 3:
                    return DHCPREQUEST;
                case 4:
                    return DHCPDECLINE;
                case 5:
                    return DHCPACK;
                case 6:
                    return DHCPNAK;
                case 7:
                    return DHCPRELEASE;
                case 8:
                    return DHCPINFORM;
                case 9:
                    return DHCPFORCERENEW;
                case 10:
                    return DHCPLEASEQUERY;
                case 11:
                    return DHCPLEASEUNASSIGNED;
                case 12:
                    return DHCPLEASEUNKNOWN;
                case 13:
                    return DHCPLEASEACTIVE;
                default:
                    return null;
            }
        }
    }

    protected byte opCode;
    protected byte hardwareType;
    protected byte hardwareAddressLength;
    protected byte hops;
    protected int transactionId;
    protected short seconds;
    protected short flags;
    protected int clientIPAddress;
    protected int yourIPAddress;
    protected int serverIPAddress;
    protected int gatewayIPAddress;
    protected byte[] clientHardwareAddress;
    protected String serverName;
    protected String bootFileName;
    protected List<DhcpOption> options = new ArrayList<DhcpOption>();

    /**
     * @return the opCode
     */
    public byte getOpCode() {
        return this.opCode;
    }

    /**
     * @param opCode
     *            the opCode to set
     * @return this
     */
    public DHCP setOpCode(final byte opCode) {
        this.opCode = opCode;
        return this;
    }

    /**
     * @return the hardwareType
     */
    public byte getHardwareType() {
        return this.hardwareType;
    }

    /**
     * @param hardwareType
     *            the hardwareType to set
     * @return this
     */
    public DHCP setHardwareType(final byte hardwareType) {
        this.hardwareType = hardwareType;
        return this;
    }

    /**
     * @return the hardwareAddressLength
     */
    public byte getHardwareAddressLength() {
        return this.hardwareAddressLength;
    }

    /**
     * @param hardwareAddressLength
     *            the hardwareAddressLength to set
     * @return this
     */
    public DHCP setHardwareAddressLength(final byte hardwareAddressLength) {
        this.hardwareAddressLength = hardwareAddressLength;
        return this;
    }

    /**
     * @return the hops
     */
    public byte getHops() {
        return this.hops;
    }

    /**
     * @param hops
     *            the hops to set
     * @return this
     */
    public DHCP setHops(final byte hops) {
        this.hops = hops;
        return this;
    }

    /**
     * @return the transactionId
     */
    public int getTransactionId() {
        return this.transactionId;
    }

    /**
     * @param transactionId
     *            the transactionId to set
     * @return this
     */
    public DHCP setTransactionId(final int transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    /**
     * @return the seconds
     */
    public short getSeconds() {
        return this.seconds;
    }

    /**
     * @param seconds
     *            the seconds to set
     * @return this
     */
    public DHCP setSeconds(final short seconds) {
        this.seconds = seconds;
        return this;
    }

    /**
     * @return the flags
     */
    public short getFlags() {
        return this.flags;
    }

    /**
     * @param flags
     *            the flags to set
     * @return this
     */
    public DHCP setFlags(final short flags) {
        this.flags = flags;
        return this;
    }

    /**
     * @return the clientIPAddress
     */
    public int getClientIPAddress() {
        return this.clientIPAddress;
    }

    /**
     * @param clientIPAddress
     *            the clientIPAddress to set
     * @return this
     */
    public DHCP setClientIPAddress(final int clientIPAddress) {
        this.clientIPAddress = clientIPAddress;
        return this;
    }

    /**
     * @return the yourIPAddress
     */
    public int getYourIPAddress() {
        return this.yourIPAddress;
    }

    /**
     * @param yourIPAddress
     *            the yourIPAddress to set
     * @return this
     */
    public DHCP setYourIPAddress(final int yourIPAddress) {
        this.yourIPAddress = yourIPAddress;
        return this;
    }

    /**
     * @return the serverIPAddress
     */
    public int getServerIPAddress() {
        return this.serverIPAddress;
    }

    /**
     * @param serverIPAddress
     *            the serverIPAddress to set
     * @return this
     */
    public DHCP setServerIPAddress(final int serverIPAddress) {
        this.serverIPAddress = serverIPAddress;
        return this;
    }

    /**
     * @return the gatewayIPAddress
     */
    public int getGatewayIPAddress() {
        return this.gatewayIPAddress;
    }

    /**
     * @param gatewayIPAddress
     *            the gatewayIPAddress to set
     * @return this
     */
    public DHCP setGatewayIPAddress(final int gatewayIPAddress) {
        this.gatewayIPAddress = gatewayIPAddress;
        return this;
    }

    /**
     * @return the clientHardwareAddress
     */
    public byte[] getClientHardwareAddress() {
        return this.clientHardwareAddress;
    }

    /**
     * @param clientHardwareAddress
     *            the clientHardwareAddress to set
     * @return this
     */
    public DHCP setClientHardwareAddress(final byte[] clientHardwareAddress) {
        this.clientHardwareAddress = clientHardwareAddress;
        return this;
    }

    /**
     * Gets a specific DHCP option parameter.
     *
     * @param optionCode
     *            The option code to get
     * @return The value of the option if it exists, null otherwise
     */
    public DhcpOption getOption(final DHCPOptionCode optionCode) {
        for (final DhcpOption opt : this.options) {
            if (opt.getCode() == optionCode.getValue()) {
                return opt;
            }
        }
        return null;
    }

    /**
     * @return the options
     */
    public List<DhcpOption> getOptions() {
        return this.options;
    }

    /**
     * @param options
     *            the options to set
     * @return this
     */
    public DHCP setOptions(final List<DhcpOption> options) {
        this.options = options;
        return this;
    }

    /**
     * @return the packetType base on option 53
     */
    public MsgType getPacketType() {
        return this.options.parallelStream()
                .filter(op -> op.getCode() == DHCPOptionCode.OptionCode_MessageType.getValue())
                .map(DhcpOption::getData)
                .filter(data -> data.length != 0)
                .map(data -> data[0])
                .map(MsgType::getType)
                .findFirst()
                .orElse(null);
    }

    /**
     * @return the serverName
     */
    public String getServerName() {
        return this.serverName;
    }

    /**
     * @param server
     *            the serverName to set
     * @return this
     */
    public DHCP setServerName(final String server) {
        this.serverName = server;
        return this;
    }

    /**
     * @return the bootFileName
     */
    public String getBootFileName() {
        return this.bootFileName;
    }

    /**
     * @param bootFile
     *            the bootFileName to set
     * @return this
     */
    public DHCP setBootFileName(final String bootFile) {
        this.bootFileName = bootFile;
        return this;
    }

    @Override
    public byte[] serialize() {
        // not guaranteed to retain length/exact format
        this.resetChecksum();

        // minimum size 240 including magic cookie, options generally padded to
        // 300
        int optionsLength = 0;
        for (final DhcpOption option : this.options) {
            if (option.getCode() == DHCPOptionCode.OptionCode_Pad.getValue() ||
                    option.getCode() == DHCPOptionCode.OptionCode_END.getValue()) {
                optionsLength += 1;
            } else {
                optionsLength += 2 + (UNSIGNED_BYTE_MASK & option.getLength());
            }
        }
        int optionsPadLength = 0;
        if (optionsLength < BASE_OPTION_LEN) {
            optionsPadLength = BASE_OPTION_LEN - optionsLength;
        }

        final byte[] data = new byte[MIN_DHCP_LEN + optionsLength + optionsPadLength];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put(this.opCode);
        bb.put(this.hardwareType);
        bb.put(this.hardwareAddressLength);
        bb.put(this.hops);
        bb.putInt(this.transactionId);
        bb.putShort(this.seconds);
        bb.putShort(this.flags);
        bb.putInt(this.clientIPAddress);
        bb.putInt(this.yourIPAddress);
        bb.putInt(this.serverIPAddress);
        bb.putInt(this.gatewayIPAddress);
        checkArgument(this.clientHardwareAddress.length <= BASE_HW_ADDR_LEN,
                "Hardware address is too long (%s bytes)", this.clientHardwareAddress.length);
        bb.put(this.clientHardwareAddress);
        if (this.clientHardwareAddress.length < BASE_HW_ADDR_LEN) {
            for (int i = 0; i < BASE_HW_ADDR_LEN - this.clientHardwareAddress.length; ++i) {
                bb.put(PAD_BYTE);
            }
        }
        this.writeString(this.serverName, bb, BASE_SERVER_NAME_LEN);
        this.writeString(this.bootFileName, bb, BASE_BOOT_FILE_NAME_LEN);
        // magic cookie
        bb.putInt(MAGIC_COOKIE);
        for (final DhcpOption option : this.options) {
            bb.put(option.serialize());
        }
        // assume the rest is padded out with zeroes
        return data;
    }

    protected void writeString(final String string, final ByteBuffer bb,
            final int maxLength) {
        if (string == null) {
            for (int i = 0; i < maxLength; ++i) {
                bb.put(PAD_BYTE);
            }
        } else {
            byte[] bytes;
            bytes = string.getBytes(StandardCharsets.US_ASCII);
            int writeLength = bytes.length;
            if (writeLength > maxLength) {
                writeLength = maxLength;
            }
            bb.put(bytes, 0, writeLength);
            for (int i = writeLength; i < maxLength; ++i) {
                bb.put((byte) 0x0);
            }
        }
    }

    private static String readString(final ByteBuffer bb, final int maxLength) {
        final byte[] bytes = new byte[maxLength];
        bb.get(bytes);
        String result;
        result = new String(bytes, StandardCharsets.US_ASCII).trim();
        return result;
    }

    /**
     * Deserializer function for DHCP packets.
     *
     * @return deserializer function
     */
    public static Deserializer<DHCP> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, MIN_HEADER_LENGTH);

            ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            DHCP dhcp = new DHCP();

            dhcp.opCode = bb.get();
            dhcp.hardwareType = bb.get();
            dhcp.hardwareAddressLength = bb.get();
            dhcp.hops = bb.get();
            dhcp.transactionId = bb.getInt();
            dhcp.seconds = bb.getShort();
            dhcp.flags = bb.getShort();
            dhcp.clientIPAddress = bb.getInt();
            dhcp.yourIPAddress = bb.getInt();
            dhcp.serverIPAddress = bb.getInt();
            dhcp.gatewayIPAddress = bb.getInt();
            final int hardwareAddressLength = UNSIGNED_BYTE_MASK & dhcp.hardwareAddressLength;
            dhcp.clientHardwareAddress = new byte[hardwareAddressLength];

            bb.get(dhcp.clientHardwareAddress);
            for (int i = hardwareAddressLength; i < BASE_HW_ADDR_LEN; ++i) {
                bb.get();
            }
            dhcp.serverName = readString(bb, BASE_SERVER_NAME_LEN);
            dhcp.bootFileName = readString(bb, BASE_BOOT_FILE_NAME_LEN);
            // read the magic cookie
            // magic cookie
            bb.getInt();

            // read options
            boolean foundEndOptionsMarker = false;
            while (bb.hasRemaining()) {
                DhcpOption option;
                int pos = bb.position();
                int optCode = UNSIGNED_BYTE_MASK & bb.array()[pos]; // to unsigned integer
                int optLen;
                byte[] optData;

                if (optCode == DHCPOptionCode.OptionCode_Pad.value) {
                    // pad, skip
                    // read option code
                    bb.get();
                    continue;
                }
                if (optCode == (UNSIGNED_BYTE_MASK & DHCPOptionCode.OptionCode_END.value)) {
                    // end of dhcp options or invalid option and set the END option
                    option = new DhcpOption();
                    option.setCode((byte) optCode);
                    dhcp.options.add(option);
                    foundEndOptionsMarker = true;
                    break;
                }

                if (bb.remaining() < 2) {
                    // No option length
                    throw new DeserializationException("Buffer underflow while reading DHCP option");
                }

                optLen = UNSIGNED_BYTE_MASK & bb.array()[pos + 1];
                if (bb.remaining() < DhcpOption.DEFAULT_LEN + optLen) {
                    // Invalid option length
                    throw new DeserializationException("Buffer underflow while reading DHCP option");
                }
                optData = new byte[DhcpOption.DEFAULT_LEN + optLen];
                bb.get(optData);
                if (OPTION_DESERIALIZERS.containsKey((byte) optCode)) {
                    option = OPTION_DESERIALIZERS.get((byte) optCode).deserialize(optData, 0, optData.length);
                    dhcp.options.add(option);
                } else {
                    // default option
                    option = DhcpOption.deserializer().deserialize(optData, 0, optData.length);
                    dhcp.options.add(option);
                }
            }

            if (!foundEndOptionsMarker) {
                throw new DeserializationException("DHCP End options marker was missing");
            }

            return dhcp;
        };
    }

    @Override
    public String toString() {
        return toStringHelper(getClass())
                .add("opCode", Byte.toString(opCode))
                .add("hardwareType", Byte.toString(hardwareType))
                .add("hardwareAddressLength", Byte.toString(hardwareAddressLength))
                .add("hops", Byte.toString(hops))
                .add("transactionId", Integer.toString(transactionId))
                .add("seconds", Short.toString(seconds))
                .add("flags", Short.toString(flags))
                .add("clientIPAddress", Integer.toString(clientIPAddress))
                .add("yourIPAddress", Integer.toString(yourIPAddress))
                .add("serverIPAddress", Integer.toString(serverIPAddress))
                .add("gatewayIPAddress", Integer.toString(gatewayIPAddress))
                .add("clientHardwareAddress", Arrays.toString(clientHardwareAddress))
                .add("serverName", serverName)
                .add("bootFileName", bootFileName)
                .toString();
        // TODO: need to handle options
    }
}
