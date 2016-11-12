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

import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispAfiAddress.AfiAddressWriter;

import java.util.Arrays;

/**
 * A class that contains a set of helper methods for LISP info request and reply.
 */
public class DefaultLispInfo extends AbstractLispMessage implements LispInfo {

    protected final boolean infoReply;
    protected final long nonce;
    protected final short keyId;
    protected final short authDataLength;
    protected final byte[] authData;
    protected final int ttl;
    protected final byte maskLength;
    protected final LispAfiAddress eidPrefix;

    private static final int INFO_REPLY_INDEX = 3;
    private static final int RESERVED_SKIP_LENGTH_1 = 3;
    private static final int RESERVED_SKIP_LENGTH_2 = 1;

    private static final int INFO_REQUEST_SHIFT_BIT = 4;

    private static final int ENABLE_BIT = 1;
    private static final int DISABLE_BIT = 0;

    private static final int UNUSED_ZERO = 0;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param infoReply      info reply flag
     * @param nonce          nonce
     * @param keyId          key identifier
     * @param authDataLength authentication data length
     * @param authData       authentication data
     * @param ttl            Time-To-Live value
     * @param maskLength     EID prefix mask length
     * @param eidPrefix      EID prefix
     */
    protected DefaultLispInfo(boolean infoReply, long nonce, short keyId, short authDataLength,
                              byte[] authData, int ttl, byte maskLength,
                              LispAfiAddress eidPrefix) {
        this.infoReply = infoReply;
        this.nonce = nonce;
        this.keyId = keyId;
        this.authDataLength = authDataLength;
        this.authData = authData;
        this.ttl = ttl;
        this.maskLength = maskLength;
        this.eidPrefix = eidPrefix;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_INFO;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        serialize(byteBuf, this);
    }

    @Override
    public Builder createBuilder() {
        return new DefaultLispInfoRequest.DefaultInfoRequestBuilder();
    }

    @Override
    public boolean isInfoReply() {
        return infoReply;
    }

    @Override
    public long getNonce() {
        return nonce;
    }

    @Override
    public short getKeyId() {
        return keyId;
    }

    @Override
    public short getAuthDataLength() {
        return authDataLength;
    }

    @Override
    public byte[] getAuthData() {
        if (authData != null && authData.length != 0) {
            return ImmutableByteSequence.copyFrom(authData).asArray();
        } else {
            return new byte[0];
        }
    }

    @Override
    public int getTtl() {
        return ttl;
    }

    @Override
    public byte getMaskLength() {
        return maskLength;
    }

    @Override
    public LispAfiAddress getPrefix() {
        return eidPrefix;
    }

    public static LispInfo deserialize(ByteBuf byteBuf) throws LispParseError, LispReaderException {

        if (byteBuf.readerIndex() != 0) {
            return null;
        }

        // infoReply -> 1 bit
        boolean infoReplyFlag = ByteOperator.getBit(byteBuf.readByte(), INFO_REPLY_INDEX);

        // let's skip the reserved field
        byteBuf.skipBytes(RESERVED_SKIP_LENGTH_1);

        // nonce -> 64 bits
        long nonce = byteBuf.readLong();

        // keyId -> 16 bits
        short keyId = byteBuf.readShort();

        // authenticationDataLength -> 16 bits
        short authLength = byteBuf.readShort();

        // authData -> depends on the authenticationDataLength
        byte[] authData = new byte[authLength];
        byteBuf.readBytes(authData);

        // ttl -> 32 bits
        int ttl = byteBuf.readInt();

        // let's skip the reserved field
        byteBuf.skipBytes(RESERVED_SKIP_LENGTH_2);

        // mask length -> 8 bits
        short maskLength = byteBuf.readUnsignedByte();

        LispAfiAddress prefix = new LispAfiAddress.AfiAddressReader().readFrom(byteBuf);

        return new DefaultLispInfo(infoReplyFlag, nonce, keyId, authLength,
                authData, ttl, (byte) maskLength, prefix);
    }

    public static void serialize(ByteBuf byteBuf, LispInfo message) throws LispWriterException {

        // specify LISP message type
        byte msgType = (byte) (LispType.LISP_INFO.getTypeCode() << INFO_REQUEST_SHIFT_BIT);

        // info reply flag
        byte infoReply = DISABLE_BIT;
        if (message.isInfoReply()) {
            infoReply = (byte) (ENABLE_BIT << INFO_REPLY_INDEX);
        }

        byteBuf.writeByte(msgType + infoReply);

        // fill zero into reserved filed
        byteBuf.writeByte((short) UNUSED_ZERO);
        byteBuf.writeByte((short) UNUSED_ZERO);
        byteBuf.writeByte((short) UNUSED_ZERO);

        // nonce
        byteBuf.writeLong(message.getNonce());

        // keyId
        byteBuf.writeShort(message.getKeyId());

        // authentication data length in octet
        byteBuf.writeShort(message.getAuthDataLength());

        // authentication data
        byte[] data = message.getAuthData();
        byte[] clone;
        if (data != null) {
            clone = data.clone();
            Arrays.fill(clone, (byte) UNUSED_ZERO);
        }

        byteBuf.writeBytes(data);

        /// TTL
        byteBuf.writeInt(message.getTtl());

        // fill zero into reserved filed
        byteBuf.writeByte((short) UNUSED_ZERO);

        // mask length
        byteBuf.writeByte(message.getMaskLength());

        // EID prefix AFI with EID prefix
        AfiAddressWriter afiAddressWriter = new AfiAddressWriter();
        afiAddressWriter.writeTo(byteBuf, message.getPrefix());
    }
}
