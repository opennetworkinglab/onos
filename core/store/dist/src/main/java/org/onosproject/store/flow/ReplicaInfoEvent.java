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
package org.onosproject.store.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;

/**
 * Describes a device replicainfo event.
 */
public class ReplicaInfoEvent extends AbstractEvent<ReplicaInfoEvent.Type, DeviceId> {

    private final ReplicaInfo replicaInfo;

    /**
     * Types of Replica info event.
     */
    public enum Type {
        /**
         * Event to notify that master placement should be changed.
         */
        MASTER_CHANGED,
        //
        BACKUPS_CHANGED,
    }


    /**
     * Creates an event of a given type and for the specified device,
     * and replica info.
     *
     * @param type        replicainfo event type
     * @param device      event device subject
     * @param replicaInfo replicainfo
     */
    public ReplicaInfoEvent(Type type, DeviceId device, ReplicaInfo replicaInfo) {
        super(type, device);
        this.replicaInfo = checkNotNull(replicaInfo);
    }

    /**
     * Returns the current replica information for the subject.
     *
     * @return replica information for the subject
     */
    public ReplicaInfo replicaInfo() {
        return replicaInfo;
    }
}
