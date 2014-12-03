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
package org.onosproject.store.cluster.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

//Not used right now
public final class ClusterManagementMessageSubjects {
    // avoid instantiation
    private ClusterManagementMessageSubjects() {}

    public static final MessageSubject CLUSTER_MEMBERSHIP_EVENT = new MessageSubject("CLUSTER_MEMBERSHIP_EVENT");
}
