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
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.types.LispNatLcafAddress;
import org.onosproject.lisp.msg.types.LispNatLcafAddress.NatLcafAddressWriter;

import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP info reply message class.
 */
public final class DefaultLispInfoReply extends DefaultLispInfo implements LispInfoReply {

    private final LispNatLcafAddress natLcafAddress;

    /**
     * A private constructor that protects object instantiation from external.
     *
     * @param infoReply          info reply flag
     * @param nonce              nonce
     * @param keyId              key identifier
     * @param authDataLength     authentication data length
     * @param authenticationData authentication data
     * @param ttl                Time-To-Live value
     * @param maskLength         EID prefix mask length
     * @param eidPrefix          EID prefix
     * @param natLcafAddress     NAT LCAF address
     */
    protected DefaultLispInfoReply(boolean infoReply, long nonce, short keyId, short authDataLength,
                                 byte[] authenticationData, int ttl, byte maskLength,
                                 LispAfiAddress eidPrefix, LispNatLcafAddress natLcafAddress) {
        super(infoReply, nonce, keyId, authDataLength, authenticationData, ttl, maskLength, eidPrefix);
        this.natLcafAddress = natLcafAddress;
    }

    @Override
    public LispNatLcafAddress getNatLcafAddress() {
        return natLcafAddress;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("keyId", keyId)
                .add("authentication data length", authDataLength)
                .add("authentication data", authenticationData)
                .add("TTL", ttl)
                .add("EID mask length", maskLength)
                .add("EID prefix", eidPrefix)
                .add("NAT LCAF address", natLcafAddress).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultLispInfoReply that = (DefaultLispInfoReply) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authDataLength, that.authDataLength) &&
                Arrays.equals(authenticationData, that.authenticationData) &&
                Objects.equal(ttl, that.ttl) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(eidPrefix, that.eidPrefix) &&
                Objects.equal(natLcafAddress, that.natLcafAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength, ttl, maskLength,
                eidPrefix, natLcafAddress) + Arrays.hashCode(authenticationData);
    }

    public static final class DefaultInfoReplyBuilder implements InfoReplyBuilder {

        private boolean infoReply;
        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authenticationData = new byte[0];
        private int ttl;
        private byte maskLength;
        private LispAfiAddress eidPrefix;
        private LispNatLcafAddress natLcafAddress;

        @Override
        public LispType getType() {
            return LispType.LISP_INFO;
        }


        @Override
        public InfoReplyBuilder withInfoReply(boolean infoReply) {
            this.infoReply = infoReply;
            return this;
        }

        @Override
        public InfoReplyBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public InfoReplyBuilder withAuthDataLength(short authDataLength) {
            this.authDataLength = authDataLength;
            return this;
        }

        @Override
        public InfoReplyBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public InfoReplyBuilder withAuthenticationData(byte[] authenticationData) {
            if (authenticationData != null) {
                this.authenticationData = authenticationData;
            }
            return this;
        }

        @Override
        public InfoReplyBuilder withTtl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        @Override
        public InfoReplyBuilder withMaskLength(byte maskLength) {
            this.maskLength = maskLength;
            return this;
        }

        @Override
        public InfoReplyBuilder withEidPrefix(LispAfiAddress eidPrefix) {
            this.eidPrefix = eidPrefix;
            return this;
        }


        @Override
        public InfoReplyBuilder withNatLcafAddress(LispNatLcafAddress natLcafAddress) {
            this.natLcafAddress = natLcafAddress;
            return this;
        }

        @Override
        public LispInfoReply build() {
            return new DefaultLispInfoReply(infoReply, nonce, keyId, authDataLength,
                    authenticationData, ttl, maskLength, eidPrefix, natLcafAddress);
        }
    }

    /**
     * A LISP message reader for InfoReply message.
     */
    public static final class InfoReplyReader implements LispMessageReader<LispInfoReply> {

        @Override
        public LispInfoReply readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {
            LispInfo lispInfo = DefaultLispInfo.deserialize(byteBuf);
            LispNatLcafAddress natLcafAddress = new LispNatLcafAddress.NatLcafAddressReader().readFrom(byteBuf);

            return new DefaultInfoReplyBuilder()
                    .withInfoReply(lispInfo.hasInfoReply())
                    .withNonce(lispInfo.getNonce())
                    .withKeyId(lispInfo.getKeyId())
                    .withAuthDataLength(lispInfo.getAuthDataLength())
                    .withAuthenticationData(lispInfo.getAuthenticationData())
                    .withTtl(lispInfo.getTtl())
                    .withMaskLength(lispInfo.getMaskLength())
                    .withEidPrefix(lispInfo.getPrefix())
                    .withNatLcafAddress(natLcafAddress).build();
        }
    }

    public static final class InfoReplyWriter implements LispMessageWriter<LispInfoReply> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispInfoReply message) throws LispWriterException {
            DefaultLispInfo.serialize(byteBuf, message);

            // NAT LCAF address
            NatLcafAddressWriter writer = new NatLcafAddressWriter();
            writer.writeTo(byteBuf, message.getNatLcafAddress());
        }
    }
}
