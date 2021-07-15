/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.store.flow.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by ECFlowRuleStore peer-peer communication.
 */
public final class ECFlowRuleStoreMessageSubjects {
    private ECFlowRuleStoreMessageSubjects() {}

    public static final  MessageSubject APPLY_BATCH_FLOWS
        = new MessageSubject("peer-forward-apply-batch");

    public static final MessageSubject GET_FLOW_ENTRY
        = new MessageSubject("peer-forward-get-flow-entry");

    public static final MessageSubject GET_DEVICE_FLOW_COUNT
        = new MessageSubject("peer-forward-get-flow-count");

    public static final MessageSubject REMOVE_FLOW_ENTRY
        = new MessageSubject("peer-forward-remove-flow-entry");

    public static final MessageSubject REMOTE_APPLY_COMPLETED
        = new MessageSubject("peer-apply-completed");

    public static final MessageSubject FLOW_TABLE_BACKUP
        = new MessageSubject("peer-flow-table-backup");

    public static final MessageSubject PURGE_FLOW_RULES
            = new MessageSubject("peer-purge-flow-rules");
}
