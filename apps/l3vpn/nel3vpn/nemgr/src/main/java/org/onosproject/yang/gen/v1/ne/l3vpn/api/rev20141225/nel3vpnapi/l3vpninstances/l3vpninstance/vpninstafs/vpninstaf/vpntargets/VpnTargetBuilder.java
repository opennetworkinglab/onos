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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .vpninstaf.vpntargets;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.l3vpn.comm.type.rev20141225.nel3vpncommtype.L3VpncommonVrfRtType;

/**
 * Represents the builder implementation of vpnTarget.
 */
public class VpnTargetBuilder implements VpnTarget.VpnTargetBuilder {

    private String vrfRtvalue;
    private L3VpncommonVrfRtType vrfRttype;

    @Override
    public String vrfRtvalue() {
        return vrfRtvalue;
    }

    @Override
    public L3VpncommonVrfRtType vrfRttype() {
        return vrfRttype;
    }

    @Override
    public VpnTargetBuilder vrfRtvalue(String vrfRtvalue) {
        this.vrfRtvalue = vrfRtvalue;
        return this;
    }

    @Override
    public VpnTargetBuilder vrfRttype(L3VpncommonVrfRtType vrfRttype) {
        this.vrfRttype = vrfRttype;
        return this;
    }

    @Override
    public VpnTarget build() {
        return new VpnTargetImpl(this);
    }

    /**
     * Creates an instance of vpnTargetBuilder.
     */
    public VpnTargetBuilder() {
    }


    /**
     * Represents the implementation of vpnTarget.
     */
    public final class VpnTargetImpl implements VpnTarget {

        private String vrfRtvalue;
        private L3VpncommonVrfRtType vrfRttype;

        @Override
        public String vrfRtvalue() {
            return vrfRtvalue;
        }

        @Override
        public L3VpncommonVrfRtType vrfRttype() {
            return vrfRttype;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vrfRtvalue, vrfRttype);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VpnTargetImpl) {
                VpnTargetImpl other = (VpnTargetImpl) obj;
                return
                     Objects.equals(vrfRtvalue, other.vrfRtvalue) &&
                     Objects.equals(vrfRttype, other.vrfRttype);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("vrfRtvalue", vrfRtvalue)
                .add("vrfRttype", vrfRttype)
                .toString();
        }

        /**
         * Creates an instance of vpnTargetImpl.
         *
         * @param builderObject builder object of vpnTarget
         */
        public VpnTargetImpl(VpnTargetBuilder builderObject) {
            this.vrfRtvalue = builderObject.vrfRtvalue();
            this.vrfRttype = builderObject.vrfRttype();
        }
    }
}