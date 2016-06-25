/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import java.util.Objects;

import org.onlab.packet.IpAddress;

/**
 * The continuous IP address range between the start address and the end address
 * for the allocation pools.
 */
public final class DefaultAllocationPool implements AllocationPool {

    private final IpAddress startIp;
    private final IpAddress endIp;

    /**
     * Creates an AllocationPool by using the start IP address and the end IP
     * address.
     *
     * @param startIp the start IP address of the allocation pool
     * @param endIp the end IP address of the allocation pool
     */
    public DefaultAllocationPool(IpAddress startIp, IpAddress endIp) {
        checkNotNull(startIp, "StartIp cannot be null");
        checkNotNull(endIp, "EndIp cannot be null");
        this.startIp = startIp;
        this.endIp = endIp;
    }

    @Override
    public IpAddress startIp() {
        return startIp;
    }

    @Override
    public IpAddress endIp() {
        return endIp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startIp, endIp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultAllocationPool) {
            final DefaultAllocationPool other = (DefaultAllocationPool) obj;
            return Objects.equals(this.startIp, other.startIp)
                    && Objects.equals(this.endIp, other.endIp);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("startIp", startIp).add("endIp", endIp)
                .toString();
    }
}

