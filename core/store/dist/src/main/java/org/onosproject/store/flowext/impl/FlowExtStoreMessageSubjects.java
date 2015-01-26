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
package org.onosproject.store.flowext.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by DistributedFlowRuleStore peer-peer communication.
 */
public final class FlowExtStoreMessageSubjects {
    private FlowExtStoreMessageSubjects() {}
    public static final  MessageSubject APPLY_EXTEND_FLOWS
    = new MessageSubject("peer-forward-apply-batch-extension");

    public static final MessageSubject GET_DEVICE_EXTENDFLOW_ENTRIES
    = new MessageSubject("peer-forward-get-device-flow-entries-extension");
}
