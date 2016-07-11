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

package org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.Ipv4Address;

/**
 * Represents the builder implementation of l3VpnIf.
 */
public class L3VpnIfBuilder implements L3VpnIf.L3VpnIfBuilder {

    private String ifName;
    private Ipv4Address ipv4Addr;
    private Ipv4Address subnetMask;

    @Override
    public String ifName() {
        return ifName;
    }

    @Override
    public Ipv4Address ipv4Addr() {
        return ipv4Addr;
    }

    @Override
    public Ipv4Address subnetMask() {
        return subnetMask;
    }

    @Override
    public L3VpnIfBuilder ifName(String ifName) {
        this.ifName = ifName;
        return this;
    }

    @Override
    public L3VpnIfBuilder ipv4Addr(Ipv4Address ipv4Addr) {
        this.ipv4Addr = ipv4Addr;
        return this;
    }

    @Override
    public L3VpnIfBuilder subnetMask(Ipv4Address subnetMask) {
        this.subnetMask = subnetMask;
        return this;
    }

    @Override
    public L3VpnIf build() {
        return new L3VpnIfImpl(this);
    }

    /**
     * Creates an instance of l3VpnIfBuilder.
     */
    public L3VpnIfBuilder() {
    }


    /**
     * Represents the implementation of l3VpnIf.
     */
    public final class L3VpnIfImpl implements L3VpnIf {

        private String ifName;
        private Ipv4Address ipv4Addr;
        private Ipv4Address subnetMask;

        @Override
        public String ifName() {
            return ifName;
        }

        @Override
        public Ipv4Address ipv4Addr() {
            return ipv4Addr;
        }

        @Override
        public Ipv4Address subnetMask() {
            return subnetMask;
        }

        @Override
        public int hashCode() {
            return Objects.hash(ifName, ipv4Addr, subnetMask);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L3VpnIfImpl) {
                L3VpnIfImpl other = (L3VpnIfImpl) obj;
                return
                     Objects.equals(ifName, other.ifName) &&
                     Objects.equals(ipv4Addr, other.ipv4Addr) &&
                     Objects.equals(subnetMask, other.subnetMask);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("ifName", ifName)
                .add("ipv4Addr", ipv4Addr)
                .add("subnetMask", subnetMask)
                .toString();
        }

        /**
         * Creates an instance of l3VpnIfImpl.
         *
         * @param builderObject builder object of l3VpnIf
         */
        public L3VpnIfImpl(L3VpnIfBuilder builderObject) {
            this.ifName = builderObject.ifName();
            this.ipv4Addr = builderObject.ipv4Addr();
            this.subnetMask = builderObject.subnetMask();
        }
    }
}