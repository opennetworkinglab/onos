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

import static org.onosproject.k8snode.api.Constants.ROUTER_BRIDGE;

/**
 * K8s router bridge.
 */
public class K8sRouterBridge implements K8sBridge {

    private static final String OF_PREFIX = "of:";

    private final int segmentId;

    /**
     * Default constructor.
     *
     * @param segmentId  segment identifier
     */
    public K8sRouterBridge(int segmentId) {
        this.segmentId = segmentId;
    }

    /**
     * Returns segment ID.
     *
     * @return segment ID
     */
    public int segmentId() {
        return segmentId;
    }

    @Override
    public DeviceId deviceId() {
        return DeviceId.deviceId(dpid());
    }

    @Override
    public String dpid() {
        return genDpidFromName(name());
    }

    @Override
    public String name() {
        return ROUTER_BRIDGE + "-" + segmentId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        K8sRouterBridge that = (K8sRouterBridge) o;
        return segmentId == that.segmentId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(segmentId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("segmentId", segmentId)
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
