/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.group;

import org.onosproject.event.AbstractEvent;

/**
 * Describes group events.
 */
public class GroupEvent extends AbstractEvent<GroupEvent.Type, Group> {

    /**
     * Type of flow rule events.
     */
    public enum Type {
        /**
         * Signifies that a new Group has been detected.
         */
        GROUP_ADDED,

        /**
         * Signifies that a Group has been removed.
         */
        GROUP_REMOVED,

        /**
         * Signifies that a Group has been updated.
         */
        GROUP_UPDATED,

        /**
         * Signifies that a request to create Group has failed.
         */
        GROUP_ADD_FAILED,

        /**
         * Signifies that a request to remove Group has failed.
         */
        GROUP_REMOVE_FAILED,

        /**
         * Signifies that a request to update Group has failed.
         */
        GROUP_UPDATE_FAILED,

        /**
         * Signifies change in the first live bucket in failover group
         * (i.e. change in which bucket is in use).
         * Only to be used with failover Group.
         */
        GROUP_BUCKET_FAILOVER,

        // internal event between Manager <-> Store

        /*
         * Signifies that a request to create Group has been added to the store.
         */
        GROUP_ADD_REQUESTED,
        /*
         * Signifies that a request to update Group has been added to the store.
         */
        GROUP_UPDATE_REQUESTED,
        /*
         * Signifies that a request to delete Group has been added to the store.
         */
        GROUP_REMOVE_REQUESTED,


    }

    /**
     * Creates an event of a given type and for the specified Group and the
     * current time.
     *
     * @param type  Group event type
     * @param group event subject
     */
    public GroupEvent(Type type, Group group) {
        super(type, group);
    }

    /**
     * Creates an event of a given type and for the specified Group and time.
     *
     * @param type  Group event type
     * @param group event subject
     * @param time  occurrence time
     */
    public GroupEvent(Type type, Group group, long time) {
        super(type, group, time);
    }

}
