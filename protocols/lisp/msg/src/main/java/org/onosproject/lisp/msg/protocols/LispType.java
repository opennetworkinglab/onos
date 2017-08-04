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

/**
 * LISP message type enumeration.
 *
 * An enumeration of LISP Message type defined in RFC6830
 * https://tools.ietf.org/html/rfc6830
 */
public enum LispType {

    /** LISP Map-Request Message. */
    LISP_MAP_REQUEST(1),

    /** LISP Map-Reply Message. */
    LISP_MAP_REPLY(2),

    /** LISP Map-Register Message. */
    LISP_MAP_REGISTER(3),

    /** LISP Map-Notify Message. */
    LISP_MAP_NOTIFY(4),

    /** LISP Map-Referral Message. */
    LISP_MAP_REFERRAL(5),

    /** LISP Info-Request or Info-Reply Message. */
    LISP_INFO(7),

    /** LISP Encapsulated Control Message. */
    LISP_ENCAPSULATED_CONTROL(8),

    /** Unknown types for internal use. */
    UNKNOWN(-1);

    private final short type;

    LispType(int type) {
        this.type = (short) type;
    }

    /**
     * Obtains LISP type code value.
     *
     * @return LISP type code value
     */
    public short getTypeCode() {
        return type;
    }

    /**
     * Obtains LISP type enum by providing type code value.
     *
     * @param typeCode LISP type code value
     * @return LISP type enum
     */
    public static LispType valueOf(short typeCode) {
        for (LispType val : values()) {
            if (val.getTypeCode() == typeCode) {
                return val;
            }
        }
        return UNKNOWN;
    }
}
