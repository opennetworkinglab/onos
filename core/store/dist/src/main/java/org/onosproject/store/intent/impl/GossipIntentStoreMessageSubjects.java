/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.intent.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * Message subjects for internal gossip intent store node-to-node messages.
 */
public final class GossipIntentStoreMessageSubjects {
    private GossipIntentStoreMessageSubjects() {}

    public static final MessageSubject INTENT_UPDATED_MSG
            = new MessageSubject("peer-intent-updated");
    public static final MessageSubject INTENT_SET_INSTALLABLES_MSG
            = new MessageSubject("peer-intent-set-installables");
    public static final MessageSubject INTENT_ANTI_ENTROPY_ADVERTISEMENT
            = new MessageSubject("intent-anti-entropy-advertisement");
}
