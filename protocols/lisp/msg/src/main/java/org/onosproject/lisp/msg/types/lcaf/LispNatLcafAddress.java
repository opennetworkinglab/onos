/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.types.lcaf;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAddressReader;
import org.onosproject.lisp.msg.types.LispAddressWriter;
import org.onosproject.lisp.msg.types.LispAfiAddress;

import java.util.List;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Network Address Translation (NAT) address class.
 * <p>
 * Instance ID type is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22#page-13
 *
 * <pre>
 * {@literal
 *  0                   1                   2                   3
 *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           AFI = 16387         |     Rsvd1     |     Flags     |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |   Type = 7    |     Rsvd2     |             Length            |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |       MS UDP Port Number      |      ETR UDP Port Number      |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |  Global ETR RLOC Address  ... |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |       MS RLOC Address  ...    |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          | Private ETR RLOC Address  ... |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |      RTR RLOC Address 1 ...   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |              AFI = x          |      RTR RLOC Address k ...   |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public final class LispNatLcafAddress extends LispLcafAddress {

    private final short msUdpPortNumber;
    private final short etrUdpPortNumber;
    private final LispAfiAddress globalEtrRlocAddress;
    private final LispAfiAddress msRlocAddress;
    private final LispAfiAddress privateEtrRlocAddress;
    private final List<LispAfiAddress> rtrRlocAddresses;

    /**
     * Initializes NAT type LCAF address.
     *
     * @param reserved2             reserved2
     * @param length                length
     * @param msUdpPortNumber       Map Server (MS) UDP port number
     * @param etrUdpPortNumber      ETR UDP port number
     * @param globalEtrRlocAddress  global ETR RLOC address
     * @param msRlocAddress         Map Server (MS) RLOC address
     * @param privateEtrRlocAddress private ETR RLOC address
     * @param rtrRlocAddresses      a collection of RTR RLOC addresses
     */
    private LispNatLcafAddress(byte reserved2, short length, short msUdpPortNumber,
                               short etrUdpPortNumber, LispAfiAddress globalEtrRlocAddress,
                               LispAfiAddress msRlocAddress, LispAfiAddress privateEtrRlocAddress,
                               List<LispAfiAddress> rtrRlocAddresses) {
        super(LispCanonicalAddressFormatEnum.NAT, reserved2, length);
        this.msUdpPortNumber = msUdpPortNumber;
        this.etrUdpPortNumber = etrUdpPortNumber;
        this.globalEtrRlocAddress = globalEtrRlocAddress;
        this.msRlocAddress = msRlocAddress;
        this.privateEtrRlocAddress = privateEtrRlocAddress;
        this.rtrRlocAddresses = ImmutableList.copyOf(rtrRlocAddresses);
    }

    /**
     * Obtains Map Server UDP port number.
     *
     * @return Map Server UDP port number
     */
    public short getMsUdpPortNumber() {
        return msUdpPortNumber;
    }

    /**
     * Obtains ETR UDP port number.
     *
     * @return ETR UDP port number
     */
    public short getEtrUdpPortNumber() {
        return etrUdpPortNumber;
    }

    /**
     * Obtains global ETR RLOC address.
     *
     * @return global ETR
     */
    public LispAfiAddress getGlobalEtrRlocAddress() {
        return globalEtrRlocAddress;
    }

    /**
     * Obtains Map Server RLOC address.
     *
     * @return Map Server RLOC address
     */
    public LispAfiAddress getMsRlocAddress() {
        return msRlocAddress;
    }

    /**
     * Obtains private ETR RLOC address.
     *
     * @return private ETR RLOC address
     */
    public LispAfiAddress getPrivateEtrRlocAddress() {
        return privateEtrRlocAddress;
    }

    /**
     * Obtains a collection of RTR RLOC addresses.
     *
     * @return a collection of RTR RLOC addresses
     */
    public List<LispAfiAddress> getRtrRlocAddresses() {
        return ImmutableList.copyOf(rtrRlocAddresses);
    }

    @Override
    public int hashCode() {
        return Objects.hash(msUdpPortNumber, etrUdpPortNumber,
                globalEtrRlocAddress, msRlocAddress, privateEtrRlocAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof LispNatLcafAddress) {
            final LispNatLcafAddress other = (LispNatLcafAddress) obj;
            return Objects.equals(this.msUdpPortNumber, other.msUdpPortNumber) &&
                    Objects.equals(this.etrUdpPortNumber, other.etrUdpPortNumber) &&
                    Objects.equals(this.globalEtrRlocAddress, other.globalEtrRlocAddress) &&
                    Objects.equals(this.msRlocAddress, other.msRlocAddress) &&
                    Objects.equals(this.privateEtrRlocAddress, other.privateEtrRlocAddress) &&
                    Objects.equals(this.rtrRlocAddresses, other.rtrRlocAddresses);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("Map Server UDP port number", msUdpPortNumber)
                .add("ETR UDP port number", etrUdpPortNumber)
                .add("global ETR RLOC address", globalEtrRlocAddress)
                .add("Map Server RLOC address", msRlocAddress)
                .add("private ETR RLOC address", privateEtrRlocAddress)
                .add("RTR RLOC addresses", rtrRlocAddresses)
                .toString();
    }

    public static final class NatAddressBuilder extends LcafAddressBuilder<NatAddressBuilder> {

        private short msUdpPortNumber;
        private short etrUdpPortNumber;
        private LispAfiAddress globalEtrRlocAddress;
        private LispAfiAddress msRlocAddress;
        private LispAfiAddress privateEtrRlocAddress;
        private List<LispAfiAddress> rtrRlocAddresses = Lists.newArrayList();

        /**
         * Sets Map Server UDP port number.
         *
         * @param msUdpPortNumber Map Server UDP port number
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withMsUdpPortNumber(short msUdpPortNumber) {
            this.msUdpPortNumber = msUdpPortNumber;
            return this;
        }

        /**
         * Sets ETR UDP port number.
         *
         * @param etrUdpPortNumber ETR UDP port number
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withEtrUdpPortNumber(short etrUdpPortNumber) {
            this.etrUdpPortNumber = etrUdpPortNumber;
            return this;
        }

        /**
         * Sets global ETR RLOC address.
         *
         * @param globalEtrRlocAddress global ETR RLOC address
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withGlobalEtrRlocAddress(LispAfiAddress globalEtrRlocAddress) {
            this.globalEtrRlocAddress = globalEtrRlocAddress;
            return this;
        }

        /**
         * Sets Map Server RLOC address.
         *
         * @param msRlocAddress Map Server RLOC address
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withMsRlocAddress(LispAfiAddress msRlocAddress) {
            this.msRlocAddress = msRlocAddress;
            return this;
        }

        /**
         * Sets private ETR RLOC address.
         *
         * @param privateEtrRlocAddress private ETR RLOC address
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withPrivateEtrRlocAddress(LispAfiAddress privateEtrRlocAddress) {
            this.privateEtrRlocAddress = privateEtrRlocAddress;
            return this;
        }

        /**
         * Sets RTR RLOC addresses.
         *
         * @param rtrRlocAddresses a collection of RTR RLOC addresses
         * @return NatAddressBuilder object
         */
        public NatAddressBuilder withRtrRlocAddresses(List<LispAfiAddress> rtrRlocAddresses) {
            if (rtrRlocAddresses != null) {
                this.rtrRlocAddresses = ImmutableList.copyOf(rtrRlocAddresses);
            }
            return this;
        }

        public LispNatLcafAddress build() {

            // TODO: need to do null check

            return new LispNatLcafAddress(reserved2, length, msUdpPortNumber,
                    etrUdpPortNumber, globalEtrRlocAddress, msRlocAddress,
                    privateEtrRlocAddress, rtrRlocAddresses);
        }
    }

    /**
     * NAT LCAF address reader class.
     */
    public static class NatLcafAddressReader
            implements LispAddressReader<LispNatLcafAddress> {

        @Override
        public LispNatLcafAddress readFrom(ByteBuf byteBuf)
                throws LispParseError, LispReaderException {

            LispLcafAddress lcafAddress = deserializeCommon(byteBuf);

            short msUdpPortNumber = (short) byteBuf.readUnsignedShort();
            short etrUdpPortNumber = (short) byteBuf.readUnsignedShort();

            LispAfiAddress globalEtrRlocAddress = new AfiAddressReader().readFrom(byteBuf);
            LispAfiAddress msRlocAddress = new AfiAddressReader().readFrom(byteBuf);
            LispAfiAddress privateEtrRlocAddress = new AfiAddressReader().readFrom(byteBuf);

            List<LispAfiAddress> rtrRlocAddresses = Lists.newArrayList();

            while (byteBuf.readerIndex() - COMMON_HEADER_SIZE < lcafAddress.getLength()) {
                rtrRlocAddresses.add(new AfiAddressReader().readFrom(byteBuf));
            }

            return new NatAddressBuilder()
                            .withMsUdpPortNumber(msUdpPortNumber)
                            .withEtrUdpPortNumber(etrUdpPortNumber)
                            .withGlobalEtrRlocAddress(globalEtrRlocAddress)
                            .withMsRlocAddress(msRlocAddress)
                            .withPrivateEtrRlocAddress(privateEtrRlocAddress)
                            .withRtrRlocAddresses(rtrRlocAddresses)
                            .build();
        }
    }

    /**
     * NAT LCAF address writer class.
     */
    public static class NatLcafAddressWriter
            implements LispAddressWriter<LispNatLcafAddress> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispNatLcafAddress address)
                throws LispWriterException {

            int lcafIndex = byteBuf.writerIndex();
            serializeCommon(byteBuf, address);

            byteBuf.writeShort(address.getMsUdpPortNumber());
            byteBuf.writeShort(address.getEtrUdpPortNumber());

            AfiAddressWriter writer = new LispAfiAddress.AfiAddressWriter();
            writer.writeTo(byteBuf, address.getGlobalEtrRlocAddress());
            writer.writeTo(byteBuf, address.getMsRlocAddress());
            writer.writeTo(byteBuf, address.getPrivateEtrRlocAddress());

            List<LispAfiAddress> rtrRlocAddresses = address.getRtrRlocAddresses();

            for (LispAfiAddress rtrRlocAddress : rtrRlocAddresses) {
                writer.writeTo(byteBuf, rtrRlocAddress);
            }

            updateLength(lcafIndex, byteBuf);
        }
    }
}
