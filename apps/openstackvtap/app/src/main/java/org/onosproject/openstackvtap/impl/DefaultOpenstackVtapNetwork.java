/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.impl;

import org.onlab.packet.IpAddress;
import org.onosproject.net.AbstractDescription;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.openstackvtap.api.OpenstackVtapNetwork;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of an immutable OpenstackVtapNetwork.
 */
public final class DefaultOpenstackVtapNetwork extends AbstractDescription implements OpenstackVtapNetwork {

    private final Mode mode;
    private final Integer networkId;
    private final IpAddress serverIp;

    /**
     * Creates an DefaultOpenstackVtapNetwork using the supplied information.
     *
     * @param mode        mode of vtap network
     * @param networkId   network id of the vtap tunneling network
     * @param serverIp    server IP address used for tunneling
     * @param annotations optional key/value annotations
     */
    protected DefaultOpenstackVtapNetwork(Mode mode,
                                          Integer networkId,
                                          IpAddress serverIp,
                                          SparseAnnotations... annotations) {
        super(annotations);
        this.mode = checkNotNull(mode);
        this.networkId = networkId;
        this.serverIp = checkNotNull(serverIp);
    }

    @Override
    public Mode mode() {
        return mode;
    }

    @Override
    public Integer networkId() {
        return networkId;
    }

    @Override
    public IpAddress serverIp() {
        return serverIp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mode, networkId, serverIp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultOpenstackVtapNetwork) {
            final DefaultOpenstackVtapNetwork other = (DefaultOpenstackVtapNetwork) obj;
            return Objects.equals(this.mode, other.mode) &&
                    Objects.equals(this.networkId, other.networkId) &&
                    Objects.equals(this.serverIp, other.serverIp) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mode", mode())
                .add("networkId", networkId())
                .add("serverIp", serverIp())
                .add("annotations", annotations())
                .toString();
    }

    /**
     * Creates OpenstackVtapNetwork builder with default parameters.
     *
     * @return builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates OpenstackVtapNetwork builder inheriting with default parameters,
     * from specified OpenstackVtapNetwork.
     *
     * @param vtapNetwork to inherit default from
     * @return builder
     */
    public static Builder builder(OpenstackVtapNetwork vtapNetwork) {
        return new Builder(vtapNetwork);
    }

    /**
     * Builder for DefaultOpenstackVtapNetwork object.
     */
    public static class Builder implements OpenstackVtapNetwork.Builder {
        private Mode mode;
        private Integer networkId;
        private IpAddress serverIp;
        private SparseAnnotations annotations = DefaultAnnotations.EMPTY;

        // private constructor not intended to use from external
        private Builder() {
        }

        Builder(OpenstackVtapNetwork description) {
            this.mode = description.mode();
            this.networkId = description.networkId();
            this.serverIp = description.serverIp();
            this.annotations  = (SparseAnnotations) description.annotations();
        }

        /**
         * Sets mandatory field mode.
         *
         * @param mode of vtap network
         * @return self
         */
        @Override
        public Builder mode(Mode mode) {
            this.mode = mode;
            return this;
        }

        /**
         * Sets mandatory field networkId.
         *
         * @param networkId of the vtap tunneling network
         * @return self
         */
        @Override
        public Builder networkId(Integer networkId) {
            this.networkId = networkId;
            return this;
        }

        /**
         * Sets mandatory field serverIp.
         *
         * @param serverIp address used for tunneling
         * @return self
         */
        @Override
        public Builder serverIp(IpAddress serverIp) {
            this.serverIp = serverIp;
            return this;
        }

        /**
         * Sets annotations.
         *
         * @param annotations of the vtap network
         * @return self
         */
        @Override
        public Builder annotations(SparseAnnotations annotations) {
            this.annotations = annotations;
            return this;
        }

        /**
         * Builds a DefaultOpenstackVtapNetwork instance.
         *
         * @return DefaultOpenstackVtapNetwork
         */
        @Override
        public DefaultOpenstackVtapNetwork build() {
            return new DefaultOpenstackVtapNetwork(checkNotNull(mode),
                    networkId,
                    checkNotNull(serverIp),
                    checkNotNull(annotations));
        }
    }

}
