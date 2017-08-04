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
package org.onosproject.net.intent;

import org.onosproject.net.NetworkResource;

import java.util.Collection;

/**
 * Auxiliary service for tracking intent path flows and for notifying the
 * intent service of environment changes via topology change delegate.
 */
public interface ObjectiveTrackerService {

    /**
     * Sets a topology change delegate.
     *
     * @param delegate topology change delegate
     */
    void setDelegate(TopologyChangeDelegate delegate);

    /**
     * Unsets topology change delegate.
     *
     * @param delegate topology change delegate
     */
    void unsetDelegate(TopologyChangeDelegate delegate);

    /**
     * Adds a path flow to be tracked.
     *
     * @param intentKey  intent identity on whose behalf the path is being tracked
     * @param resources resources to track
     */
    // TODO consider using the IntentData here rather than just the key
    void addTrackedResources(Key intentKey,
                                    Collection<NetworkResource> resources);

    /**
     * Removes a path flow to be tracked.
     *
     * @param intentKey  intent identity on whose behalf the path is being tracked
     * @param resources resources to stop tracking
     */
    void removeTrackedResources(Key intentKey,
                                       Collection<NetworkResource> resources);

    /**
     * Submits the specified intent data to be tracked.
     *
     * @param intentData intent data object to be tracked
     */
    void trackIntent(IntentData intentData);
}
