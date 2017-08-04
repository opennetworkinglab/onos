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

import org.onosproject.lisp.msg.protocols.LispMessage.Builder;
import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * LISP info message interface.
 */
public interface InfoBuilder<T> extends Builder {

    /**
     * Sets info reply flag value.
     *
     * @param infoReply info reply
     * @return T object
     */
    T withIsInfoReply(boolean infoReply);

    /**
     * Sets nonce value.
     *
     * @param nonce nonce value
     * @return T object
     */
    T withNonce(long nonce);

    /**
     * Sets authentication data length.
     *
     * @param authDataLength authentication data length
     * @return T object
     */
    T withAuthDataLength(short authDataLength);

    /**
     * Sets key identifier.
     *
     * @param keyId key identifier
     * @return T object
     */
    T withKeyId(short keyId);

    /**
     * Sets authentication data.
     *
     * @param authData authentication data
     * @return T object
     */
    T withAuthData(byte[] authData);

    /**
     * Sets authentication key.
     *
     * @param key authentication key
     * @return RegisterBuilder object
     */
    T withAuthKey(String key);

    /**
     * Sets Time-To-Live value.
     *
     * @param ttl Time-To-Live value
     * @return T object
     */
    T withTtl(int ttl);

    /**
     * Sets EID prefix mask length.
     *
     * @param maskLength EID prefix mask length
     * @return T object
     */
    T withMaskLength(byte maskLength);

    /**
     * Sets EID prefix.
     *
     * @param eidPrefix EID prefix
     * @return T object
     */
    T withEidPrefix(LispAfiAddress eidPrefix);
}
