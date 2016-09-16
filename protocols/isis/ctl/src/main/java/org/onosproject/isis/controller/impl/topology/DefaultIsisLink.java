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
import org.onosproject.isis.controller.topology.IsisLink;
import org.onosproject.isis.controller.topology.IsisLinkTed;

/**
 * Representation of an ISIS Link.
 */
public class DefaultIsisLink implements IsisLink {

    private String remoteSystemId;
    private String localSystemId;
    private Ip4Address interfaceIp;
    private Ip4Address neighborIp;
    private IsisLinkTed linkTed;

    @Override
    public String remoteSystemId() {
        return this.remoteSystemId;
    }

    @Override
    public String localSystemId() {
        return this.localSystemId;
    }

    @Override
    public Ip4Address interfaceIp() {
        return this.interfaceIp;
    }

    @Override
    public Ip4Address neighborIp() {
        return this.neighborIp;
    }

    @Override
    public IsisLinkTed linkTed() {
        return this.linkTed;
    }

    @Override
    public void setRemoteSystemId(String remoteSystemId) {
        this.remoteSystemId = remoteSystemId;
    }

    @Override
    public void setLocalSystemId(String localSystemId) {
        this.localSystemId = localSystemId;
    }

    @Override
    public void setInterfaceIp(Ip4Address interfaceIp) {
        this.interfaceIp = interfaceIp;
    }

    @Override
    public void setNeighborIp(Ip4Address neighborIp) {
        this.neighborIp = neighborIp;
    }

    @Override
    public void setLinkTed(IsisLinkTed linkTed) {
        this.linkTed = linkTed;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("remoteSystemId", remoteSystemId)
                .add("localSystemId", localSystemId)
                .add("interfaceIp", interfaceIp)
                .add("neighborIp", neighborIp)
                .add("linkTed", linkTed)
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
        DefaultIsisLink that = (DefaultIsisLink) o;
        return Objects.equal(remoteSystemId, that.remoteSystemId) &&
                Objects.equal(localSystemId, that.localSystemId) &&
                Objects.equal(interfaceIp, that.interfaceIp) &&
                Objects.equal(neighborIp, that.neighborIp) &&
                Objects.equal(linkTed, that.linkTed);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(remoteSystemId, localSystemId, interfaceIp, neighborIp, linkTed);
    }
}