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

import io.netty.buffer.ByteBuf;
import org.onosproject.lisp.msg.exceptions.LispWriterException;

/**
 * LISP signature interface.
 *
 * <p>
 * LISP signature format is defined in draft-ietf-lisp-ddt-09.
 * https://tools.ietf.org/html/draft-ietf-lisp-ddt-09#page-14
 *
 * <pre>
 * {@literal
 *      0                   1                   2                   3
 *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *    /|                      Original Record TTL                      |
 *   / +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *  /  |                      Signature Expiration                     |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * s   |                      Signature Inception                      |
 * i   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * g   |            Key Tag            |           Sig Length          |
 * |   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * \   | Sig-Algorithm |    Reserved   |            Reserved           |
 *  \  +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 *   \ ~                             Signature                         ~
 *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * }</pre>
 */
public interface LispSignature {

    /**
     * Obtains record TTL value.
     *
     * @return record TTL value
     */
    int getRecordTtl();

    /**
     * Obtains signature expiration.
     *
     * @return signature expiration
     */
    int getSigExpiration();

    /**
     * Obtains signature inception.
     *
     * @return signature inception
     */
    int getSigInception();

    /**
     * Obtains key tag.
     *
     * @return key tag
     */
    short getKeyTag();

    /**
     * Obtains signature length.
     *
     * @return signature length.
     */
    short getSigLength();

    /**
     * Obtains signature algorithm.
     *
     * @return signature algorithm
     */
    byte getSigAlgorithm();

    /**
     * Obtains signature.
     *
     * @return signature
     */
    int getSignature();

    /**
     * Writes LISP object into communication channel.
     *
     * @param byteBuf byte buffer
     * @throws LispWriterException on error
     */
    void writeTo(ByteBuf byteBuf) throws LispWriterException;

    /**
     * A builder for LISP signature.
     */
    interface SignatureBuilder {

        /**
         * Sets record TTL value.
         *
         * @param recordTtl record TTL
         * @return SignatureBuilder object
         */
        SignatureBuilder withRecordTtl(int recordTtl);

        /**
         * Sets signature expiration.
         *
         * @param sigExpiration signature expiration
         * @return SignatureBuilder object
         */
        SignatureBuilder withSigExpiration(int sigExpiration);

        /**
         * Sets signature inception.
         *
         * @param sigInception signature inception
         * @return SignatureBuilder object
         */
        SignatureBuilder withSigInception(int sigInception);

        /**
         * Sets key tag.
         *
         * @param keyTag key tag
         * @return SignatureBuilder object
         */
        SignatureBuilder withKeyTag(short keyTag);

        /**
         * Sets signature length.
         *
         * @param sigLength signature length
         * @return SignatureBuilder object
         */
        SignatureBuilder withSigLength(short sigLength);

        /**
         * Sets signature algorithm.
         *
         * @param sigAlgorithm signature algorithm
         * @return SignatureBuilder object
         */
        SignatureBuilder withSigAlgorithm(byte sigAlgorithm);

        /**
         * Sets signature.
         *
         * @param signature signature
         * @return SignatureBuilder object
         */
        SignatureBuilder withSignature(int signature);

        /**
         * Builds LISP signature object.
         *
         * @return LISP signature object
         */
        LispSignature build();
    }
}
