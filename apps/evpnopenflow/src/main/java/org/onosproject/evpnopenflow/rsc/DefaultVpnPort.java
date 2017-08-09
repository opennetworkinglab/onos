/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.evpnopenflow.rsc;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.ID_CANNOT_BE_NULL;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_ID;
import static org.onosproject.evpnopenflow.rsc.EvpnConstants.VPN_INSTANCE_ID_CANNOT_BE_NULL;

/**
 * Default implementation of VPN port.
 */
public class DefaultVpnPort implements VpnPort {

    private final VpnPortId id;
    private final VpnInstanceId vpnInstanceId;

    /**
     * creates vpn port object.
     *
     * @param id            vpn port id
     * @param vpnInstanceId vpn instance id
     */
    public DefaultVpnPort(VpnPortId id, VpnInstanceId vpnInstanceId) {
        this.id = checkNotNull(id, ID_CANNOT_BE_NULL);
        this.vpnInstanceId = checkNotNull(vpnInstanceId,
                                          VPN_INSTANCE_ID_CANNOT_BE_NULL);
    }

    @Override
    public VpnPortId id() {
        return id;
    }

    @Override
    public VpnInstanceId vpnInstanceId() {
        return vpnInstanceId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, vpnInstanceId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultVpnPort) {
            final DefaultVpnPort that = (DefaultVpnPort) obj;
            return Objects.equals(this.id, that.id)
                    && Objects.equals(this.vpnInstanceId, that.vpnInstanceId);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add(ID, id)
                .add(VPN_INSTANCE_ID, vpnInstanceId).toString();
    }
}
