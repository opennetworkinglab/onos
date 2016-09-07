/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.mastership;

import org.joda.time.LocalDateTime;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

/**
 * Describes a device mastership event.
 */
public class MastershipEvent extends AbstractEvent<MastershipEvent.Type, DeviceId> {

    //Contains master and standby information.
    RoleInfo roleInfo;

    /**
     * Type of mastership events.
     */
    public enum Type {
        /**
         * Signifies that the master for a device has changed.
         */
        MASTER_CHANGED,

        /**
         * Signifies that the list of backup nodes has changed. If
         * the change in the backups list is accompanied by a change in
         * master, the event is subsumed by MASTER_CHANGED.
         */
        BACKUPS_CHANGED,

        /**
         * Signifies that the underlying storage for the Mastership state
         * of this device is unavailable.
         */
        SUSPENDED
    }

    /**
     * Creates an event of a given type and for the specified device,
     * role information, and the current time.
     *
     * @param type   mastership event type
     * @param device event device subject
     * @param info   mastership role information
     */
    public MastershipEvent(Type type, DeviceId device, RoleInfo info) {
        super(type, device);
        this.roleInfo = info;
    }

    /**
     * Creates an event of a given type and for the specified device, master,
     * and time.
     *
     * @param type   mastership event type
     * @param device event device subject
     * @param info   role information
     * @param time   occurrence time
     */
    public MastershipEvent(Type type, DeviceId device, RoleInfo info, long time) {
        super(type, device, time);
        this.roleInfo = info;
    }

    /**
     * Returns the current role state for the subject.
     *
     * @return RoleInfo associated with Device ID subject
     */
    public RoleInfo roleInfo() {
        return roleInfo;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("time", new LocalDateTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("roleInfo", roleInfo)
                .toString();
    }
}
