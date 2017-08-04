/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onlab.packet.DeserializationException;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP signature class.
 */
public final class DefaultLispSignature implements LispSignature {

    private final int recordTtl;
    private final int sigExpiration;
    private final int sigInception;
    private final short keyTag;
    private final short sigLength;
    private final byte sigAlgorithm;
    private final int signature;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param recordTtl     record time-to-live value
     * @param sigExpiration signature expiration
     * @param sigInception  signature inception
     * @param keyTag        key tag
     * @param sigLength     signature length
     * @param sigAlgorithm  signature algorithm
     * @param signature     signature
     */
    private DefaultLispSignature(int recordTtl, int sigExpiration, int sigInception,
                                 short keyTag, short sigLength, byte sigAlgorithm,
                                 int signature) {
        this.recordTtl = recordTtl;
        this.sigExpiration = sigExpiration;
        this.sigInception = sigInception;
        this.keyTag = keyTag;
        this.sigLength = sigLength;
        this.sigAlgorithm = sigAlgorithm;
        this.signature = signature;
    }

    @Override
    public int getRecordTtl() {
        return recordTtl;
    }

    @Override
    public int getSigExpiration() {
        return sigExpiration;
    }

    @Override
    public int getSigInception() {
        return sigInception;
    }

    @Override
    public short getKeyTag() {
        return keyTag;
    }

    @Override
    public short getSigLength() {
        return sigLength;
    }

    @Override
    public byte getSigAlgorithm() {
        return sigAlgorithm;
    }

    @Override
    public int getSignature() {
        return signature;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {

    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("record TTL", recordTtl)
                .add("signature expiration", sigExpiration)
                .add("signature inception", sigInception)
                .add("key tag", keyTag)
                .add("signature length", sigLength)
                .add("signature algorithm", sigAlgorithm)
                .add("signature", signature)
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
        DefaultLispSignature that = (DefaultLispSignature) o;
        return Objects.equal(recordTtl, that.recordTtl) &&
                Objects.equal(sigExpiration, that.sigExpiration) &&
                Objects.equal(sigInception, that.sigInception) &&
                Objects.equal(keyTag, that.keyTag) &&
                Objects.equal(sigLength, that.sigLength) &&
                Objects.equal(sigAlgorithm, that.sigAlgorithm) &&
                Objects.equal(signature, that.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(recordTtl, sigExpiration, sigInception,
                                keyTag, sigLength, sigAlgorithm, signature);
    }

    public static final class DefaultSignatureBuilder implements LispSignature.SignatureBuilder {

        private int recordTtl;
        private int sigExpiration;
        private int sigInception;
        private short keyTag;
        private short sigLength;
        private byte sigAlgorithm;
        private int signature;

        @Override
        public SignatureBuilder withRecordTtl(int recordTtl) {
            this.recordTtl = recordTtl;
            return this;
        }

        @Override
        public SignatureBuilder withSigExpiration(int sigExpiration) {
            this.sigExpiration = sigExpiration;
            return this;
        }

        @Override
        public SignatureBuilder withSigInception(int sigInception) {
            this.sigInception = sigInception;
            return this;
        }

        @Override
        public SignatureBuilder withKeyTag(short keyTag) {
            this.keyTag = keyTag;
            return this;
        }

        @Override
        public SignatureBuilder withSigLength(short sigLength) {
            this.sigLength = sigLength;
            return this;
        }

        @Override
        public SignatureBuilder withSigAlgorithm(byte sigAlgorithm) {
            this.sigAlgorithm = sigAlgorithm;
            return this;
        }

        @Override
        public SignatureBuilder withSignature(int signature) {
            this.signature = signature;
            return this;
        }

        @Override
        public LispSignature build() {

            return new DefaultLispSignature(recordTtl, sigExpiration, sigInception,
                                    keyTag, sigLength, sigAlgorithm, signature);
        }
    }

    /**
     * A LISP reader for Signature section.
     */
    public static final class SignatureReader
                                implements LispMessageReader<LispSignature> {

        private static final int RESERVED_SKIP_LENGTH = 3;

        @Override
        public LispSignature readFrom(ByteBuf byteBuf) throws LispParseError,
                                LispReaderException, DeserializationException {

            // record TTL -> 32 bits
            int recordTtl = byteBuf.readInt();

            // signature expiration -> 32 bits
            int sigExpiration = byteBuf.readInt();

            // signature inception -> 32 bits
            int sigInception = byteBuf.readInt();

            // key tag -> 16 bits
            short keyTag = byteBuf.readShort();

            // signature length -> 16 bits
            short sigLength = byteBuf.readShort();

            // signature algorithm -> 8 bits
            byte sigAlgorithm = byteBuf.readByte();

            byteBuf.skipBytes(RESERVED_SKIP_LENGTH);

            // TODO: the size of signature should be determined by sigAlgorithm
            int signature = byteBuf.readInt();

            return new DefaultSignatureBuilder()
                            .withRecordTtl(recordTtl)
                            .withSigExpiration(sigExpiration)
                            .withSigInception(sigInception)
                            .withKeyTag(keyTag)
                            .withSigLength(sigLength)
                            .withSigAlgorithm(sigAlgorithm)
                            .withSignature(signature)
                            .build();
        }
    }

    /**
     * A LISP writer for Signature section.
     */
    public static final class SignatureWriter implements LispMessageWriter<LispSignature> {

        private static final int UNUSED_ZERO = 0;

        @Override
        public void writeTo(ByteBuf byteBuf, LispSignature message) throws LispWriterException {

            // record TTL
            byteBuf.writeInt(message.getRecordTtl());

            // signature expiration
            byteBuf.writeInt(message.getSigExpiration());

            // signature inception
            byteBuf.writeInt(message.getSigInception());

            // key tag
            byteBuf.writeShort(message.getKeyTag());

            // signature length
            byteBuf.writeShort(message.getSigLength());

            // signature algorithm
            byteBuf.writeByte(message.getSigAlgorithm());

            byteBuf.writeByte(UNUSED_ZERO);
            byteBuf.writeShort(UNUSED_ZERO);

            // signature
            // TODO: the size of signature should be determined by sigAlgorithm
            byteBuf.writeInt(message.getSignature());
        }
    }
}
