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

import com.google.common.annotations.Beta;
import org.onosproject.net.Port;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentId;

import java.util.Set;

/**
 * Service for providing device resources.
 */
@Beta
public interface DeviceResourceService {
    /**
     * Request a set of ports needed to satisfy the intent.
     *
     * @param ports set of ports to allocate
     * @param intent the intent
     * @return true if ports were successfully allocated, false otherwise
     */
    boolean requestPorts(Set<Port> ports, Intent intent);

    /**
     * Returns the set of ports allocated for an intent.
     *
     * @param intentId the intent ID
     * @return set of allocated ports
     */
    Set<Port> getAllocations(IntentId intentId);

    /**
     * Returns the intent allocated to a port.
     *
     * @param port the port
     * @return intent ID allocated to the port
     */
    IntentId getAllocations(Port port);

    /**
     * Request a mapping between the given intents.
     *
     * @param keyIntentId the key intent ID
     * @param valIntentId the value intent ID
     * @return true if mapping was successful, false otherwise
     */
    boolean requestMapping(IntentId keyIntentId, IntentId valIntentId);

    /**
     * Returns the intents mapped to a lower intent.
     *
     * @param intentId the intent ID
     * @return the set of intent IDs
     */
    Set<IntentId> getMapping(IntentId intentId);

    /**
     * Release mapping of given intent.
     *
     * @param intentId intent ID
     */
    void releaseMapping(IntentId intentId);

    /**
     * Release ports associated with given intent ID.
     *
     * @param intentId intent ID
     */
    void releasePorts(IntentId intentId);
}
