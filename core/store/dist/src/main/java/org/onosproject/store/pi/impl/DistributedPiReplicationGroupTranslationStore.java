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

package org.onosproject.store.pi.impl;

import org.onosproject.net.group.Group;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.service.PiReplicationGroupTranslationStore;
import org.osgi.service.component.annotations.Component;

/**
 * Distributed implementation of a PI translation store for groups that require
 * packet replication.
 */
@Component(immediate = true, service = PiReplicationGroupTranslationStore.class)
public class DistributedPiReplicationGroupTranslationStore
        extends AbstractDistributedPiTranslationStore<Group, PiPreEntry>
        implements PiReplicationGroupTranslationStore {

    private static final String MAP_SIMPLE_NAME = "replication-group";

    @Override
    protected String mapSimpleName() {
        return MAP_SIMPLE_NAME;
    }
}
