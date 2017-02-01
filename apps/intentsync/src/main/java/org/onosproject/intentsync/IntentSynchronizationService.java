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

package org.onosproject.intentsync;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.intent.Intent;

/**
 * Submits and withdraws intents to the IntentService from a single point in
 * the cluster at any one time. The provided intents will be synchronized with
 * the IntentService on leadership change.
 * <p>
 * This is a sample utility and not part of the core ONOS API. This means it is
 * subject to change or to be removed. It is recommended to consider using one
 * of the built-in ONOS distributed primitives such as the
 * {@link org.onosproject.store.service.WorkQueue} instead of using this.
 * </p>
 */
public interface IntentSynchronizationService {

    /**
     * Submits and intent to the synchronizer.
     * <p>
     * The intent will be submitted directly to the IntentService if this node
     * is the leader, otherwise it will be stored in the synchronizer for
     * synchronization if this node becomes the leader.
     * </p>
     *
     * @param intent intent to submit
     */
    void submit(Intent intent);

    /**
     * Withdraws an intent from the synchronizer.
     * <p>
     * The intent will be withdrawn directly from the IntentService if this node
     * is the leader. The intent will be removed from the synchronizer's
     * in-memory storage.
     * </p>
     *
     * @param intent intent to withdraw
     */
    void withdraw(Intent intent);

    /**
     * Withdraws intents by app Id.
     *
     * @param applicationId the Id of the application that created the intents
     *                      to be removed
     */
    void removeIntentsByAppId(ApplicationId applicationId);
}
