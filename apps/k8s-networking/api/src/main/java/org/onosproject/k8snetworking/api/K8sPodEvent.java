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
package org.onosproject.k8snetworking.api;

import io.fabric8.kubernetes.api.model.Pod;
import org.onosproject.event.AbstractEvent;

public class K8sPodEvent extends AbstractEvent<K8sPodEvent.Type, Pod> {

    /**
     * Creates an event of a given type for the specified kubernetes pod.
     *
     * @param type      kubernetes pod event type
     * @param subject   kubernetes pod
     */
    protected K8sPodEvent(Type type, Pod subject) {
        super(type, subject);
    }

    /**
     * Kubernetes pod events.
     */
    public enum Type {

        /**
         * Signifies that a new kubernetes pod is created.
         */
        K8S_POD_CREATED,

        /**
         * Signifies that the kubernetes pod is updated.
         */
        K8S_POD_UPDATED,

        /**
         * Signifies that the kubernetes pod is removed.
         */
        K8S_POD_REMOVED,
    }
}
