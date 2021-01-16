/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import io.fabric8.kubernetes.api.model.Pod;
import org.onosproject.event.AbstractEvent;

/**
 * Kubevirt pod event class.
 */
public class KubevirtPodEvent extends AbstractEvent<KubevirtPodEvent.Type, Pod> {

    /**
     * Creates an event of a given type for the specified kubevirt pod.
     *
     * @param type      kubevirt pod event type
     * @param subject   kubevirt pod
     */
    public KubevirtPodEvent(Type type, Pod subject) {
        super(type, subject);
    }

    public enum Type {
        /**
         * Signifies that a new kubevirt pod is created.
         */
        KUBEVIRT_POD_CREATED,

        /**
         * Signifies that the kubevirt pod is updated.
         */
        KUBEVIRT_POD_UPDATED,

        /**
         * Signifies that the kubevirt pod is in Pending phase.
         */
        KUBEVIRT_POD_PENDING,

        /**
         * Signifies that the kubevirt pod is in Running phase.
         */
        KUBEVIRT_POD_RUNNING,

        /**
         * Signifies that the kubevirt pod is in Succeeded phase.
         */
        KUBEVIRT_POD_SUCCEEDED,

        /**
         * Signifies that the kubevirt pod is in Failed phase.
         */
        KUBEVIRT_POD_FAILED,

        /**
         * Signifies that the kubevirt pod is in Unknown phase.
         */
        KUBEVIRT_POD_UNKNOWN,

        /**
         * Signifies that the kubevirt pod is in Completed phase.
         */
        KUBEVIRT_POD_COMPLETED,

        /**
         * Signifies that the kubevirt pod is in CrashLoopBackOff phase.
         */
        KUBEVIRT_POD_CRASH_LOOP_BACK_OFF,

        /**
         * Signifies that the kubevirt pod annotation is added.
         */
        KUBEVIRT_POD_ANNOTATION_ADDED,

        /**
         * Signifies that the kubevirt pod is removed.
         */
        KUBEVIRT_POD_REMOVED,
    }
}
