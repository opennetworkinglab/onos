/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;

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

    public enum DHCPOptionCode {
        OptionCode_SubnetMask((byte) 1), OptionCode_RouterAddress((byte) 3), OptionCode_DomainServer((byte) 6),
        OptionCode_HostName((byte) 12), OptionCode_DomainName((byte) 15), OptionCode_BroadcastAddress((byte) 28),
        OptionCode_RequestedIP((byte) 50), OptionCode_LeaseTime((byte) 51), OptionCode_MessageType((byte) 53),
        OptionCode_DHCPServerIp((byte) 54), OptionCode_RequestedParameters((byte) 55),
        OptionCode_RenewalTime((byte) 58), OPtionCode_RebindingTime((byte) 59), OptionCode_ClientID((byte) 61),
        OptionCode_CircuitID((byte) 82), OptionCode_END((byte) 255);

        protected byte value;

        private DHCPOptionCode(final byte value) {
            this.value = value;
        }

        public byte getValue() {
            return this.value;
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
    protected List<DHCPOption> options = new ArrayList<DHCPOption>();

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
    public DHCPOption getOption(final DHCPOptionCode optionCode) {
        for (final DHCPOption opt : this.options) {
            if (opt.code == optionCode.value) {
                return opt;
            }
        }
        return null;
    }

    /**
     * @return the options
     */
    public List<DHCPOption> getOptions() {
        return this.options;
    }

    /**
     * @param options
     *            the options to set
     * @return this
     */
    public DHCP setOptions(final List<DHCPOption> options) {
        this.options = options;
        return this;
    }

    /**
     * @return the packetType base on option 53
     */
    public DHCPPacketType getPacketType() {
        final ListIterator<DHCPOption> lit = this.options.listIterator();
        while (lit.hasNext()) {
            final DHCPOption option = lit.next();
            // only care option 53
            if (option.getCode() == 53) {
                return DHCPPacketType.getType(option.getData()[0]);
            }
        }
        return null;
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
        for (final DHCPOption option : this.options) {
            if (option.getCode() == 0 || option.getCode() == ((byte) 255)) {
                optionsLength += 1;
            } else {
                optionsLength += 2 + (0xff & option.getLength());
            }
        }
        int optionsPadLength = 0;
        if (optionsLength < 60) {
            optionsPadLength = 60 - optionsLength;
        }

        final byte[] data = new byte[240 + optionsLength + optionsPadLength];
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
        checkArgument(this.clientHardwareAddress.length <= 16,
                "Hardware address is too long (%s bytes)", this.clientHardwareAddress.length);
        bb.put(this.clientHardwareAddress);
        if (this.clientHardwareAddress.length < 16) {
            for (int i = 0; i < 16 - this.clientHardwareAddress.length; ++i) {
                bb.put((byte) 0x0);
            }
        }
        this.writeString(this.serverName, bb, 64);
        this.writeString(this.bootFileName, bb, 128);
        // magic cookie
        bb.put((byte) 0x63);
        bb.put((byte) 0x82);
        bb.put((byte) 0x53);
        bb.put((byte) 0x63);
        for (final DHCPOption option : this.options) {
            dhcpOptionToByteArray(option, bb);
        }
        // assume the rest is padded out with zeroes
        return data;
    }

    public static ByteBuffer dhcpOptionToByteArray(DHCPOption option, ByteBuffer bb) {
        final int code = option.getCode() & 0xff;
        bb.put((byte) code);
        if (code != 0 && code != 255) {
            bb.put(option.getLength());
            bb.put(option.getData());
        }
        return bb;
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        if (bb.remaining() < DHCP.MIN_HEADER_LENGTH) {
            return this;
        }

        this.opCode = bb.get();
        this.hardwareType = bb.get();
        this.hardwareAddressLength = bb.get();
        this.hops = bb.get();
        this.transactionId = bb.getInt();
        this.seconds = bb.getShort();
        this.flags = bb.getShort();
        this.clientIPAddress = bb.getInt();
        this.yourIPAddress = bb.getInt();
        this.serverIPAddress = bb.getInt();
        this.gatewayIPAddress = bb.getInt();
        final int hardwareAddressLength = 0xff & this.hardwareAddressLength;
        this.clientHardwareAddress = new byte[hardwareAddressLength];

        bb.get(this.clientHardwareAddress);
        for (int i = hardwareAddressLength; i < 16; ++i) {
            bb.get();
        }
        this.serverName = this.readString(bb, 64);
        this.bootFileName = this.readString(bb, 128);
        // read the magic cookie
        // magic cookie
        bb.get();
        bb.get();
        bb.get();
        bb.get();
        // read options
        while (bb.hasRemaining()) {
            final DHCPOption option = new DHCPOption();
            int code = 0xff & bb.get(); // convert signed byte to int in range
            // [0,255]
            option.setCode((byte) code);
            if (code == 0) {
                // skip these
                continue;
            } else if (code != 255) {
                if (bb.hasRemaining()) {
                    final int l = 0xff & bb.get(); // convert signed byte to
                    // int in range [0,255]
                    option.setLength((byte) l);
                    if (bb.remaining() >= l) {
                        final byte[] optionData = new byte[l];
                        bb.get(optionData);
                        option.setData(optionData);
                    } else {
                        // Skip the invalid option and set the END option
                        code = 0xff;
                        option.setCode((byte) code);
                        option.setLength((byte) 0);
                    }
                } else {
                    // Skip the invalid option and set the END option
                    code = 0xff;
                    option.setCode((byte) code);
                    option.setLength((byte) 0);
                }
            }
            this.options.add(option);
            if (code == 255) {
                // remaining bytes are supposed to be 0, but ignore them just in
                // case
                break;
            }
        }

        return this;
    }

    protected void writeString(final String string, final ByteBuffer bb,
            final int maxLength) {
        if (string == null) {
            for (int i = 0; i < maxLength; ++i) {
                bb.put((byte) 0x0);
            }
        } else {
            byte[] bytes = null;
            try {
                bytes = string.getBytes("ascii");
            } catch (final UnsupportedEncodingException e) {
                throw new RuntimeException("Failure encoding server name", e);
            }
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
        String result = null;
        try {
            result = new String(bytes, "ascii").trim();
        } catch (final UnsupportedEncodingException e) {
            throw new RuntimeException("Failure decoding string", e);
        }
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
            final int hardwareAddressLength = 0xff & dhcp.hardwareAddressLength;
            dhcp.clientHardwareAddress = new byte[hardwareAddressLength];

            bb.get(dhcp.clientHardwareAddress);
            for (int i = hardwareAddressLength; i < 16; ++i) {
                bb.get();
            }
            dhcp.serverName = readString(bb, 64);
            dhcp.bootFileName = readString(bb, 128);
            // read the magic cookie
            // magic cookie
            bb.get();
            bb.get();
            bb.get();
            bb.get();

            // read options
            boolean foundEndOptionsMarker = false;
            while (bb.hasRemaining()) {
                final DHCPOption option = new DHCPOption();
                int code = 0xff & bb.get(); // convert signed byte to int in range
                // [0,255]
                option.setCode((byte) code);
                if (code == 0) {
                    // skip these
                    continue;
                } else if (code != 255) {
                    if (bb.hasRemaining()) {
                        final int l = 0xff & bb.get(); // convert signed byte to
                        // int in range [0,255]
                        option.setLength((byte) l);
                        if (bb.remaining() >= l) {
                            final byte[] optionData = new byte[l];
                            bb.get(optionData);
                            option.setData(optionData);
                            dhcp.options.add(option);
                        } else {
                            throw new DeserializationException(
                                    "Buffer underflow while reading DHCP option");
                        }
                    }
                } else if (code == 255) {
                    DHCPOption end = new DHCPOption();
                    end.setCode((byte) 255);
                    dhcp.options.add(end);
                    // remaining bytes are supposed to be 0, but ignore them just in
                    // case
                    foundEndOptionsMarker = true;
                    break;
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
