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
package org.onosproject.protocol.restconf;

import org.onosproject.net.DeviceId;

/**
 * Notifies providers about incoming RESTCONF notification events.
 */
public interface RestConfNotificationEventListener {

    /**
     * Handles the notification event.
     *
     * @param <T> entity type
     * @param deviceId of the restconf device
     * @param eventJsonString the json string representation of the event
     */
    <T> void handleNotificationEvent(DeviceId deviceId, T eventJsonString);

}
