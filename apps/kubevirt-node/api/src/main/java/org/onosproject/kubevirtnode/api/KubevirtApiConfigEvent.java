/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import org.onosproject.event.AbstractEvent;

/**
 * Describes KubeVirt API server config event.
 */
public class KubevirtApiConfigEvent extends AbstractEvent<KubevirtApiConfigEvent.Type, KubevirtApiConfig> {

    /**
     * Lists of KubeVirt API server config event types.
     */
    public enum Type {
        /**
         * Signifies that API config is created.
         */
        KUBEVIRT_API_CONFIG_CREATED,

        /**
         * Signifies that API config is updated.
         */
        KUBEVIRT_API_CONFIG_UPDATED,

        /**
         * Signifies that API config is removed.
         */
        KUBEVIRT_API_CONFIG_REMOVED,
    }

    /**
     * Creates an event with the given type and node.
     *
     * @param type event type
     * @param subject KubeVirt API config
     */
    public KubevirtApiConfigEvent(Type type, KubevirtApiConfig subject) {
        super(type, subject);
    }
}
