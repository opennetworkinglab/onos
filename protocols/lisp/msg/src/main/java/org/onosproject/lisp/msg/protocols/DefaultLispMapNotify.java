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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.onlab.util.ImmutableByteSequence;

import java.util.List;

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
        return null;
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

    public static final class DefaultNotifyBuilder implements NotifyBuilder {

        private long nonce;
        private short keyId;
        private byte[] authenticationData;
        private byte recordCount;
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
        public NotifyBuilder addRecord(LispMapRecord record) {
            this.mapRecords.add(record);
            return this;
        }

        @Override
        public LispMessage build() {
            return new DefaultLispMapNotify(nonce, keyId, authenticationData,
                    recordCount, mapRecords);
        }
    }
}
