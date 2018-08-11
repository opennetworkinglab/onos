/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.api;

/**
 * Handles precommit request.
 */
public interface PreCommitService<T, E, S> {

    /**
     * Subscribes pre-update event for the given subject inside the given class.
     *
     * @param subject       subject to subscribe
     * @param eventType     event type (update or remove)
     * @param className     target class name
     */
    void subscribePreCommit(T subject, E eventType, String className);

    /**
     * Unsubscribes pre-update event for the given subject inside the given class.
     *
     * @param subject       subject to unsubscribe
     * @param eventType     event type (update or remove)
     * @param service       service instance
     * @param className     target class name
     */
    void unsubscribePreCommit(T subject, E eventType, S service, String className);

    /**
     * Obtains the count value of subscribers for the given subject and event type.
     *
     * @param subject       subject to subscribe
     * @param eventType     event type (update or remove)
     * @return subscriber count
     */
    int subscriberCountByEventType(T subject, E eventType);

    /**
     * Obtains the count value of subscribers for the given subject.
     *
     * @param subject       subject to subscribe
     * @return subscriber count
     */
    int subscriberCount(T subject);
}
