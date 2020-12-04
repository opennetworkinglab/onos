/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import com.google.common.collect.Maps;
import org.onosproject.net.group.Group;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiHandle;
import org.onosproject.net.pi.runtime.PiPreEntry;
import org.onosproject.net.pi.service.PiReplicationGroupTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;

import java.util.Map;
import java.util.Optional;

/**
 * Adapter implementation of PiGroupTranslator.
 */
public class PiReplicationGroupTranslatorAdapter implements PiReplicationGroupTranslator {

    private final Map<PiHandle, PiTranslatedEntity<Group, PiPreEntry>> store = Maps.newHashMap();

    @Override
    public PiPreEntry translate(Group original, PiPipeconf pipeconf) throws PiTranslationException {
        // NOTE Passing device equals to null is meant only for testing purposes
        return PiReplicationGroupTranslatorImpl.translate(original, pipeconf, null);
    }

    @Override
    public void learn(PiHandle handle, PiTranslatedEntity<Group, PiPreEntry> entity) {
        store.put(handle, entity);
    }

    @Override
    public Optional<PiTranslatedEntity<Group, PiPreEntry>> lookup(PiHandle handle) {
        return Optional.ofNullable(store.get(handle));
    }

    @Override
    public void forget(PiHandle handle) {
        store.remove(handle);
    }
}
