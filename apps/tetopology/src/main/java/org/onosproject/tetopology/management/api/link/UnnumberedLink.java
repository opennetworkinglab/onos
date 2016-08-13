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
package org.onosproject.tetopology.management.api.link;

import java.util.Objects;

import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;

/**
 * Implementation of unnumbered link as an ElementType.
 */
public class UnnumberedLink implements ElementType {
    private IpAddress routerId;
    private long interfaceId;

    /**
     * Creates an instance of UnnumberedLink.
     */
    public UnnumberedLink() {
    }

    /**
     * Sets the router Id.
     *
     * @param routerId the routerId to set
     */
    public void setRouterId(IpAddress routerId) {
        this.routerId = routerId;
    }

    /**
     * Sets the interface Id.
     *
     * @param interfaceId the interfaceId to set
     */
    public void setInterfaceId(long interfaceId) {
        this.interfaceId = interfaceId;
    }

    /**
     * Returns the router Id.
     *
     * @return router identifier
     */
    public IpAddress routerId() {
        return routerId;
    }

    /**
     * Returns the interface Id.
     *
     * @return interface identifier
     */
    public long interfaceId() {
        return interfaceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(routerId, interfaceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof UnnumberedLink) {
            UnnumberedLink other = (UnnumberedLink) obj;
            return
                 Objects.equals(routerId, other.routerId) &&
                 Objects.equals(interfaceId, other.interfaceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("routerId", routerId)
            .add("interfaceId", interfaceId)
            .toString();
    }
}
