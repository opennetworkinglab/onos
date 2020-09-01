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
package org.onosproject.k8snode.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import org.onosproject.net.DeviceId;

import java.util.Objects;

import static org.onosproject.k8snode.api.Constants.GENEVE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.GRE_TUNNEL;
import static org.onosproject.k8snode.api.Constants.TUNNEL_BRIDGE;
import static org.onosproject.k8snode.api.Constants.VXLAN_TUNNEL;

/**
 * K8s tunnel bridge.
 */
public class K8sTunnelBridge {

    private static final String OF_PREFIX = "of:";

    private final int tunnelId;

    /**
     * Default constructor.
     *
     * @param tunnelId  tunnel identifier
     */
    public K8sTunnelBridge(int tunnelId) {
        this.tunnelId = tunnelId;
    }

    /**
     * Returns device identifier.
     *
     * @return device identifier
     */
    public DeviceId deviceId() {
        return DeviceId.deviceId(dpid());
    }

    /**
     * Returns tunnel ID.
     *
     * @return tunnel ID
     */
    public int tunnelId() {
        return tunnelId;
    }

    /**
     * Return the datapath identifier.
     *
     * @return datapath identifier
     */
    public String dpid() {
        return genDpidFromName(name());
    }

    /**
     * Returns bridge name.
     *
     * @return bridge name
     */
    public String name() {
        return TUNNEL_BRIDGE + "-" + tunnelId;
    }

    /**
     * Returns GRE port name.
     *
     * @return GRE port name
     */
    public String grePortName() {
        return GRE_TUNNEL + "-" + tunnelId;
    }

    /**
     * Returns VXLAN port name.
     *
     * @return VXLAN port name
     */
    public String vxlanPortName() {
        return VXLAN_TUNNEL + "-" + tunnelId;
    }

    /**
     * Returns GENEVE port name.
     *
     * @return GENEVE port name
     */
    public String genevePortName() {
        return GENEVE_TUNNEL + "-" + tunnelId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        K8sTunnelBridge that = (K8sTunnelBridge) o;
        return tunnelId == that.tunnelId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tunnelId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tunnelId", tunnelId)
                .toString();
    }

    private String genDpidFromName(String name) {
        if (name != null) {
            String hexString = Integer.toHexString(name.hashCode());
            return OF_PREFIX + Strings.padStart(hexString, 16, '0');
        }

        return null;
    }
}
