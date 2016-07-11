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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances;

import com.google.common.base.MoreObjects;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.VpnInstAfs;
import org.onosproject.yang.gen.v1.ne.l3vpn.comm.rev20141225.nel3vpncomm.l3vpnifs.L3VpnIfs;

/**
 * Represents the builder implementation of l3VpnInstance.
 */
public class L3VpnInstanceBuilder implements L3VpnInstance.L3VpnInstanceBuilder {

    private String vrfName;
    private String vrfDescription;
    private L3VpnIfs l3VpnIfs;
    private VpnInstAfs vpnInstAfs;

    @Override
    public String vrfName() {
        return vrfName;
    }

    @Override
    public String vrfDescription() {
        return vrfDescription;
    }

    @Override
    public L3VpnIfs l3VpnIfs() {
        return l3VpnIfs;
    }

    @Override
    public VpnInstAfs vpnInstAfs() {
        return vpnInstAfs;
    }

    @Override
    public L3VpnInstanceBuilder vrfName(String vrfName) {
        this.vrfName = vrfName;
        return this;
    }

    @Override
    public L3VpnInstanceBuilder vrfDescription(String vrfDescription) {
        this.vrfDescription = vrfDescription;
        return this;
    }

    @Override
    public L3VpnInstanceBuilder l3VpnIfs(L3VpnIfs l3VpnIfs) {
        this.l3VpnIfs = l3VpnIfs;
        return this;
    }

    @Override
    public L3VpnInstanceBuilder vpnInstAfs(VpnInstAfs vpnInstAfs) {
        this.vpnInstAfs = vpnInstAfs;
        return this;
    }

    @Override
    public L3VpnInstance build() {
        return new L3VpnInstanceImpl(this);
    }

    /**
     * Creates an instance of l3VpnInstanceBuilder.
     */
    public L3VpnInstanceBuilder() {
    }


    /**
     * Represents the implementation of l3VpnInstance.
     */
    public final class L3VpnInstanceImpl implements L3VpnInstance {

        private String vrfName;
        private String vrfDescription;
        private L3VpnIfs l3VpnIfs;
        private VpnInstAfs vpnInstAfs;

        @Override
        public String vrfName() {
            return vrfName;
        }

        @Override
        public String vrfDescription() {
            return vrfDescription;
        }

        @Override
        public L3VpnIfs l3VpnIfs() {
            return l3VpnIfs;
        }

        @Override
        public VpnInstAfs vpnInstAfs() {
            return vpnInstAfs;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vrfName, vrfDescription, l3VpnIfs, vpnInstAfs);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L3VpnInstanceImpl) {
                L3VpnInstanceImpl other = (L3VpnInstanceImpl) obj;
                return
                     Objects.equals(vrfName, other.vrfName) &&
                     Objects.equals(vrfDescription, other.vrfDescription) &&
                     Objects.equals(l3VpnIfs, other.l3VpnIfs) &&
                     Objects.equals(vpnInstAfs, other.vpnInstAfs);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("vrfName", vrfName)
                .add("vrfDescription", vrfDescription)
                .add("l3VpnIfs", l3VpnIfs)
                .add("vpnInstAfs", vpnInstAfs)
                .toString();
        }

        /**
         * Creates an instance of l3VpnInstanceImpl.
         *
         * @param builderObject builder object of l3VpnInstance
         */
        public L3VpnInstanceImpl(L3VpnInstanceBuilder builderObject) {
            this.vrfName = builderObject.vrfName();
            this.vrfDescription = builderObject.vrfDescription();
            this.l3VpnIfs = builderObject.l3VpnIfs();
            this.vpnInstAfs = builderObject.vpnInstAfs();
        }
    }
}