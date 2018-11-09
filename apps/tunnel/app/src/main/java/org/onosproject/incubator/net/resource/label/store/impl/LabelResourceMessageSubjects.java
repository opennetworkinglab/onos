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
package org.onosproject.incubator.net.resource.label.store.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

public final class LabelResourceMessageSubjects {

    private LabelResourceMessageSubjects() {
    }
    public static final MessageSubject LABEL_POOL_CREATED
                        = new MessageSubject("label-resource-pool-created");
    public static final MessageSubject LABEL_POOL_DESTROYED
                        = new MessageSubject("label-resource-pool-destroyed");
    public static final MessageSubject LABEL_POOL_APPLY
                        = new MessageSubject("label-resource-pool-apply");
    public static final MessageSubject LABEL_POOL_RELEASE
                        = new MessageSubject("label-resource-pool-release");
}
