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

import java.util.Objects;

/**
 * LISP Locator address typed by Address Family Identifier (AFI).
 */
public abstract class LispAfiAddress {

    private final AddressFamilyIdentifierEnum afi;

    /**
     * Initializes AFI enumeration value.
     *
     * @param afi address family identifier
     */
    protected LispAfiAddress(AddressFamilyIdentifierEnum afi) {
        this.afi = afi;
    }

    /**
     * Obtains AFI enumeration value.
     *
     * @return AFI enumeration value
     */
    public AddressFamilyIdentifierEnum getAfi() {
        return afi;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afi);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        }

        LispAfiAddress other = (LispAfiAddress) obj;
        if (afi != other.afi) {
            return false;
        }
        return true;
    }
}
