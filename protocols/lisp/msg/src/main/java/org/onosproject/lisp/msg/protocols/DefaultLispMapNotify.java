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
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;

import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP map notify message class.
 */
public final class DefaultLispMapNotify implements LispMapNotify {

    private final long nonce;
    private final short keyId;
    private final byte[] authenticationData;
    private final byte recordCount;
    private final List<LispMapRecord> mapRecords;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce              nonce
     * @param keyId              key identifier
     * @param authenticationData authentication data
     * @param recordCount        record count number
     * @param mapRecords         a collection of map records
     */
    private DefaultLispMapNotify(long nonce, short keyId, byte[] authenticationData,
                                 byte recordCount, List<LispMapRecord> mapRecords) {
        this.nonce = nonce;
        this.keyId = keyId;
        this.authenticationData = authenticationData;
        this.recordCount = recordCount;
        this.mapRecords = mapRecords;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_NOTIFY;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) {
        // TODO: serialize LispMapRegister message
    }

    @Override
    public Builder createBuilder() {
        return new DefaultNotifyBuilder();
    }

    @Override
    public long getNonce() {
        return this.nonce;
    }

    @Override
    public byte getRecordCount() {
        return this.recordCount;
    }

    @Override
    public short getKeyId() {
        return this.keyId;
    }

    @Override
    public byte[] getAuthenticationData() {
        return ImmutableByteSequence.copyFrom(this.authenticationData).asArray();
    }

    @Override
    public List<LispMapRecord> getLispRecords() {
        return ImmutableList.copyOf(mapRecords);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("recordCount", recordCount)
                .add("keyId", keyId)
                .add("mapRecords", mapRecords).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultLispMapNotify that = (DefaultLispMapNotify) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(recordCount, that.recordCount) &&
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authenticationData, that.authenticationData);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, recordCount, keyId, authenticationData);
    }

    public static final class DefaultNotifyBuilder implements NotifyBuilder {

        private long nonce;
        private short keyId;
        private byte[] authenticationData;
        private byte recordCount;
        private List<LispMapRecord> mapRecords;

        @Override
        public LispType getType() {
            return LispType.LISP_MAP_NOTIFY;
        }

        @Override
        public NotifyBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public NotifyBuilder withRecordCount(byte recordCount) {
            this.recordCount = recordCount;
            return this;
        }

        @Override
        public NotifyBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public NotifyBuilder withAuthenticationData(byte[] authenticationData) {
            this.authenticationData = authenticationData;
            return this;
        }

        @Override
        public NotifyBuilder withMapRecords(List<LispMapRecord> mapRecords) {
            this.mapRecords = ImmutableList.copyOf(mapRecords);
            return this;
        }

        @Override
        public LispMapNotify build() {
            return new DefaultLispMapNotify(nonce, keyId, authenticationData,
                    recordCount, mapRecords);
        }
    }

    /**
     * A private LISP message reader for MapNotify message.
     */
    private static class NotifyReader implements LispMessageReader<LispMapNotify> {

        private static final int RESERVED_SKIP_LENGTH = 3;

        @Override
        public LispMapNotify readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            if (byteBuf.readerIndex() != 0) {
                return null;
            }

            // skip first three bytes as they represent type and reserved fields
            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

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

            return new DefaultNotifyBuilder()
                        .withRecordCount(recordCount)
                        .withNonce(nonce)
                        .withKeyId(keyId)
                        .withAuthenticationData(authData)
                        .withMapRecords(mapRecords)
                        .build();
        }
    }
}
