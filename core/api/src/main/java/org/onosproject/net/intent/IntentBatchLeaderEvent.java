/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.onosproject.core.ApplicationId;
import org.onosproject.event.AbstractEvent;

/**
 * A class to represent an intent related event.
 */
public class IntentBatchLeaderEvent extends AbstractEvent<IntentBatchLeaderEvent.Type, ApplicationId> {

    public enum Type {
        /**
         * Signifies that this instance has become the leader for the given application id.
         */
        ELECTED,

        /**
         * Signifies that instance is no longer the leader for a given application id.
         */
        BOOTED
    }

    /**
     * Creates an event of a given type and for the specified appId and the
     * current time.
     *
     * @param type   event type
     * @param appId  subject appId
     * @param time   time the event created in milliseconds since start of epoch
     */
    public IntentBatchLeaderEvent(Type type, ApplicationId appId, long time) {
        super(type, appId, time);
    }

    /**
     * Creates an event of a given type and for the specified appId and the
     * current time.
     *
     * @param type   event type
     * @param appId subject appId
     */
    public IntentBatchLeaderEvent(Type type, ApplicationId appId) {
        super(type, appId);
    }

}
