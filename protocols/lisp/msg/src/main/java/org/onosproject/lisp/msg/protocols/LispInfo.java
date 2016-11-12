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

import org.onosproject.lisp.msg.types.LispAfiAddress;

/**
 * LISP info super interface.
 */
public interface LispInfo extends LispMessage {

    /**
     * Obtains has info reply flag value.
     *
     * @return has info reply flag value
     */
    boolean isInfoReply();

    /**
     * Obtains nonce value.
     *
     * @return nonce value
     */
    long getNonce();

    /**
     * Obtains key identifier.
     *
     * @return key identifier
     */
    short getKeyId();

    /**
     * Obtains authentication data length.
     *
     * @return authentication data length
     */
    short getAuthDataLength();

    /**
     * Obtains authentication data.
     *
     * @return authentication data
     */
    byte[] getAuthData();

    /**
     * Obtains TTL value.
     *
     * @return record TTL value
     */
    int getTtl();

    /**
     * Obtains mask length of the EID Record.
     *
     * @return mask length of the EID Record
     */
    byte getMaskLength();

    /**
     * Obtains EID prefix.
     *
     * @return EID prefix
     */
    LispAfiAddress getPrefix();
}
