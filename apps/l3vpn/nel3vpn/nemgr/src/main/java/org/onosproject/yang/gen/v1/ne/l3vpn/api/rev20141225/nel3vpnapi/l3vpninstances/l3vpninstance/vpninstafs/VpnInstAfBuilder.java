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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonL3VpnPrefixType;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .vpninstaf.VpnTargets;

/**
 * Represents the builder implementation of vpnInstAf.
 */
public class VpnInstAfBuilder implements VpnInstAf.VpnInstAfBuilder {

    private L3VpncommonL3VpnPrefixType afType;
    private String vrfRd;
    private VpnTargets vpnTargets;

    @Override
    public L3VpncommonL3VpnPrefixType afType() {
        return afType;
    }

    @Override
    public String vrfRd() {
        return vrfRd;
    }

    @Override
    public VpnTargets vpnTargets() {
        return vpnTargets;
    }

    @Override
    public VpnInstAfBuilder afType(L3VpncommonL3VpnPrefixType afType) {
        this.afType = afType;
        return this;
    }

    @Override
    public VpnInstAfBuilder vrfRd(String vrfRd) {
        this.vrfRd = vrfRd;
        return this;
    }

    @Override
    public VpnInstAfBuilder vpnTargets(VpnTargets vpnTargets) {
        this.vpnTargets = vpnTargets;
        return this;
    }

    @Override
    public VpnInstAf build() {
        return new VpnInstAfImpl(this);
    }

    /**
     * Creates an instance of vpnInstAfBuilder.
     */
    public VpnInstAfBuilder() {
    }


    /**
     * Represents the implementation of vpnInstAf.
     */
    public final class VpnInstAfImpl implements VpnInstAf {

        private L3VpncommonL3VpnPrefixType afType;
        private String vrfRd;
        private VpnTargets vpnTargets;

        @Override
        public L3VpncommonL3VpnPrefixType afType() {
            return afType;
        }

        @Override
        public String vrfRd() {
            return vrfRd;
        }

        @Override
        public VpnTargets vpnTargets() {
            return vpnTargets;
        }

        @Override
        public int hashCode() {
            return Objects.hash(afType, vrfRd, vpnTargets);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VpnInstAfImpl) {
                VpnInstAfImpl other = (VpnInstAfImpl) obj;
                return
                     Objects.equals(afType, other.afType) &&
                     Objects.equals(vrfRd, other.vrfRd) &&
                     Objects.equals(vpnTargets, other.vpnTargets);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("afType", afType)
                .add("vrfRd", vrfRd)
                .add("vpnTargets", vpnTargets)
                .toString();
        }

        /**
         * Creates an instance of vpnInstAfImpl.
         *
         * @param builderObject builder object of vpnInstAf
         */
        public VpnInstAfImpl(VpnInstAfBuilder builderObject) {
            this.afType = builderObject.afType();
            this.vrfRd = builderObject.vrfRd();
            this.vpnTargets = builderObject.vpnTargets();
        }
    }
}