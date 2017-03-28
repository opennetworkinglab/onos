/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.netconf.client;

import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.net.DeviceId;
import org.onosproject.yang.model.ResourceId;


/**
 * Event details.
 */
public class EventData {

    private DeviceId devId;
    private ResourceId key;
    private DynamicConfigEvent.Type type;

    /**
     * Creates an instance of EventData.
     *
     * @param devId device id
     * @param key device key
     * @param type event type
     */
    public EventData(DeviceId devId, ResourceId key, DynamicConfigEvent.Type type) {
        devId = devId;
        key = key;
        type = type;
    }

    public DeviceId getDevId() {
        return devId;
    }

    public ResourceId getKey() {
        return key;
    }

    public DynamicConfigEvent.Type getType() {
        return type;
    }
}