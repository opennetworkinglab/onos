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

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * Default Networks implementation.
 * <p>
 * The Set/Get methods below are defined to accept and pass references because
 * the object class is treated as a "composite" object class that holds
 * references to various member objects and their relationships, forming a
 * data tree. Internal routines of the TE topology manager may use the
 * following example methods to construct and manipulate any piece of data in
 * the data tree:
 * <pre>
 * newNode.getTe().setAdminStatus(), or
 * newNode.getSupportingNodeIds().add(nodeId), etc.
 * </pre>
 * Same for constructors where, for example, a child list may be constructed
 * first and passed in by reference to its parent object constructor.
 */
public class DefaultNetworks implements Networks, TeTopologyEventSubject {
    private List<Network> networks;

    /**
     * Creates an instance of DefaultNetworks.
     */
    public DefaultNetworks() {
    }

    /**
     * Constructor with all fields.
     *
     * @param networks list of networks
     */
    public DefaultNetworks(List<Network> networks) {
        this.networks = networks;
    }

    @Override
    public List<Network> networks() {
        return networks;
    }

    /**
     * Sets the networks.
     *
     * @param networks the networks to set
     */
    public void setNetworks(List<Network> networks) {
        this.networks = networks;
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
            return Objects.equal(this.networks, that.networks);
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
