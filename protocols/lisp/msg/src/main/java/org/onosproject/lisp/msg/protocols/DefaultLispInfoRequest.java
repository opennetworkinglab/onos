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

import java.util.Arrays;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default LISP info request message class.
 */
public class DefaultLispInfoRequest extends DefaultLispInfo implements LispInfoRequest {

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
     */
    protected DefaultLispInfoRequest(boolean infoReply, long nonce, short keyId, short authDataLength,
                                     byte[] authenticationData, int ttl, byte maskLength,
                                     LispAfiAddress eidPrefix) {
        super(infoReply, nonce, keyId, authDataLength, authenticationData, ttl, maskLength, eidPrefix);
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
                .add("EID prefix", eidPrefix).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DefaultLispInfoRequest that = (DefaultLispInfoRequest) o;
        return Objects.equal(nonce, that.nonce) &&
                Objects.equal(keyId, that.keyId) &&
                Objects.equal(authDataLength, that.authDataLength) &&
                Arrays.equals(authenticationData, that.authenticationData) &&
                Objects.equal(ttl, that.ttl) &&
                Objects.equal(maskLength, that.maskLength) &&
                Objects.equal(eidPrefix, that.eidPrefix);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(nonce, keyId, authDataLength, ttl, maskLength,
                eidPrefix) + Arrays.hashCode(authenticationData);
    }

    public static final class DefaultInfoRequestBuilder implements InfoRequestBuilder {

        private boolean infoReply;
        private long nonce;
        private short keyId;
        private short authDataLength;
        private byte[] authenticationData = new byte[0];
        private int ttl;
        private byte maskLength;
        private LispAfiAddress eidPrefix;

        @Override
        public LispType getType() {
            return LispType.LISP_INFO;
        }


        @Override
        public InfoRequestBuilder withInfoReply(boolean infoReply) {
            this.infoReply = infoReply;
            return this;
        }

        @Override
        public InfoRequestBuilder withNonce(long nonce) {
            this.nonce = nonce;
            return this;
        }

        @Override
        public InfoRequestBuilder withAuthDataLength(short authDataLength) {
            this.authDataLength = authDataLength;
            return this;
        }

        @Override
        public InfoRequestBuilder withKeyId(short keyId) {
            this.keyId = keyId;
            return this;
        }

        @Override
        public InfoRequestBuilder withAuthenticationData(byte[] authenticationData) {
            if (authenticationData != null) {
                this.authenticationData = authenticationData;
            }
            return this;
        }

        @Override
        public InfoRequestBuilder withTtl(int ttl) {
            this.ttl = ttl;
            return this;
        }

        @Override
        public InfoRequestBuilder withMaskLength(byte maskLength) {
            this.maskLength = maskLength;
            return this;
        }

        @Override
        public InfoRequestBuilder withEidPrefix(LispAfiAddress eidPrefix) {
            this.eidPrefix = eidPrefix;
            return this;
        }

        @Override
        public LispInfoRequest build() {
            return new DefaultLispInfoRequest(infoReply, nonce, keyId,
                    authDataLength, authenticationData, ttl, maskLength, eidPrefix);
        }
    }

    /**
     * A LISP message reader for InfoRequest message.
     */
    public static class InfoRequestReader implements LispMessageReader<LispInfoRequest> {

        @Override
        public LispInfoRequest readFrom(ByteBuf byteBuf) throws LispParseError, LispReaderException {

            LispInfo lispInfo = DefaultLispInfo.deserialize(byteBuf);

            return new DefaultInfoRequestBuilder()
                    .withInfoReply(lispInfo.hasInfoReply())
                    .withNonce(lispInfo.getNonce())
                    .withKeyId(lispInfo.getKeyId())
                    .withAuthDataLength(lispInfo.getAuthDataLength())
                    .withAuthenticationData(lispInfo.getAuthenticationData())
                    .withTtl(lispInfo.getTtl())
                    .withMaskLength(lispInfo.getMaskLength())
                    .withEidPrefix(lispInfo.getPrefix()).build();
        }
    }

    /**
     * A LISP message writer for InfoRequest message.
     */
    public static final class InfoRequestWriter implements LispMessageWriter<LispInfoRequest> {

        @Override
        public void writeTo(ByteBuf byteBuf, LispInfoRequest message) throws LispWriterException {
            DefaultLispInfo.serialize(byteBuf, message);
        }
    }
}
