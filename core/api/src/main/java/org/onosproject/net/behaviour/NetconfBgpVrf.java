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
package org.onosproject.net.behaviour;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

/**
 * Represent the object for the xml element of bgpVrf.
 */
public class NetconfBgpVrf {
    private final String operation;
    private final String vrfName;
    private final NetconfBgpVrfAFs bgpVrfAFs;

    /**
     * NetconfBgpVrf constructor.
     *
     * @param operation operation
     * @param vrfName vrf name
     * @param bgpVrfAFs NetconfBgpVrfAFs
     */
    public NetconfBgpVrf(String operation, String vrfName,
                         NetconfBgpVrfAFs bgpVrfAFs) {
        checkNotNull(operation, "operation cannot be null");
        checkNotNull(vrfName, "vrfName cannot be null");
        checkNotNull(bgpVrfAFs, "bgpVrfAFs cannot be null");
        this.operation = operation;
        this.vrfName = vrfName;
        this.bgpVrfAFs = bgpVrfAFs;
    }

    /**
     * Returns operation.
     *
     * @return operation
     */
    public String operation() {
        return operation;
    }

    /**
     * Returns vrfName.
     *
     * @return vrfName
     */
    public String vrfName() {
        return vrfName;
    }

    /**
     * Returns bgpVrfAFs.
     *
     * @return bgpVrfAFs
     */
    public NetconfBgpVrfAFs bgpVrfAFs() {
        return bgpVrfAFs;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operation, vrfName, bgpVrfAFs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfBgpVrf) {
            final NetconfBgpVrf other = (NetconfBgpVrf) obj;
            return Objects.equals(this.operation, other.operation)
                    && Objects.equals(this.vrfName, other.vrfName)
                    && Objects.equals(this.bgpVrfAFs, other.bgpVrfAFs);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("operation", operation)
                .add("vrfName", vrfName).add("bgpVrfAFs", bgpVrfAFs).toString();
    }
}
