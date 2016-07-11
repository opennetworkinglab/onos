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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .VpnInstAf;

/**
 * Represents the builder implementation of vpnInstAfs.
 */
public class VpnInstAfsBuilder implements VpnInstAfs.VpnInstAfsBuilder {

    private List<VpnInstAf> vpnInstAf;

    @Override
    public List<VpnInstAf> vpnInstAf() {
        return vpnInstAf;
    }

    @Override
    public VpnInstAfsBuilder vpnInstAf(List<VpnInstAf> vpnInstAf) {
        this.vpnInstAf = vpnInstAf;
        return this;
    }

    @Override
    public VpnInstAfs build() {
        return new VpnInstAfsImpl(this);
    }

    /**
     * Creates an instance of vpnInstAfsBuilder.
     */
    public VpnInstAfsBuilder() {
    }


    /**
     * Represents the implementation of vpnInstAfs.
     */
    public final class VpnInstAfsImpl implements VpnInstAfs {

        private List<VpnInstAf> vpnInstAf;

        @Override
        public List<VpnInstAf> vpnInstAf() {
            return vpnInstAf;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vpnInstAf);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VpnInstAfsImpl) {
                VpnInstAfsImpl other = (VpnInstAfsImpl) obj;
                return
                     Objects.equals(vpnInstAf, other.vpnInstAf);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("vpnInstAf", vpnInstAf)
                .toString();
        }

        /**
         * Creates an instance of vpnInstAfsImpl.
         *
         * @param builderObject builder object of vpnInstAfs
         */
        public VpnInstAfsImpl(VpnInstAfsBuilder builderObject) {
            this.vpnInstAf = builderObject.vpnInstAf();
        }
    }
}