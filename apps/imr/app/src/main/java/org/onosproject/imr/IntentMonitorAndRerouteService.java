/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.imr;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.core.ApplicationId;
import org.onosproject.imr.data.Route;
import org.onosproject.net.ElementId;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.intent.Key;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intent Monitor and Reroute ONOS service.
 */
public interface IntentMonitorAndRerouteService {

    /**
     * Starts to monitor an intent.
     * If the intent is not already submitted to the intent subsystem
     * it memorizes the key and it will start to monitor it as soon as it will be installed
     * @param intentKey Key of the intent to monitor
     * @return true, false only if the intent is of one of the not currently supported type
     */
    boolean startMonitorIntent(Key intentKey);

    /**
     * Stops to monitor an intent.
     * @param intentKey Key of the intent you want to stop the monitoring.
     * @return false if the intent key passed is not one of the tracked intent, true otherwise.
     */
    boolean stopMonitorIntent(Key intentKey);

    /**
     * Applies a new route to a monitored intent.
     * @param route Route you want to apply.
     * @return False in case Application ID or Intent Key are not tracked by IMR
     */
    boolean applyPath(Route route);

    /**
     * Returns the statistics of all the monitored intents.
     * @return Statistics (in terms of flow entries) of all the monitored intents.
     */
    Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats();

    /**
     * Returns the statistics of all the monitored intents submitted by a specific application.
     * @param appId Application id of the monitored intent.
     * @return Statistics (in terms of flow entries) of the requested intents.
     */
    Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats(ApplicationId appId);

    /**
     * Returns the statistics of a specific monitored intent.
     * @param appId Application id of the monitored intent.
     * @param intentKey key of the monitored intent.
     * @return Statistics (in terms of flow entries) of the requested intent.
     */
    Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats(ApplicationId appId, Key intentKey);

    /**
     * Returns the monitored intents in terms of key and connect points.
     * @return Intents monitored identified by the application id and
     * the intent key, plus the endpoints of that intent.
     */
    Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> getMonitoredIntents();

    /**
     * Returns the monitored intents submitted by a specific application.
     * @param appId Application id of the application to extract the monitored intents.
     * @return Intents monitored identified by the application id and
     * the intent key, plus the endpoints of that intent.
     */
    Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> getMonitoredIntents(ApplicationId appId);
}
