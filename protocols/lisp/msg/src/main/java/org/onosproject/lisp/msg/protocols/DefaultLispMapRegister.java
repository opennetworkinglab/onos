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

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP map register message class.
 */
public final class DefaultLispMapRegister implements LispMapRegister {

    private final long nonce;
    private final short keyId;
    private final byte[] authenticationData;
    private final byte recordCount;
    private final List<LispMapRecord> mapRecords;
    private final boolean proxyMapReply;
    private final boolean wantMapNotify;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce              nonce
     * @param keyId              key identifier
     * @param authenticationData authentication data
     * @param recordCount        record count number
     * @param mapRecords         a collection of map records
     * @param proxyMapReply      proxy map reply flag
     * @param wantMapNotify      want map notify flag
     */
    private DefaultLispMapRegister(long nonce, short keyId,
                                   byte[] authenticationData, byte recordCount,
                                   List<LispMapRecord> mapRecords,
                                   boolean proxyMapReply, boolean wantMapNotify) {
        this.nonce = nonce;
        this.keyId = keyId;
        this.authenticationData = authenticationData;
        this.recordCount = recordCount;
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
    public byte getRecordCount() {
        return recordCount;
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
    public byte[] getAuthenticationData() {
        return ImmutableByteSequence.copyFrom(this.authenticationData).asArray();
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
                .add("recordCount", recordCount)
                .add("keyId", keyId)
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
                Objects.equal(recordCount, that.recordCount) &&
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authenticationData, that.authenticationData) &&
                Objects.equal(proxyMapReply, that.proxyMapReply) &&
                Objects.equal(wantMapNotify, that.wantMapNotify);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, recordCount, keyId, authenticationData,
                                proxyMapReply, wantMapNotify);
    }

    public static final class DefaultRegisterBuilder implements RegisterBuilder {

        private long nonce;
        private short keyId;
        private byte[] authenticationData;
        private byte recordCount;
        private List<LispMapRecord> mapRecords;
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
        public RegisterBuilder withRecordCount(byte recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        @Override
        public RegisterBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public RegisterBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public RegisterBuilder withAuthenticationData(byte[] authenticationData) {
            this.authenticationData = authenticationData;
            return this;
        }

        @Override
        public RegisterBuilder withMapRecords(List<LispMapRecord> mapRecords) {
            this.mapRecords = ImmutableList.copyOf(mapRecords);
            return this;
        }

        @Override
        public LispMapRegister build() {
            return new DefaultLispMapRegister(nonce, keyId, authenticationData,
                    recordCount, mapRecords, proxyMapReply, wantMapNotify);
        }
    }

    /**
     * A private LISP message reader for MapRegister message.
     */
    private static class RegisterReader implements LispMessageReader<LispMapRegister> {

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
                mapRecords.add(new DefaultLispMapRecord.MapRecordReader().readFrom(byteBuf));
            }

            return new DefaultRegisterBuilder()
                    .withIsProxyMapReply(proxyMapReplyFlag)
                    .withIsWantMapNotify(wantMapNotifyFlag)
                    .withRecordCount(recordCount)
                    .withNonce(nonce)
                    .withKeyId(keyId)
                    .withAuthenticationData(authData)
                    .withMapRecords(mapRecords)
                    .build();
        }
    }
}
