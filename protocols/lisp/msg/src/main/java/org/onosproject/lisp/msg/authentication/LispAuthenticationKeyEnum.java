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
package org.onosproject.lisp.msg.authentication;

/**
 * Authentication key enumeration class.
 *
 * By far, LISP also support two types of MAC authentication which are
 * HMAC-SHA-1-96 and HMAC-SHA-256-128.
 *
 * https://tools.ietf.org/html/rfc6830#page-39
 */
public enum LispAuthenticationKeyEnum {

    /** No authentication. */
    NONE(0, null, 0),

    /** HMAC SHA1 encryption. */
    SHA1(1, "HmacSHA1", 20),

    /** HMAC SHA256 encryption. */
    SHA256(2, "HmacSHA256", 32),

    /** Unsupported authentication type. */
    UNKNOWN(-1, "UNKNOWN", 0);

    private short keyId;
    private String name;
    private short length;

    LispAuthenticationKeyEnum(int keyId, String name, int length) {
        this.keyId = (short) keyId;
        this.name = name;
        this.length = (short) length;
    }

    /**
     * Obtains authentication key identifier.
     *
     * @return authentication key identifier
     */
    public short getKeyId() {
        return keyId;
    }

    /**
     * Obtains authentication name.
     *
     * @return authentication name
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains hash length.
     *
     * @return hash length
     */
    public short getHashLength() {
        return length;
    }

    /**
     * Obtains LISP authentication key enum by providing key identifier.
     *
     * @param keyId LISP authentication key identifier
     * @return LISP authentication key enum
     */
    public static LispAuthenticationKeyEnum valueOf(short keyId) {
        for (LispAuthenticationKeyEnum val : values()) {
            if (val.getKeyId() == keyId) {
                return val;
            }
        }
        return UNKNOWN;
    }
}
