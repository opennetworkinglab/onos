/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import org.onlab.packet.IpAddress;

import java.util.Objects;

/**
 * Kubevirt IP Pool.
 */
public class KubevirtIpPool {

    private final IpAddress start;
    private final IpAddress end;

    /**
     * Default constructor.
     *
     * @param start     start address of IP pool
     * @param end       end address of IP pool
     */
    public KubevirtIpPool(IpAddress start, IpAddress end) {
        this.start = start;
        this.end = end;
    }

    /**
     * Returns the start address of IP pool.
     *
     * @return start address of IP pool
     */
    public IpAddress getStart() {
        return start;
    }

    /**
     * Returns the end address of IP pool.
     *
     * @return end address of IP pool
     */
    public IpAddress getEnd() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KubevirtIpPool that = (KubevirtIpPool) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("start", start)
                .add("end", end)
                .toString();
    }
}
