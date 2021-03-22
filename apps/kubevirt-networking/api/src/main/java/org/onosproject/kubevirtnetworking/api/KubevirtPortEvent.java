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

import org.onlab.util.Tools;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Kubevirt port event class.
 */
public class KubevirtPortEvent extends AbstractEvent<KubevirtPortEvent.Type, KubevirtPort> {

    private final String securityGroupId;
    private final DeviceId deviceId;

    /**
     * Creates an event of a given type for the specified port.
     *
     * @param type      kubevirt port event type
     * @param subject   kubevirt port subject
     */
    public KubevirtPortEvent(Type type, KubevirtPort subject) {
        super(type, subject);
        securityGroupId = null;
        deviceId = null;
    }

    /**
     * Creates an event of a given type for the specified port.
     *
     * @param type              kubevirt port event type
     * @param subject           kubevirt port subject
     * @param securityGroupId   kubevirt security group ID
     */
    public KubevirtPortEvent(Type type, KubevirtPort subject, String securityGroupId) {
        super(type, subject);
        this.securityGroupId = securityGroupId;
        this.deviceId = null;
    }

    /**
     * Creates an event of a given type for the specified port.
     *
     * @param type              kubevirt port event type
     * @param subject           kubevirt port subject
     * @param deviceId          kubevirt device ID
     */
    public KubevirtPortEvent(Type type, KubevirtPort subject, DeviceId deviceId) {
        super(type, subject);
        this.deviceId = deviceId;
        this.securityGroupId = null;
    }

    /**
     * Kubevirt port events.
     */
    public enum Type {

        /**
         * Signifies that a new kubevirt port is created.
         */
        KUBEVIRT_PORT_CREATED,

        /**
         * Signifies that the kubevirt port is updated.
         */
        KUBEVIRT_PORT_UPDATED,

        /**
         * Signifies that the kubevirt port is removed.
         */
        KUBEVIRT_PORT_REMOVED,

        /**
         * Signifies that the kubevirt device is added.
         */
        KUBEVIRT_PORT_DEVICE_ADDED,

        /**
         * Signifies that the kubevirt security group rule is added to a specific port.
         */
        KUBEVIRT_PORT_SECURITY_GROUP_ADDED,

        /**
         * Signifies that the kubevirt security group rule is removed from a specific port.
         */
        KUBEVIRT_PORT_SECURITY_GROUP_REMOVED,
    }

    /**
     * Returns the security group rule IDs updated.
     *
     * @return edgestack security group
     */
    public String securityGroupId() {
        return securityGroupId;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("port", subject())
                .add("security group", securityGroupId())
                .toString();
    }
}
