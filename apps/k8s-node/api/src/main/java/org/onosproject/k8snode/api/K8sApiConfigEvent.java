/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes kubernetes API server config event.
 */
public class K8sApiConfigEvent extends AbstractEvent<K8sApiConfigEvent.Type, K8sApiConfig> {

    /**
     * Lists of kubernetes API server config event types.
     */
    public enum Type {
        /**
         * Signifies that API config is created.
         */
        K8S_API_CONFIG_CREATED,

        /**
         * Signifies that API config is updated.
         */
        K8S_API_CONFIG_UPDATED,

        /**
         * Signifies that API config is removed.
         */
        K8S_API_CONFIG_REMOVED,
    }

    /**
     * Creates an event with the given type and node.
     *
     * @param type event type
     * @param subject kubernetes API config
     */
    public K8sApiConfigEvent(Type type, K8sApiConfig subject) {
        super(type, subject);
    }
}
