/*
 * Copyright 2014-present Open Networking Foundation
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

import org.onlab.util.Tools;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Describes a device mastership event.
 */
public class MastershipEvent extends AbstractEvent<MastershipEvent.Type, DeviceId> {

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
        SUSPENDED,

        /**
         * Signifies that the underlying storage for the Mastership state
         * of this device became available again.
         */
        RESTORED
    }

    private final MastershipInfo mastershipInfo;

    /**
     * Creates an event of a given type and for the specified device,
     * role information, and the current time.
     *
     * @param type           mastership event type
     * @param device         event device subject
     * @param mastershipInfo mastership info
     */
    public MastershipEvent(Type type, DeviceId device, MastershipInfo mastershipInfo) {
        super(type, device);
        this.mastershipInfo = mastershipInfo;
    }

    /**
     * Creates an event of a given type and for the specified device, master,
     * and time.
     *
     * @param type           mastership event type
     * @param device         event device subject
     * @param mastershipInfo mastership information
     * @param time           occurrence time
     */
    public MastershipEvent(Type type, DeviceId device, MastershipInfo mastershipInfo, long time) {
        super(type, device, time);
        this.mastershipInfo = mastershipInfo;
    }

    /**
     * Returns the mastership info.
     *
     * @return the mastership info
     */
    public MastershipInfo mastershipInfo() {
        return mastershipInfo;
    }

    /**
     * Returns the current role state for the subject.
     *
     * @return RoleInfo associated with Device ID subject
     * @deprecated since 1.14
     */
    @Deprecated
    public RoleInfo roleInfo() {
        return new RoleInfo(mastershipInfo.master().orElse(null), mastershipInfo.backups());
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), mastershipInfo(), time());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MastershipEvent) {
            final MastershipEvent other = (MastershipEvent) obj;
            return Objects.equals(this.type(), other.type()) &&
                    Objects.equals(this.subject(), other.subject()) &&
                    Objects.equals(this.mastershipInfo(), other.mastershipInfo()) &&
                    Objects.equals(this.time(), other.time());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("time", Tools.defaultOffsetDataTime(time()))
                .add("type", type())
                .add("subject", subject())
                .add("mastershipInfo", mastershipInfo())
                .toString();
    }
}
