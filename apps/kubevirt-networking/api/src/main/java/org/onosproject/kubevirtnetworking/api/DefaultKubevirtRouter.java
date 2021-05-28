/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.onlab.packet.MacAddress;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Default implementation class of kubevirt router.
 */
public final class DefaultKubevirtRouter implements KubevirtRouter {

    private static final String NOT_NULL_MSG = "Router % cannot be null";

    private final String name;
    private final String description;
    private final boolean enableSnat;
    private final MacAddress mac;
    private final Set<String> internal;
    private final Map<String, String> external;
    private final KubevirtPeerRouter peerRouter;
    private final String gateway;

    /**
     * A default constructor.
     *
     * @param name          router name
     * @param description   router description
     * @param enableSnat    snat use indicator
     * @param mac           MAC address
     * @param internal      internal networks
     * @param external      external network
     * @param peerRouter    external peer router
     * @param gateway       elected gateway node id
     */
    public DefaultKubevirtRouter(String name, String description,
                                 boolean enableSnat, MacAddress mac,
                                 Set<String> internal,
                                 Map<String, String> external,
                                 KubevirtPeerRouter peerRouter,
                                 String gateway) {
        this.name = name;
        this.description = description;
        this.enableSnat = enableSnat;
        this.mac = mac;
        this.internal = internal;
        this.external = external;
        this.peerRouter = peerRouter;
        this.gateway = gateway;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public boolean enableSnat() {
        return enableSnat;
    }

    @Override
    public MacAddress mac() {
        return mac;
    }

    @Override
    public Set<String> internal() {
        if (internal == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(internal);
        }
    }

    @Override
    public Map<String, String> external() {
        if (external == null) {
            return ImmutableMap.of();
        } else {
            return ImmutableMap.copyOf(external);
        }
    }

    @Override
    public KubevirtPeerRouter peerRouter() {
        return peerRouter;
    }

    @Override
    public String electedGateway() {
        return gateway;
    }

    @Override
    public KubevirtRouter updatePeerRouter(KubevirtPeerRouter updated) {
        return DefaultKubevirtRouter.builder()
                .name(name)
                .enableSnat(enableSnat)
                .description(description)
                .mac(mac)
                .internal(internal)
                .external(external)
                .peerRouter(updated)
                .electedGateway(gateway)
                .build();
    }

    @Override
    public KubevirtRouter updatedElectedGateway(String updated) {
        return DefaultKubevirtRouter.builder()
                .name(name)
                .enableSnat(enableSnat)
                .description(description)
                .mac(mac)
                .internal(internal)
                .external(external)
                .peerRouter(peerRouter)
                .electedGateway(updated)
                .build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultKubevirtRouter that = (DefaultKubevirtRouter) o;
        return enableSnat == that.enableSnat && name.equals(that.name) &&
                description.equals(that.description) && mac.equals(that.mac) &&
                internal.equals(that.internal) && external.equals(that.external) &&
                peerRouter.equals(that.peerRouter) && gateway.equals(that.electedGateway());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, mac);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("description", description)
                .add("enableSnat", enableSnat)
                .add("mac", mac)
                .add("internal", internal)
                .add("external", external)
                .add("peerRouter", peerRouter)
                .add("electedGateway", gateway)
                .toString();
    }

    /**
     * Returns new builder instance.
     *
     * @return kubevirt router builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements KubevirtRouter.Builder {

        private String name;
        private String description;
        private boolean enableSnat;
        private MacAddress mac;
        private Set<String> internal;
        private Map<String, String> external;
        private KubevirtPeerRouter peerRouter;
        private String gateway;

        @Override
        public KubevirtRouter build() {
            checkArgument(name != null, NOT_NULL_MSG, "name");

            return new DefaultKubevirtRouter(name, description, enableSnat, mac,
                    internal, external, peerRouter, gateway);
        }

        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Builder enableSnat(boolean flag) {
            this.enableSnat = flag;
            return this;
        }

        @Override
        public Builder mac(MacAddress mac) {
            this.mac = mac;
            return this;
        }

        @Override
        public Builder internal(Set<String> internal) {
            this.internal = Objects.requireNonNullElseGet(internal, HashSet::new);
            return this;
        }

        @Override
        public Builder external(Map<String, String> external) {
            this.external = Objects.requireNonNullElseGet(external, HashMap::new);
            return this;
        }

        @Override
        public Builder peerRouter(KubevirtPeerRouter router) {
            this.peerRouter = router;
            return this;
        }

        @Override
        public Builder electedGateway(String gateway) {
            this.gateway = gateway;
            return this;
        }
    }
}
