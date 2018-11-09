/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.ofagent.impl;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.TenantId;
import org.onosproject.ofagent.api.OFAgent;
import org.onosproject.ofagent.api.OFController;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of OpenFlow agent.
 */
public final class DefaultOFAgent implements OFAgent {

    private final NetworkId networkId;
    private final TenantId tenantId;

    private final Set<OFController> controllers;
    private final State state;

    private DefaultOFAgent(NetworkId networkId, TenantId tenantId,
                           Set<OFController> controllers,
                           State state) {
        this.networkId = networkId;
        this.tenantId = tenantId;
        this.controllers = controllers;
        this.state = state;
    }

    @Override
    public NetworkId networkId() {
        return networkId;
    }

    @Override
    public TenantId tenantId() {
        return tenantId;
    }

    @Override
    public Set<OFController> controllers() {
        return controllers;
    }

    @Override
    public State state() {
        return state;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultOFAgent) {
            DefaultOFAgent that = (DefaultOFAgent) obj;
            if (Objects.equals(networkId, that.networkId)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networkId", this.networkId)
                .add("tenantId", this.tenantId)
                .add("controllers", this.controllers)
                .add("state", this.state)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return default ofagent builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements OFAgent.Builder {

        private NetworkId networkId;
        private TenantId tenantId;
        private Set<OFController> controllers = Sets.newHashSet();
        private State state;

        private Builder() {
        }

        @Override
        public OFAgent build() {
            checkNotNull(networkId, "Network ID cannot be null");
            checkNotNull(tenantId, "Tenant ID cannot be null");
            checkNotNull(state, "State cannot be null");
            controllers = controllers == null ? ImmutableSet.of() : controllers;

            return new DefaultOFAgent(networkId, tenantId, controllers, state);
        }

        @Override
        public Builder from(OFAgent ofAgent) {
            this.networkId = ofAgent.networkId();
            this.tenantId = ofAgent.tenantId();
            this.controllers = Sets.newHashSet(ofAgent.controllers());
            this.state = ofAgent.state();
            return this;
        }

        @Override
        public Builder networkId(NetworkId networkId) {
            this.networkId = networkId;
            return this;
        }

        @Override
        public Builder tenantId(TenantId tenantId) {
            this.tenantId = tenantId;
            return this;
        }

        @Override
        public Builder controllers(Set<OFController> controllers) {
            this.controllers = controllers;
            return this;
        }

        @Override
        public OFAgent.Builder addController(OFController controller) {
            this.controllers.add(controller);
            return this;
        }

        @Override
        public OFAgent.Builder deleteController(OFController controller) {
            this.controllers.remove(controller);
            return this;
        }

        @Override
        public Builder state(State state) {
            this.state = state;
            return this;
        }
    }
}
