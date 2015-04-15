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
package org.onosproject.store.host.impl;

import org.onosproject.store.cluster.messaging.MessageSubject;

public final class GossipHostStoreMessageSubjects {
    private GossipHostStoreMessageSubjects() {}

    public static final MessageSubject HOST_UPDATED_MSG
        = new MessageSubject("peer-host-updated");
    public static final MessageSubject HOST_REMOVED_MSG
        = new MessageSubject("peer-host-removed");
    public static final MessageSubject HOST_ANTI_ENTROPY_ADVERTISEMENT
        = new MessageSubject("host-enti-entropy-advertisement");;
}
