/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.net.config.basics;

import com.google.common.annotations.Beta;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;

/**
 * Configuration for multicast.
 */
@Beta
public class McastConfig extends Config<ApplicationId> {
    private static final String INGRESS_VLAN = "ingressVlan";
    private static final String EGRESS_VLAN = "egressVlan";
    private static final String EGRESS_INNER_VLAN = "egressInnerVlan";

    @Override
    public boolean isValid() {
        return hasOnlyFields(INGRESS_VLAN, EGRESS_VLAN, EGRESS_INNER_VLAN) &&
                ingressVlan() != null && egressVlan() != null && egressInnerVlan() != null;
    }

    /**
     * Gets ingress VLAN of multicast traffic.
     *
     * @return Ingress VLAN ID
     */
    public VlanId ingressVlan() {
        if (!object.has(INGRESS_VLAN)) {
            return VlanId.NONE;
        }

        try {
            return VlanId.vlanId(object.path(INGRESS_VLAN).asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sets ingress VLAN of multicast traffic.
     *
     * @param vlanId Ingress VLAN ID
     * @return this {@link McastConfig}
     */
    public McastConfig setIngressVlan(VlanId vlanId) {
        if (vlanId == null) {
            object.remove(INGRESS_VLAN);
        } else {
            object.put(INGRESS_VLAN, vlanId.toString());
        }
        return this;
    }

    /**
     * Gets egress VLAN of multicast traffic.
     *
     * @return Egress VLAN ID
     */
    public VlanId egressVlan() {
        if (!object.has(EGRESS_VLAN)) {
            return VlanId.NONE;
        }

        try {
            return VlanId.vlanId(object.path(EGRESS_VLAN).asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sets egress VLAN of multicast traffic.
     *
     * @param vlanId Egress VLAN ID
     * @return this {@link McastConfig}
     */
    public McastConfig setEgressVlan(VlanId vlanId) {
        if (vlanId == null) {
            object.remove(EGRESS_VLAN);
        } else {
            object.put(EGRESS_VLAN, vlanId.toString());
        }
        return this;
    }

    /**
     * Gets egress inner VLAN of multicast traffic.
     *
     * @return Egress inner VLAN ID
     */
    public VlanId egressInnerVlan() {
        if (!object.has(EGRESS_INNER_VLAN)) {
            return VlanId.NONE;
        }

        try {
            return VlanId.vlanId(object.path(EGRESS_INNER_VLAN).asText());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Sets egress inner VLAN of multicast traffic.
     *
     * @param vlanId Egress inner VLAN ID
     * @return this {@link McastConfig}
     */
    public McastConfig setEgressInnerVlan(VlanId vlanId) {
        if (vlanId == null) {
            object.remove(EGRESS_INNER_VLAN);
        } else {
            object.put(EGRESS_INNER_VLAN, vlanId.toString());
        }
        return this;
    }
}
