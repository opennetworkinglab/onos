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

package org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.acgroup.acs.ac;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;

/**
 * Represents the builder implementation of l3Access.
 */
public class L3AccessBuilder implements L3Access.L3AccessBuilder {

    private Ipv4Address address;

    @Override
    public Ipv4Address address() {
        return address;
    }

    @Override
    public L3AccessBuilder address(Ipv4Address address) {
        this.address = address;
        return this;
    }

    @Override
    public L3Access build() {
        return new L3AccessImpl(this);
    }

    /**
     * Creates an instance of l3AccessBuilder.
     */
    public L3AccessBuilder() {
    }


    /**
     * Represents the implementation of l3Access.
     */
    public final class L3AccessImpl implements L3Access {

        private Ipv4Address address;

        @Override
        public Ipv4Address address() {
            return address;
        }

        @Override
        public int hashCode() {
            return Objects.hash(address);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L3AccessImpl) {
                L3AccessImpl other = (L3AccessImpl) obj;
                return
                     Objects.equals(address, other.address);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("address", address)
                .toString();
        }

        /**
         * Creates an instance of l3AccessImpl.
         *
         * @param builderObject builder object of l3Access
         */
        public L3AccessImpl(L3AccessBuilder builderObject) {
            this.address = builderObject.address();
        }
    }
}