/*
 * Copyright 2016-present Open Networking Foundation
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
import io.netty.buffer.Unpooled;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.authentication.LispAuthenticationFactory;
import org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordReader;
import org.onosproject.lisp.msg.protocols.DefaultLispMapRecord.MapRecordWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.valueOf;

/**
 * Default LISP map notify message class.
 */
public final class DefaultLispMapNotify extends AbstractLispMessage
        implements LispMapNotify {

    private static final Logger log = LoggerFactory.getLogger(DefaultLispMapNotify.class);

    private final long nonce;
    private final short keyId;
    private final short authDataLength;
    private final byte[] authData;
    private final List<LispMapRecord> mapRecords;

    static final NotifyWriter WRITER;

    static {
        WRITER = new NotifyWriter();
    }

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param nonce      nonce
     * @param keyId      key identifier
     * @param authData   authentication data
     * @param mapRecords a collection of map records
     */
    private DefaultLispMapNotify(long nonce, short keyId, short authDataLength,
                                 byte[] authData, List<LispMapRecord> mapRecords) {
        this.nonce = nonce;
        this.keyId = keyId;
        this.authDataLength = authDataLength;
        this.authData = authData;
        this.mapRecords = mapRecords;
    }

    @Override
    public LispType getType() {
        return LispType.LISP_MAP_NOTIFY;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
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
    public byte[] getAuthData() {
        if (authData != null && authData.length != 0) {
            return ImmutableByteSequence.copyFrom(authData).asArray();
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
                .add("authentication data", authData)
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
                Arrays.equals(authData, that.authData);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength) +
                Arrays.hashCode(authData);
    }

    public static final class DefaultNotifyBuilder implements NotifyBuilder {

        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authData;
        private String authKey;
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
        public NotifyBuilder withAuthKey(String key) {
            this.authKey = key;
            return this;
        }

        @Override
        public NotifyBuilder withAuthDataLength(short authDataLength) {
            this.authDataLength = authDataLength;
            return this;
        }

        @Override
        public NotifyBuilder withAuthData(byte[] authData) {
            if (authData != null) {
                this.authData = authData;
            } else {
                this.authData = new byte[0];
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

            // if authentication data is not specified, we will calculate it
            if (authData == null) {
                LispAuthenticationFactory factory = LispAuthenticationFactory.getInstance();

                authDataLength = LispAuthenticationKeyEnum.valueOf(keyId).getHashLength();
                byte[] tmpAuthData = new byte[authDataLength];
                Arrays.fill(tmpAuthData, (byte) 0);
                authData = tmpAuthData;

                ByteBuf byteBuf = Unpooled.buffer();
                try {
                    new DefaultLispMapNotify(nonce, keyId, authDataLength,
                            authData, mapRecords).writeTo(byteBuf);
                } catch (LispWriterException e) {
                    log.warn("Failed to serialize map notify message", e);
                }

                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);

                if (authKey == null) {
                    log.warn("Must specify authentication key");
                }

                authData = factory.createAuthenticationData(valueOf(keyId), authKey, bytes);
            }

            return new DefaultLispMapNotify(nonce, keyId, authDataLength, authData, mapRecords);
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

            // authData -> depends on the authenticationDataLength
            byte[] authData = new byte[authLength];
            byteBuf.readBytes(authData);

            List<LispMapRecord> mapRecords = Lists.newArrayList();
            for (int i = 0; i < recordCount; i++) {
                mapRecords.add(new MapRecordReader().readFrom(byteBuf));
            }

            return new DefaultNotifyBuilder()
                    .withNonce(nonce)
                    .withKeyId(keyId)
                    .withAuthDataLength(authLength)
                    .withAuthData(authData)
                    .withMapRecords(mapRecords)
                    .build();
        }
    }

    /**
     * A LISP message reader for MapNotify message.
     */
    public static final class NotifyWriter implements LispMessageWriter<LispMapNotify> {

        private static final int NOTIFY_SHIFT_BIT = 4;

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispMapNotify message) throws LispWriterException {

            // specify LISP message type
            byte msgType = (byte) (LispType.LISP_MAP_NOTIFY.getTypeCode() << NOTIFY_SHIFT_BIT);
            byteBuf.writeByte(msgType);

            // reserved field
            byteBuf.writeShort((short) UNUSED_ZERO);

            // record count
            byteBuf.writeByte(message.getMapRecords().size());

            // nonce
            byteBuf.writeLong(message.getNonce());

            // keyId
            byteBuf.writeShort(message.getKeyId());

            // authentication data and its length
            if (message.getAuthData() == null) {
                byteBuf.writeShort((short) 0);
            } else {
                byteBuf.writeShort(message.getAuthData().length);
                byteBuf.writeBytes(message.getAuthData());
            }

            // serialize map records
            MapRecordWriter writer = new MapRecordWriter();
            List<LispMapRecord> records = message.getMapRecords();

            for (LispMapRecord record : records) {
                writer.writeTo(byteBuf, record);
            }
        }
    }
}
