/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.link.impl;

        import org.onosproject.store.cluster.messaging.MessageSubject;

/**
 * MessageSubjects used by GossipLinkStore peer-peer communication.
 */
public final class GossipLinkStoreMessageSubjects {

    private GossipLinkStoreMessageSubjects() {}

    public static final MessageSubject LINK_UPDATE =
            new MessageSubject("peer-link-update");
    public static final MessageSubject LINK_REMOVED =
            new MessageSubject("peer-link-removed");
    public static final MessageSubject LINK_ANTI_ENTROPY_ADVERTISEMENT =
            new MessageSubject("link-enti-entropy-advertisement");
    public static final MessageSubject LINK_INJECTED =
            new MessageSubject("peer-link-injected");
}
