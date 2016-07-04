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

package org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype;

import org.onosproject.yang.gen.v1.ne.bgpcomm.type.rev20141225.nebgpcommtype.bgpcommimrouteprotocol
            .BgpcommImRouteProtocolEnum;
import java.util.Objects;
import com.google.common.base.MoreObjects;

/**
 * Represents the implementation of bgpcommImRouteProtocol.
 */
public final class BgpcommImRouteProtocol {

    private BgpcommImRouteProtocolEnum enumeration;

    /**
     * Creates an instance of bgpcommImRouteProtocol.
     */
    private BgpcommImRouteProtocol() {
    }

    /**
     * Creates an instance of bgpcommImRouteProtocolForTypeEnumeration.
     *
     * @param value value of bgpcommImRouteProtocolForTypeEnumeration
     */
    public BgpcommImRouteProtocol(BgpcommImRouteProtocolEnum value) {
        this.enumeration = value;
    }

    /**
     * Returns the object of bgpcommImRouteProtocolForTypeEnumeration.
     *
     * @param value value of bgpcommImRouteProtocolForTypeEnumeration
     * @return Object of bgpcommImRouteProtocolForTypeEnumeration
     */
    public static BgpcommImRouteProtocol of(BgpcommImRouteProtocolEnum value) {
        return new BgpcommImRouteProtocol(value);
    }

    /**
     * Returns the attribute enumeration.
     *
     * @return value of enumeration
     */
    public BgpcommImRouteProtocolEnum enumeration() {
        return enumeration;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enumeration);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpcommImRouteProtocol) {
            BgpcommImRouteProtocol other = (BgpcommImRouteProtocol) obj;
            return
                 Objects.equals(enumeration, other.enumeration);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("enumeration", enumeration)
            .toString();
    }

    /**
     * Returns the object of bgpcommImRouteProtocol fromString input String.
     *
     * @param valInString input String
     * @return Object of bgpcommImRouteProtocol
     */
    public static BgpcommImRouteProtocol fromString(String valInString) {
        try {
            BgpcommImRouteProtocolEnum tmpVal = BgpcommImRouteProtocolEnum.fromString(valInString);
            return of(tmpVal);
        } catch (Exception e) {
        }
        return null;
    }
}
