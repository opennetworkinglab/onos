/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import java.util.List;

/**
 * Default Networks implementation.
 */
public class DefaultNetworks implements Networks {
    private final List<Network> networks;

    /**
     * Creates an instance of DefaultNetworks.
     *
     * @param networks list of networks
     */
    public DefaultNetworks(List<Network> networks) {
        this.networks = networks != null ?
                Lists.newArrayList(networks) : null;
    }

    @Override
    public List<Network> networks() {
        if (networks == null) {
            return null;
        }
        return ImmutableList.copyOf(networks);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networks);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetworks) {
            DefaultNetworks that = (DefaultNetworks) object;
            return Objects.equal(networks, that.networks);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("networks", networks)
                .toString();
    }

}
