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
            .vpninstaf;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.l3vpninstance.vpninstafs
            .vpninstaf.vpntargets.VpnTarget;

/**
 * Represents the builder implementation of vpnTargets.
 */
public class VpnTargetsBuilder implements VpnTargets.VpnTargetsBuilder {

    private List<VpnTarget> vpnTarget;

    @Override
    public List<VpnTarget> vpnTarget() {
        return vpnTarget;
    }

    @Override
    public VpnTargetsBuilder vpnTarget(List<VpnTarget> vpnTarget) {
        this.vpnTarget = vpnTarget;
        return this;
    }

    @Override
    public VpnTargets build() {
        return new VpnTargetsImpl(this);
    }

    /**
     * Creates an instance of vpnTargetsBuilder.
     */
    public VpnTargetsBuilder() {
    }


    /**
     * Represents the implementation of vpnTargets.
     */
    public final class VpnTargetsImpl implements VpnTargets {

        private List<VpnTarget> vpnTarget;

        @Override
        public List<VpnTarget> vpnTarget() {
            return vpnTarget;
        }

        @Override
        public int hashCode() {
            return Objects.hash(vpnTarget);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof VpnTargetsImpl) {
                VpnTargetsImpl other = (VpnTargetsImpl) obj;
                return
                     Objects.equals(vpnTarget, other.vpnTarget);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("vpnTarget", vpnTarget)
                .toString();
        }

        /**
         * Creates an instance of vpnTargetsImpl.
         *
         * @param builderObject builder object of vpnTargets
         */
        public VpnTargetsImpl(VpnTargetsBuilder builderObject) {
            this.vpnTarget = builderObject.vpnTarget();
        }
    }
}