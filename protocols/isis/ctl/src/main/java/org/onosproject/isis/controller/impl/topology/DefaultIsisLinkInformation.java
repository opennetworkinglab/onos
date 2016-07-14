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
import org.onosproject.isis.controller.topology.LinkInformation;

/**
 * Representation of an ISIS link information..
 */
public class DefaultIsisLinkInformation implements LinkInformation {

    String linkId;
    String linkSourceId;
    String linkDestinationId;
    Ip4Address interfaceIp;
    Ip4Address neighborIp;
    boolean alreadyCreated;

    /**
     * Gets link id.
     *
     * @return link id
     */
    public String linkId() {
        return linkId;
    }

    /**
     * Sets link id.DefaultIsisDeviceInformation.
     *
     * @param linkId link id
     */
    public void setLinkId(String linkId) {
        this.linkId = linkId;
    }

    /**
     * Gets is already created or not.
     *
     * @return true if already created else false
     */
    public boolean isAlreadyCreated() {
        return alreadyCreated;
    }

    /**
     * Sets is already created or not.
     *
     * @param alreadyCreated true or false
     */
    public void setAlreadyCreated(boolean alreadyCreated) {
        this.alreadyCreated = alreadyCreated;
    }

    /**
     * Gets link destination id.
     *
     * @return link destination id
     */
    public String linkDestinationId() {
        return linkDestinationId;
    }

    /**
     * Sets link destination id.
     *
     * @param linkDestinationId link destination id
     */
    public void setLinkDestinationId(String linkDestinationId) {
        this.linkDestinationId = linkDestinationId;
    }

    /**
     * Gets link source id.
     *
     * @return link source id
     */
    public String linkSourceId() {
        return linkSourceId;
    }

    /**
     * Sets link source id.
     *
     * @param linkSourceId link source id
     */
    public void setLinkSourceId(String linkSourceId) {
        this.linkSourceId = linkSourceId;
    }

    /**
     * Gets interface IP address.
     *
     * @return interface IP address
     */
    public Ip4Address interfaceIp() {
        return interfaceIp;
    }

    /**
     * Sets interface IP address.
     *
     * @param interfaceIp interface IP address
     */
    public void setInterfaceIp(Ip4Address interfaceIp) {
        this.interfaceIp = interfaceIp;
    }

    @Override
    public Ip4Address neighborIp() {
        return this.neighborIp;
    }

    @Override
    public void setNeighborIp(Ip4Address neighborIp) {
        this.neighborIp = neighborIp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("linkId", linkId)
                .add("linkSourceId", linkSourceId)
                .add("linkDestinationId", linkDestinationId)
                .add("interfaceIp", interfaceIp)
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
        DefaultIsisLinkInformation that = (DefaultIsisLinkInformation) o;
        return Objects.equal(linkId, that.linkId) &&
                Objects.equal(linkSourceId, that.linkSourceId) &&
                Objects.equal(linkDestinationId, that.linkDestinationId) &&
                Objects.equal(interfaceIp, that.interfaceIp);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(linkId, linkSourceId, linkDestinationId,
                interfaceIp);
    }
}