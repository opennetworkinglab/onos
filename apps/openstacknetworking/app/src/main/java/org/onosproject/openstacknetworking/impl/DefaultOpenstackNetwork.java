/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.openstacknetworking.api.OpenstackNetwork;

import java.util.Objects;

/**
 * Default implementation of augmented network.
 */
public final class DefaultOpenstackNetwork implements OpenstackNetwork {

    private final String networkId;
    private final Type type;

    /**
     * A default constructor for openstack network implementation class.
     *
     * @param networkId network ID
     * @param type      network type
     */
    public DefaultOpenstackNetwork(String networkId, Type type) {
        this.networkId = networkId;
        this.type = type;
    }

    @Override
    public String networkId() {
        return networkId;
    }

    @Override
    public Type type() {
        return type;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("networkId", networkId)
                .add("type", type)
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultOpenstackNetwork) {
            DefaultOpenstackNetwork that = (DefaultOpenstackNetwork) obj;
            return Objects.equals(networkId, that.networkId) &&
                    Objects.equals(type, that.type);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(networkId, type);
    }
}
