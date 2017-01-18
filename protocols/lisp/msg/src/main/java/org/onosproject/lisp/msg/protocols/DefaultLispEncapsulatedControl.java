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

package org.onosproject.lisp.msg.protocols;

import com.google.common.base.Objects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.onlab.packet.Data;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.IP;
import org.onlab.packet.UDP;
import org.onlab.util.ByteOperator;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.protocols.LispType.LISP_ENCAPSULATED_CONTROL;

/**
 * Default LISP Encapsulated Control message class.
 */
public final class DefaultLispEncapsulatedControl extends AbstractLispMessage
        implements LispEncapsulatedControl {

    private final boolean isSecurity;
    private final IP innerIpHeader;
    private final UDP innerUdp;
    private final LispMessage innerMessage;

    static final EcmWriter WRITER;
    static {
        WRITER = new EcmWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param isSecurity a security flag
     * @param innerIpHeader a inner IP Header
     * @param innerUdp a inner UDP Header
     * @param innerMessage a inner LISP control message
     */
    private  DefaultLispEncapsulatedControl(boolean isSecurity, IP innerIpHeader,
                                            UDP innerUdp, LispMessage innerMessage) {
        this.isSecurity = isSecurity;
        this.innerIpHeader = innerIpHeader;
        this.innerUdp = innerUdp;
        this.innerMessage = innerMessage;
    }

    @Override
    public LispType getType() {
        return LISP_ENCAPSULATED_CONTROL;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    @Override
    public Builder createBuilder() {
        return new DefaultEcmBuilder();
    }

    @Override
    public boolean isSecurity() {
        return isSecurity;
    }

    @Override
    public IP innerIpHeader() {
        return innerIpHeader;
    }

    public UDP innerUdp() {
        return innerUdp;
    }

    @Override
    public LispMessage getControlMessage() {
        return innerMessage;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("isSecurity", isSecurity)
                .add("inner IP header", innerIpHeader)
                .add("inner UDP header", innerUdp)
                .add("inner lisp Message", innerMessage)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultLispEncapsulatedControl that = (DefaultLispEncapsulatedControl) o;
        return Objects.equal(isSecurity, that.isSecurity) &&
                Objects.equal(innerIpHeader, that.innerIpHeader) &&
                Objects.equal(innerUdp, that.innerUdp) &&
                Objects.equal(innerMessage, that.innerMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(isSecurity, innerIpHeader, innerUdp, innerMessage);
    }

    /**
     * LISP ECM Builder implementation.
     */
    public static final class DefaultEcmBuilder implements EcmBuilder {

        private boolean isSecurity;
        private IP innerIpHeader;
        private UDP innerUdpHeader;
        private LispMessage innerMessage;

        @Override
        public LispType getType() {
            return LispType.LISP_ENCAPSULATED_CONTROL;
        }

        @Override
        public EcmBuilder isSecurity(boolean security) {
            this.isSecurity = security;
            return this;
        }

        @Override
        public EcmBuilder innerIpHeader(IP innerIpHeader) {
            this.innerIpHeader = innerIpHeader;
            return this;
        }

        @Override
        public EcmBuilder innerUdpHeader(UDP innerUdpHeader) {
            this.innerUdpHeader = innerUdpHeader;
            return this;
        }

        @Override
        public EcmBuilder innerLispMessage(LispMessage msg) {
            this.innerMessage = msg;
            return this;
        }

        @Override
        public LispEncapsulatedControl build() {
            return new DefaultLispEncapsulatedControl(isSecurity, innerIpHeader,
                                                      innerUdpHeader, innerMessage);
        }
    }

    /**
     * A LISP message reader for ECM.
     */
    public static final class EcmReader
            implements LispMessageReader<LispEncapsulatedControl> {

        private static final int SECURITY_INDEX = 3;
        private static final int RESERVED_SKIP_LENGTH = 3;

        @Override
        public LispEncapsulatedControl readFrom(ByteBuf byteBuf) throws
                LispParseError, LispReaderException, DeserializationException {

            if (byteBuf.readerIndex() != 0) {
                return null;
            }

            boolean securityFlag = ByteOperator.getBit(byteBuf.readByte(),
                                                            SECURITY_INDEX);
            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            short totalLength = byteBuf.getShort(byteBuf.readerIndex() + 2);

            byte[] ipHeaderByte = new byte[totalLength];
            byteBuf.getBytes(byteBuf.readerIndex(), ipHeaderByte, 0, totalLength);

            IP innerIpHeader = IP.deserializer().deserialize(ipHeaderByte, 0,
                                                             totalLength);

            UDP innerUdp = (UDP) innerIpHeader.getPayload();
            Data data = (Data) innerUdp.getPayload();
            ByteBuf msgBuffer = Unpooled.buffer();
            msgBuffer.writeBytes(data.getData());

            LispMessageReader reader = LispMessageReaderFactory.getReader(msgBuffer);
            LispMessage innerMessage = (LispMessage) reader.readFrom(msgBuffer);

            return new DefaultLispEncapsulatedControl(securityFlag, innerIpHeader,
                                                      innerUdp, innerMessage);
        }
    }

    /**
     * LISP ECM writer class.
     */
    public static class EcmWriter
            implements LispMessageWriter<LispEncapsulatedControl> {

        private static final short ECM_MSG_CODE =
                LispType.LISP_ENCAPSULATED_CONTROL.getTypeCode();
        private static final int TYPE_SHIFT_BIT = 4;

        private static final int SECURITY_SHIFT_BIT = 3;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispEncapsulatedControl message)
                throws LispWriterException {

            // specify LISP message type
            byte msgType = (byte) (ECM_MSG_CODE << TYPE_SHIFT_BIT);

            byte security = DISABLE_BIT;
            if (message.isSecurity()) {
                security = (byte) (ENABLE_BIT << SECURITY_SHIFT_BIT);
            }

            byteBuf.writeByte(msgType + security);

            // fill zero into reserved field
            byteBuf.writeByte((byte) UNUSED_ZERO);
            byteBuf.writeByte((byte) UNUSED_ZERO);
            byteBuf.writeByte((byte) UNUSED_ZERO);

            ByteBuf buffer = Unpooled.buffer();
            message.getControlMessage().writeTo(buffer);
            byte[] dataBytes = new byte[buffer.writerIndex()];
            buffer.getBytes(0, dataBytes, 0, buffer.writerIndex());

            message.innerUdp().setPayload(new Data(dataBytes));
            message.innerIpHeader().setPayload(message.innerUdp());

            byteBuf.writeBytes(message.innerIpHeader().serialize());
        }
    }

}
