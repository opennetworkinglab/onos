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
package org.onlab.onos.net.resource;

import java.util.Set;

import org.onlab.onos.net.Link;
import org.onlab.onos.net.intent.IntentId;


/**
 * Manages link resources.
 */
public interface LinkResourceStore {
    Set<ResourceAllocation> getFreeResources(Link link);

    void allocateResources(LinkResourceAllocations allocations);

    void releaseResources(LinkResourceAllocations allocations);

    LinkResourceAllocations getAllocations(IntentId intentId);

    Iterable<LinkResourceAllocations> getAllocations(Link link);

    Iterable<LinkResourceAllocations> getAllocations();
}
