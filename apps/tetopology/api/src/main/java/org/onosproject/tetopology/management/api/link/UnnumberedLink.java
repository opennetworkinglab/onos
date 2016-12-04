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

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

import java.util.Objects;

/**
 * Implementation of unnumbered link as an element type.
 */
public class UnnumberedLink implements ElementType {
    private final IpAddress routerId;
    private final long interfaceId;

    /**
     * Creates a unnumbered link.
     *
     * @param routerId    the router id to set
     * @param interfaceId the interface id to set
     */
    public UnnumberedLink(IpAddress routerId, long interfaceId) {
        this.routerId = routerId;
        this.interfaceId = interfaceId;
    }

    /**
     * Returns the router identifier.
     *
     * @return router id
     */
    public IpAddress routerId() {
        return routerId;
    }

    /**
     * Returns the interface identifier.
     *
     * @return interface id
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
