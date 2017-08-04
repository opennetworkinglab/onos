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
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.onosproject.lisp.msg.types.LispAfiAddress;
import org.onosproject.lisp.msg.authentication.LispAuthenticationFactory;
import org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum;
import org.onosproject.lisp.msg.exceptions.LispParseError;
import org.onosproject.lisp.msg.exceptions.LispReaderException;
import org.onosproject.lisp.msg.exceptions.LispWriterException;
import org.onosproject.lisp.msg.types.lcaf.LispLcafAddress.LcafAddressReader;
import org.onosproject.lisp.msg.types.lcaf.LispLcafAddress.LcafAddressWriter;
import org.onosproject.lisp.msg.types.lcaf.LispNatLcafAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.lisp.msg.authentication.LispAuthenticationKeyEnum.valueOf;

/**
 * Default LISP info reply message class.
 */
public final class DefaultLispInfoReply extends DefaultLispInfo implements LispInfoReply {

    private static final Logger log = LoggerFactory.getLogger(DefaultLispInfoReply.class);

    private final LispNatLcafAddress natLcafAddress;

    static final InfoReplyWriter WRITER;

    static {
        WRITER = new InfoReplyWriter();
    }

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
     * @param natLcafAddress NAT LCAF address
     */
    DefaultLispInfoReply(boolean infoReply, long nonce, short keyId, short authDataLength,
                                   byte[] authData, int ttl, byte maskLength,
                                   LispAfiAddress eidPrefix, LispNatLcafAddress natLcafAddress) {
        super(infoReply, nonce, keyId, authDataLength, authData, ttl, maskLength, eidPrefix);
        this.natLcafAddress = natLcafAddress;
    }

    @Override
    public LispNatLcafAddress getNatLcafAddress() {
        return natLcafAddress;
    }

    @Override
    public void writeTo(ByteBuf byteBuf) throws LispWriterException {
        WRITER.writeTo(byteBuf, this);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("type", getType())
                .add("nonce", nonce)
                .add("keyId", keyId)
                .add("authentication data length", authDataLength)
                .add("authentication data", authData)
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
                Arrays.equals(authData, that.authData) &&
                Objects.equal(ttl, that.ttl) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(eidPrefix, that.eidPrefix) &&
                Objects.equal(natLcafAddress, that.natLcafAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength, ttl, maskLength,
                eidPrefix, natLcafAddress) + Arrays.hashCode(authData);
    }

    public static final class DefaultInfoReplyBuilder implements InfoReplyBuilder {

        private boolean infoReply;
        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authData;
        private String authKey;
        private int ttl;
        private byte maskLength;
        private LispAfiAddress eidPrefix;
        private LispNatLcafAddress natLcafAddress;

        @Override
        public LispType getType() {
            return LispType.LISP_INFO;
        }


        @Override
        public InfoReplyBuilder withIsInfoReply(boolean infoReply) {
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
        public InfoReplyBuilder withAuthData(byte[] authenticationData) {
            if (authenticationData != null) {
                this.authData = authenticationData;
            }
            return this;
        }

        @Override
        public InfoReplyBuilder withAuthKey(String key) {
            this.authKey = key;
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

            // if authentication data is not specified, we will calculate it
            if (authData == null) {
                LispAuthenticationFactory factory = LispAuthenticationFactory.getInstance();

                authDataLength = LispAuthenticationKeyEnum.valueOf(keyId).getHashLength();
                byte[] tmpAuthData = new byte[authDataLength];
                Arrays.fill(tmpAuthData, (byte) 0);
                authData = tmpAuthData;

                ByteBuf byteBuf = Unpooled.buffer();
                try {
                    new DefaultLispInfoReply(infoReply, nonce, keyId, authDataLength,
                            authData, ttl, maskLength, eidPrefix, natLcafAddress).writeTo(byteBuf);
                } catch (LispWriterException e) {
                    log.warn("Failed to serialize info reply", e);
                }

                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.readBytes(bytes);

                if (authKey == null) {
                    log.warn("Must specify authentication key");
                }

                authData = factory.createAuthenticationData(valueOf(keyId), authKey, bytes);
            }

            return new DefaultLispInfoReply(infoReply, nonce, keyId, authDataLength,
                    authData, ttl, maskLength, eidPrefix, natLcafAddress);
        }
    }

    /**
     * A LISP message reader for InfoReply message.
     */
    public static final class InfoReplyReader implements LispMessageReader<LispInfoReply> {

        @Override
        public LispInfoReply readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {
            LispInfo lispInfo = deserialize(byteBuf);

            LispNatLcafAddress natLcafAddress = (LispNatLcafAddress)
                    new LcafAddressReader().readFrom(byteBuf);

            if (lispInfo != null) {
                return new DefaultInfoReplyBuilder()
                        .withIsInfoReply(lispInfo.isInfoReply())
                        .withNonce(lispInfo.getNonce())
                        .withKeyId(lispInfo.getKeyId())
                        .withAuthDataLength(lispInfo.getAuthDataLength())
                        .withAuthData(lispInfo.getAuthData())
                        .withTtl(lispInfo.getTtl())
                        .withMaskLength(lispInfo.getMaskLength())
                        .withEidPrefix(lispInfo.getPrefix())
                        .withNatLcafAddress(natLcafAddress).build();
            }
            return null;
        }
    }

    public static final class InfoReplyWriter implements LispMessageWriter<LispInfoReply> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispInfoReply message) throws LispWriterException {
            serialize(byteBuf, message);

            new LcafAddressWriter().writeTo(byteBuf, message.getNatLcafAddress());
        }
    }
}
