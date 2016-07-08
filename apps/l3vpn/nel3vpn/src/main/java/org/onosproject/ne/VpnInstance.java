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
package org.onosproject.ne;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Objects;

/**
 * VpnInstance vpn instance.
 */
public class VpnInstance {
    private final String neId;
    private final List<VrfEntity> vrfList;

    /**
     * VrfEntity constructor.
     * 
     * @param neId ne identifier
     * @param vrfList list of vrf
     * @param routeDistinguisher route distinguisher
     * @param importTargets list of importTarget
     * @param exportTargets list of exportTarget
     * @param bgp bgp
     */
    public VpnInstance(String neId, List<VrfEntity> vrfList) {
        checkNotNull(neId, "neId cannot be null");
        checkNotNull(vrfList, "vrfList cannot be null");
        this.neId = neId;
        this.vrfList = vrfList;
    }

    /**
     * Returns neId.
     * 
     * @return neId
     */
    public String neId() {
        return neId;
    }

    /**
     * Returns vrfList.
     * 
     * @return vrfList
     */
    public List<VrfEntity> vrfList() {
        return vrfList;
    }

    @Override
    public int hashCode() {
        return Objects.hash(neId, vrfList);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof VpnInstance) {
            final VpnInstance other = (VpnInstance) obj;
            return Objects.equals(this.neId, other.neId)
                    && Objects.equals(this.vrfList, other.vrfList);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("neId", neId).add("vrfList", vrfList)
                .toString();
    }
}
