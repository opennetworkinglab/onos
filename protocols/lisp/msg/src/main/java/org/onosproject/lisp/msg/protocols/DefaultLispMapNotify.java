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
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordWriter;

/**
 * Default LISP map notify message class.
 */
public final class DefaultLispMapNotify implements LispMapNotify {

    private final long nonce;
    private final short keyId;
    private final short authDataLength;
    private final byte[] authenticationData;
    private final List<LispMapRecord> mapRecords;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce              nonce
     * @param keyId              key identifier
     * @param authenticationData authentication data
     * @param mapRecords         a collection of map records
     */
    private DefaultLispMapNotify(long nonce, short keyId, short authDataLength,
                                 byte[] authenticationData, List<LispMapRecord> mapRecords) {
        this.nonce = nonce;
        this.keyId = keyId;
        this.authDataLength = authDataLength;
        this.authenticationData = authenticationData;
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
        return nonce;
    }

    @Override
    public int getRecordCount() {
        return mapRecords.size();
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
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authDataLength, that.authDataLength) &&
                Arrays.equals(authenticationData, that.authenticationData);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength) +
                Arrays.hashCode(authenticationData);
    }

    public static final class DefaultNotifyBuilder implements NotifyBuilder {

        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authenticationData;
        private List<LispMapRecord> mapRecords = Lists.newArrayList();

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
        public NotifyBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public NotifyBuilder withAuthDataLength(short authDataLength) {
            this.authDataLength = authDataLength;
            return this;
        }

        @Override
        public NotifyBuilder withAuthenticationData(byte[] authenticationData) {
            if (authenticationData != null) {
                this.authenticationData = authenticationData;
            } else {
                this.authenticationData = new byte[0];
            }
            return this;
        }

        @Override
        public NotifyBuilder withMapRecords(List<LispMapRecord> mapRecords) {
            if (mapRecords != null) {
                this.mapRecords = ImmutableList.copyOf(mapRecords);
            }
            return this;
        }

        @Override
        public LispMapNotify build() {

            if (authenticationData == null) {
                authenticationData = new byte[0];
            }

            return new DefaultLispMapNotify(nonce, keyId, authDataLength,
                    authenticationData, mapRecords);
        }
    }

    /**
     * A LISP message reader for MapNotify message.
     */
    public static final class NotifyReader implements LispMessageReader<LispMapNotify> {

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
                        .withNonce(nonce)
                        .withKeyId(keyId)
                        .withAuthDataLength(authLength)
                        .withAuthenticationData(authData)
                        .withMapRecords(mapRecords)
                        .build();
        }
    }

    /**
     * A LISP message reader for MapNotify message.
     */
    public static final class NotifyWriter implements LispMessageWriter<LispMapNotify> {

        private static final int NOTIFY_MSG_TYPE = 4;
        private static final int NOTIFY_SHIFT_BIT = 4;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapNotify message) throws LispWriterException {

            // specify LISP message type
            byte msgType = (byte) (NOTIFY_MSG_TYPE << NOTIFY_SHIFT_BIT);
            byteBuf.writeByte(msgType);

            // reserved field
            byteBuf.writeShort((short) UNUSED_ZERO);

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
