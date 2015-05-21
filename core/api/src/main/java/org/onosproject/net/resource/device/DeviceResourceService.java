/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.resource.device;

import org.onosproject.net.Port;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;

import java.util.Set;

/**
 * Service for providing device resources.
 */
public interface DeviceResourceService {
    /**
     * Request a set of ports needed to satisfy the intent.
     *
     * @param intent the intent
     * @return set of ports
     */
    Set<Port> requestPorts(Intent intent);

    /**
     * Release ports associated with given intent ID.
     *
     * @param intentId intent ID
     */
    void releasePorts(IntentId intentId);
}
