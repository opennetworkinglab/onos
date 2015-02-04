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

/**
 * Service for tracking and delegating batches of intent operations.
 */
@Deprecated
public interface IntentBatchService {

    /**
     * Return true if this instance is the local leader for batch
     * processing a given application id.
     *
     * @param applicationId an application id
     * @return true if this instance is the local leader for batch
     */
    boolean isLocalLeader(ApplicationId applicationId);

    /**
     * Sets the batch service delegate.
     *
     * @param delegate delegate to apply
     */
    void setDelegate(IntentBatchDelegate delegate);

    /**
     * Unsets the batch service delegate.
     *
     * @param delegate delegate to unset
     */
    void unsetDelegate(IntentBatchDelegate delegate);

    /**
     * Adds the specified listener for intent batch leadership events.
     *
     * @param listener listener to be added
     */
    void addListener(IntentBatchListener listener);

    /**
     * Removes the specified listener for intent batch leadership events.
     *
     * @param listener listener to be removed
     */
    void removeListener(IntentBatchListener listener);
}
