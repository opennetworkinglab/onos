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

package org.onosproject.isis.controller.impl.topology;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.Ip4Address;
import org.onosproject.isis.controller.topology.IsisRouter;

/**
 * Representation of an ISIS Router.
 */
public class DefaultIsisRouter implements IsisRouter {

    private String systemId;
    private Ip4Address neighborRouterId;
    private Ip4Address interfaceId;
    private boolean isDis;

    /**
     * Gets the system ID.
     *
     * @return systemId system ID
     */
    public String systemId() {
        return systemId;
    }

    /**
     * Sets IP address of the Router.
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    /**
     * Gets IP address of the interface.
     *
     * @return IP address of the interface
     */
    public Ip4Address interfaceId() {
        return interfaceId;
    }

    /**
     * Gets IP address of the interface.
     *
     * @param interfaceId IP address of the interface
     */
    public void setInterfaceId(Ip4Address interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * Gets neighbor's Router id.
     *
     * @return neighbor's Router id
     */
    public Ip4Address neighborRouterId() {
        return neighborRouterId;
    }

    /**
     * Sets neighbor's Router id.
     *
     * @param advertisingRouterId neighbor's Router id
     */
    public void setNeighborRouterId(Ip4Address advertisingRouterId) {
        this.neighborRouterId = advertisingRouterId;
    }

    /**
     * Gets if DR or not.
     *
     * @return true if DR else false
     */
    public boolean isDis() {
        return isDis;
    }

    /**
     * Sets dis or not.
     *
     * @param dis true if DIS else false
     */
    public void setDis(boolean dis) {
        isDis = dis;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("systemId", systemId)
                .add("neighborRouterId", neighborRouterId)
                .add("interfaceId", interfaceId)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultIsisRouter that = (DefaultIsisRouter) o;
        return Objects.equal(systemId, that.systemId) &&
                Objects.equal(neighborRouterId, that.neighborRouterId) &&
                Objects.equal(interfaceId, that.interfaceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(systemId, neighborRouterId, interfaceId);
    }
}