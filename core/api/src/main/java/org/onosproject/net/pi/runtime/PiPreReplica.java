/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.Objects;
import org.onosproject.net.PortNumber;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Representation of a packet replica used for multicast or cloning process in a
 * protocol-independent packet replication engine.
 * <p>
 * Each replica is uniquely identified inside a given multicast group or clone
 * session by the pair (egress port, instance ID).
 */
@Beta
public final class PiPreReplica {

    private final PortNumber egressPort;
    private final int instanceId;

    /**
     * Returns a new PRE packet replica for the given egress port and instance
     * ID.
     *
     * @param egressPort egress port
     * @param instanceId instance ID
     */
    public PiPreReplica(PortNumber egressPort, int instanceId) {
        this.egressPort = checkNotNull(egressPort);
        this.instanceId = instanceId;
    }

    /**
     * Returns the egress port of this replica.
     *
     * @return egress port
     */
    public PortNumber egressPort() {
        return egressPort;
    }

    /**
     * Returns the instance ID of this replica.
     *
     * @return instance ID
     */
    public int instanceId() {
        return instanceId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(egressPort, instanceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiPreReplica other = (PiPreReplica) obj;
        return Objects.equal(this.egressPort, other.egressPort)
                && Objects.equal(this.instanceId, other.instanceId);
    }

    @Override
    public String toString() {
        return format("%s:0x%s", egressPort, Integer.toHexString(instanceId));
    }
}
