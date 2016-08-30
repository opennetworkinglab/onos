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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.util.ByteOperator;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordReader;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordWriter;


/**
 * Default LISP map register message class.
 */
public final class DefaultLispMapRegister implements LispMapRegister {

    private final long nonce;
    private final short keyId;
    private final short authDataLength;
    private final byte[] authenticationData;
    private final List<LispMapRecord> mapRecords;
    private final boolean proxyMapReply;
    private final boolean wantMapNotify;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce              nonce
     * @param keyId              key identifier
     * @param authenticationData authentication data
     * @param mapRecords         a collection of map records
     * @param proxyMapReply      proxy map reply flag
     * @param wantMapNotify      want map notify flag
     */
    private DefaultLispMapRegister(long nonce, short keyId, short authDataLength,
                                   byte[] authenticationData, List<LispMapRecord> mapRecords,
                                   boolean proxyMapReply, boolean wantMapNotify) {
        this.nonce = nonce;
        this.keyId = keyId;
        this.authDataLength = authDataLength;
        this.authenticationData = authenticationData;
        this.mapRecords = mapRecords;
        this.proxyMapReply = proxyMapReply;
        this.wantMapNotify = wantMapNotify;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_REGISTER;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {
        // TODO: serialize LispMapRegister message
    }

    @Override
    public Builder createBuilder() {
        return new DefaultRegisterBuilder();
    }

    @Override
    public boolean isProxyMapReply() {
        return proxyMapReply;
    }

    @Override
    public boolean isWantMapNotify() {
        return wantMapNotify;
    }

    @Override
    public int getRecordCount() {
        return mapRecords.size();
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
    public byte[] getAuthenticationData() {
        if (authenticationData != null && authenticationData.length != 0) {
            return ImmutableByteSequence.copyFrom(authenticationData).asArray();
        } else {
            return new byte[0];
        }
    }

    @Override
    public List<LispMapRecord> getMapRecords() {
        return ImmutableList.copyOf(mapRecords);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("keyId", keyId)
                .add("authentication data length", authDataLength)
                .add("authentication data", authenticationData)
                .add("mapRecords", mapRecords)
                .add("proxyMapReply", proxyMapReply)
                .add("wantMapNotify", wantMapNotify).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultLispMapRegister that = (DefaultLispMapRegister) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authDataLength, that.authDataLength) &&
                Arrays.equals(authenticationData, that.authenticationData) &&
                Objects.equal(proxyMapReply, that.proxyMapReply) &&
                Objects.equal(wantMapNotify, that.wantMapNotify);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength,
                proxyMapReply, wantMapNotify) + Arrays.hashCode(authenticationData);
    }

    public static final class DefaultRegisterBuilder implements RegisterBuilder {

        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authenticationData = new byte[0];
        private List<LispMapRecord> mapRecords = Lists.newArrayList();
        private boolean proxyMapReply;
        private boolean wantMapNotify;

        @Override
        public LispType getType() {
            return LispType.LISP_MAP_REGISTER;
        }

        @Override
        public RegisterBuilder withIsProxyMapReply(boolean proxyMapReply) {
            this.proxyMapReply = proxyMapReply;
            return this;
        }

        @Override
        public RegisterBuilder withIsWantMapNotify(boolean wantMapNotify) {
            this.wantMapNotify = wantMapNotify;
            return this;
        }

        @Override
        public RegisterBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public RegisterBuilder withAuthDataLength(short authDataLength) {
            this.authDataLength = authDataLength;
            return this;
        }

        @Override
        public RegisterBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public RegisterBuilder withAuthenticationData(byte[] authenticationData) {
            if (authenticationData != null) {
                this.authenticationData = authenticationData;
            }
            return this;
        }

        @Override
        public RegisterBuilder withMapRecords(List<LispMapRecord> mapRecords) {
            if (mapRecords != null) {
                this.mapRecords = ImmutableList.copyOf(mapRecords);
            }
            return this;
        }

        @Override
        public LispMapRegister build() {
            return new DefaultLispMapRegister(nonce, keyId, authDataLength,
                    authenticationData, mapRecords, proxyMapReply, wantMapNotify);
        }
    }

    /**
     * A LISP message reader for MapRegister message.
     */
    public static final class RegisterReader implements LispMessageReader<LispMapRegister> {

        private static final int PROXY_MAP_REPLY_INDEX = 3;
        private static final int WANT_MAP_NOTIFY_INDEX = 0;
        private static final int RESERVED_SKIP_LENGTH = 1;

        @Override
        public LispMapRegister readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            if (byteBuf.readerIndex() != 0) {
                return null;
            }

            // proxyMapReply -> 1 bit
            boolean proxyMapReplyFlag = ByteOperator.getBit(byteBuf.readByte(), PROXY_MAP_REPLY_INDEX);

            // let's skip the reserved field
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            byte reservedWithFlag = byteBuf.readByte();

            // wantMapReply -> 1 bit
            boolean wantMapNotifyFlag = ByteOperator.getBit(reservedWithFlag, WANT_MAP_NOTIFY_INDEX);

            // record count -> 8 bits
            byte recordCount = (byte) byteBuf.readUnsignedByte();

            // nonce -> 64 bits
            long nonce = byteBuf.readLong();

            // keyId -> 16 bits
            short keyId = byteBuf.readShort();

            // authenticationDataLength -> 16 bits
            short authLength = byteBuf.readShort();

            // authenticationData -> depends on the authenticationDataLength
            byte[] authData = new byte[authLength];
            byteBuf.readBytes(authData);

            List<LispMapRecord> mapRecords = Lists.newArrayList();
            for (int i = 0; i < recordCount; i++) {
                mapRecords.add(new MapRecordReader().readFrom(byteBuf));
            }

            return new DefaultRegisterBuilder()
                    .withIsProxyMapReply(proxyMapReplyFlag)
                    .withIsWantMapNotify(wantMapNotifyFlag)
                    .withNonce(nonce)
                    .withKeyId(keyId)
                    .withAuthenticationData(authData)
                    .withMapRecords(mapRecords)
                    .build();
        }
    }

    /**
     * LISP map register message writer class.
     */
    public static class RegisterWriter implements LispMessageWriter<LispMapRegister> {

        private static final int REGISTER_MSG_TYPE = 3;
        private static final int REGISTER_SHIFT_BIT = 4;

        private static final int PROXY_MAP_REPLY_SHIFT_BIT = 3;

        private static final int ENABLE_BIT = 1;
        private static final int DISABLE_BIT = 0;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapRegister message) throws LispWriterException {

            // specify LISP message type
            byte msgType = (byte) (REGISTER_MSG_TYPE << REGISTER_SHIFT_BIT);

            // proxy map reply flag
            byte proxyMapReply = DISABLE_BIT;
            if (message.isProxyMapReply())  {
                proxyMapReply = (byte) (ENABLE_BIT << PROXY_MAP_REPLY_SHIFT_BIT);
            }

            byteBuf.writeByte(msgType + proxyMapReply);

            // fill zero into reserved field
            byteBuf.writeByte((short) UNUSED_ZERO);

            // want map notify flag
            byte wantMapNotify = DISABLE_BIT;
            if (message.isWantMapNotify()) {
                wantMapNotify = (byte) ENABLE_BIT;
            }

            byteBuf.writeByte(wantMapNotify);

            // record count
            byteBuf.writeByte(message.getMapRecords().size());

            // nonce
            byteBuf.writeLong(message.getNonce());

            // keyId
            byteBuf.writeShort(message.getKeyId());

            // authentication data length in octet
            byteBuf.writeShort(message.getAuthDataLength());

            // authentication data
            byte[] data = message.getAuthenticationData();
            byte[] clone;
            if (data != null) {
                clone = data.clone();
                Arrays.fill(clone, (byte) UNUSED_ZERO);
            }

            byteBuf.writeBytes(data);

            // TODO: need to implement MAC authentication mechanism

            // serialize map records
            MapRecordWriter writer = new MapRecordWriter();
            List<LispMapRecord> records = message.getMapRecords();

            for (int i = 0; i < records.size(); i++) {
                writer.writeTo(byteBuf, records.get(i));
            }
        }
    }
}
