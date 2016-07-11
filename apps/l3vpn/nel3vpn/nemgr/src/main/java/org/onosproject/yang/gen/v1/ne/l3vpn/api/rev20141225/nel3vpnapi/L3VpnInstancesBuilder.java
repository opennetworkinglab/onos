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

package org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi;

import com.google.common.base.MoreObjects;
import java.util.List;
import java.util.Objects;
import org.onosproject.yang.gen.v1.ne.l3vpn.api.rev20141225.nel3vpnapi.l3vpninstances.L3VpnInstance;

/**
 * Represents the builder implementation of l3VpnInstances.
 */
public class L3VpnInstancesBuilder implements L3VpnInstances.L3VpnInstancesBuilder {

    private List<L3VpnInstance> l3VpnInstance;

    @Override
    public List<L3VpnInstance> l3VpnInstance() {
        return l3VpnInstance;
    }

    @Override
    public L3VpnInstancesBuilder l3VpnInstance(List<L3VpnInstance> l3VpnInstance) {
        this.l3VpnInstance = l3VpnInstance;
        return this;
    }

    @Override
    public L3VpnInstances build() {
        return new L3VpnInstancesImpl(this);
    }

    /**
     * Creates an instance of l3VpnInstancesBuilder.
     */
    public L3VpnInstancesBuilder() {
    }


    /**
     * Represents the implementation of l3VpnInstances.
     */
    public final class L3VpnInstancesImpl implements L3VpnInstances {

        private List<L3VpnInstance> l3VpnInstance;

        @Override
        public List<L3VpnInstance> l3VpnInstance() {
            return l3VpnInstance;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l3VpnInstance);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj instanceof L3VpnInstancesImpl) {
                L3VpnInstancesImpl other = (L3VpnInstancesImpl) obj;
                return
                     Objects.equals(l3VpnInstance, other.l3VpnInstance);
            }
            return false;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                .add("l3VpnInstance", l3VpnInstance)
                .toString();
        }

        /**
         * Creates an instance of l3VpnInstancesImpl.
         *
         * @param builderObject builder object of l3VpnInstances
         */
        public L3VpnInstancesImpl(L3VpnInstancesBuilder builderObject) {
            this.l3VpnInstance = builderObject.l3VpnInstance();
        }
    }
}