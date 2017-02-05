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
package org.onosproject.lisp.msg.types.lcaf;

/**
 * LISP Canonical Address Format (LCAF) Enumeration class.
 *
 * LCAF defines a canonical address format encoding used in LISP control message
 * and in the encoding of lookup keys for the LISP Mapping Database System.
 *
 * LCAF is defined in draft-ietf-lisp-lcaf-22
 * https://tools.ietf.org/html/draft-ietf-lisp-lcaf-22
 */
public enum LispCanonicalAddressFormatEnum {
    UNKNOWN(-1),                // Unknown Type
    UNSPECIFIED(0),             // Unspecified Type
    LIST(1),                    // AFI LIST Type
    SEGMENT(2),                 // Instance ID Type
    AS(3),                      // AS Number Type
    APPLICATION_DATA(4),        // Application Data Type
    GEO_COORDINATE(5),          // Geo Coordinate Type
    NAT(7),                     // NAT Traversal Type
    NONCE(8),                   // Nonce Locator Type
    MULTICAST(9),               // Multi-cast Info Type
    TRAFFIC_ENGINEERING(10),    // Explicit Locator Path Type
    SECURITY(11),               // Security Key Type
    SOURCE_DEST(12);            // Source/Dest Key Type

    private byte lispCode;

    /**
     * Private constructor which avoid instantiating object externally.
     *
     * @param lispCode lisp code value
     */
    LispCanonicalAddressFormatEnum(int lispCode) {
        this.lispCode = (byte) lispCode;
    }

    /**
     * Obtains lisp code value.
     *
     * @return lisp code value
     */
    public byte getLispCode() {
        return lispCode;
    }

    /**
     * Obtains the LCAF enum using given lisp code.
     *
     * @param lispCode lisp code
     * @return LCAP enum
     */
    public static LispCanonicalAddressFormatEnum valueOf(int lispCode) {
        for (LispCanonicalAddressFormatEnum val : values()) {
            if (val.getLispCode() == lispCode) {
                return val;
            }
        }
        return UNKNOWN;
    }
}
