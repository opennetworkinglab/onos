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
 * Represent the object for the xml element of vpnInstAF.
 */
public class NetconfVpnInstAF {
    private final String afType;
    private final String vrfRD;
    private final NetconfVpnTargets vpnTargets;

    /**
     * NetconfVpnInstAF constructor.
     * 
     * @param afType address family Type
     * @param vrfRD vrfRD
     * @param vpnTargets vpn targets
     */
    public NetconfVpnInstAF(String afType, String vrfRD,
                            NetconfVpnTargets vpnTargets) {
        checkNotNull(afType, "afType cannot be null");
        checkNotNull(vrfRD, "vrfRD cannot be null");
        checkNotNull(vpnTargets, "vpnTargets cannot be null");
        this.afType = afType;
        this.vrfRD = vrfRD;
        this.vpnTargets = vpnTargets;
    }

    /**
     * Returns afType.
     * 
     * @return afType
     */
    public String afType() {
        return afType;
    }

    /**
     * Returns vrfRD.
     * 
     * @return vrfRD
     */
    public String vrfRD() {
        return vrfRD;
    }

    /**
     * Returns vpnTargets.
     * 
     * @return vpnTargets
     */
    public NetconfVpnTargets vpnTargets() {
        return vpnTargets;
    }

    @Override
    public int hashCode() {
        return Objects.hash(afType, vrfRD, vpnTargets);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof NetconfVpnInstAF) {
            final NetconfVpnInstAF other = (NetconfVpnInstAF) obj;
            return Objects.equals(this.afType, other.afType)
                    && Objects.equals(this.vrfRD, other.vrfRD)
                    && Objects.equals(this.vpnTargets, other.vpnTargets);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("afType", afType).add("vrfRD", vrfRD)
                .add("vpnTargets", vpnTargets).toString();
    }
}
