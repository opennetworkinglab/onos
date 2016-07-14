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

    NO_ADDRESS(0), IP(1), IP6(2), DNS(16), DISTINGUISHED_NAME(17), AS(18), LCAF(16387),
    MAC(16389), OUI(16391), UNKNOWN(-1);

    private short ianaCode;

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
