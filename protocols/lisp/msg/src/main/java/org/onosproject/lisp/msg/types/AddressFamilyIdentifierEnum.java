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
package org.onosproject.lisp.msg.types;

/**
 * Address Family Identifier (AFI) Enumeration class.
 *
 * An enumeration of AFIs defined by iana.
 * Now, we only declare 10 enums, more enums will be declared later on.
 *
 * http://www.iana.org/assignments/address-family-numbers/address-family-numbers.xhtml
 */
public enum AddressFamilyIdentifierEnum {

    NO_ADDRESS(0),              // Reserved
    IP4(1),                     // IP4 (IP version 4)
    IP6(2),                     // IP6 (IP version 6)
    DNS(16),                    // Domain Name System
    DISTINGUISHED_NAME(17),     // Distinguished Name
    AS(18),                     // AS Number
    LCAF(16387),                // LISP Canonical Address Format (LCAF)
    MAC(16389),                 // 48-bit MAC
    OUI(16391),                 // 24-bit Organizationally Unique Identifier
    UNKNOWN(-1);                // Other Enums for internal use

    private final short ianaCode;

    AddressFamilyIdentifierEnum(int ianaCode) {
        this.ianaCode = (short) ianaCode;
    }

    /**
     * Obtains iana code value.
     *
     * @return iana code value
     */
    public short getIanaCode() {
        return ianaCode;
    }

    /**
     * Obtains AFI enum by providing iana code value.
     *
     * @param ianaCode iana code value
     * @return AFI enum
     */
    public static AddressFamilyIdentifierEnum valueOf(short ianaCode) {
        for (AddressFamilyIdentifierEnum val : values()) {
            if (val.getIanaCode() == ianaCode) {
                return val;
            }
        }
        return UNKNOWN;
    }
}
