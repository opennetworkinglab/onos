/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.cpu;

import org.onlab.util.Identifier;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Represents a CPU core ID.
 * This class is immutable.
 */
public final class CpuCoreId extends Identifier<Integer> {

    /**
     * A CPU core with this ID is shared.
     */
    private static final int SHARED = -1;

    /**
     * Upper limits of CPU core IDs and sockets in one machine.
     */
    public static final int MAX_CPU_CORE_NB = 512;
    public static final int MAX_CPU_SOCKET_NB = 4;

    private final int physicalId;

    /**
     * Constructor from integer values.
     *
     * @param logicalId the logical ID of this CPU core
     * @param physicalId the physical ID of this CPU core
     */
    public CpuCoreId(int logicalId, int physicalId) {
        super(logicalId);
        if (logicalId >= 0) {
            checkArgument(logicalId < MAX_CPU_CORE_NB,
                "Logical CPU core ID must be in [0, " +
                String.valueOf(CpuCoreId.MAX_CPU_CORE_NB - 1) + "]");
        }
        this.physicalId = physicalId;
    }

    /**
     * Constructor from a string.
     *
     * @param logicalIdStr the logical ID of this CPU core as a string
     * @param physicalIdStr the physical ID of this CPU core as a string
     */
    public CpuCoreId(String logicalIdStr, String physicalIdStr) {
        this(Integer.parseInt(logicalIdStr), Integer.parseInt(physicalIdStr));
    }

    /**
     * Static constructor of a shared CPU core ID.
     *
     * @return a shared CPU core ID
     */
    public static CpuCoreId shared() {
        return new CpuCoreId(SHARED, SHARED);
    }

    /**
     * Get the logical ID of this CPU core.
     *
     * @return logical ID of this CPU core
     */
    public int logicalId() {
        return identifier;
    }

    /**
     * Get the physical ID of this CPU core.
     *
     * @return physical ID of this CPU core
     */
    public int physicalId() {
        return physicalId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .omitNullValues()
                .add("logicalId",  logicalId())
                .add("physicalId", physicalId())
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier, physicalId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CpuCoreId)) {
            return false;
        }
        CpuCoreId coreId = (CpuCoreId) obj;
        return  this.logicalId() ==  coreId.logicalId() &&
                this.physicalId() == coreId.physicalId();
    }

}
