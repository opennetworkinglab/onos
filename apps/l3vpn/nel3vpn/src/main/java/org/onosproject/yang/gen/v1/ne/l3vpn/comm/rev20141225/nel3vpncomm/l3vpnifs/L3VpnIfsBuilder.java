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

package org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.l3vpnifs.L3VpnIf;

/**
 * Represents the builder implementation of l3VpnIfs.
 */
public class L3VpnIfsBuilder implements L3VpnIfs.L3VpnIfsBuilder {

    private List<L3VpnIf> l3VpnIf;

    @Override
    public List<L3VpnIf> l3VpnIf() {
        return l3VpnIf;
    }

    @Override
    public L3VpnIfsBuilder l3VpnIf(List<L3VpnIf> l3VpnIf) {
        this.l3VpnIf = l3VpnIf;
        return this;
    }

    @Override
    public L3VpnIfs build() {
        return new L3VpnIfsImpl(this);
    }

    /**
     * Creates an instance of l3VpnIfsBuilder.
     */
    public L3VpnIfsBuilder() {
    }


    /**
     * Represents the implementation of l3VpnIfs.
     */
    public final class L3VpnIfsImpl implements L3VpnIfs {

        private List<L3VpnIf> l3VpnIf;

        @Override
        public List<L3VpnIf> l3VpnIf() {
            return l3VpnIf;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l3VpnIf);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L3VpnIfsImpl) {
                L3VpnIfsImpl other = (L3VpnIfsImpl) obj;
                return
                     Objects.equals(l3VpnIf, other.l3VpnIf);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("l3VpnIf", l3VpnIf)
                .toString();
        }

        /**
         * Creates an instance of l3VpnIfsImpl.
         *
         * @param builderObject builder object of l3VpnIfs
         */
        public L3VpnIfsImpl(L3VpnIfsBuilder builderObject) {
            this.l3VpnIf = builderObject.l3VpnIf();
        }
    }
}