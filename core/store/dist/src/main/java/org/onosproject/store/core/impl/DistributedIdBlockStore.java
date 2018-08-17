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
package org.onosproject.store.core.impl;

import com.google.common.collect.Maps;
import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code IdBlockStore} using {@code AtomicCounter}.
 */
@Component(immediate = true, service = IdBlockStore.class)
public class DistributedIdBlockStore implements IdBlockStore {

    private final Logger log = getLogger(getClass());
    private final Map<String, AtomicCounter> topicCounters = Maps.newConcurrentMap();

    private static final long DEFAULT_BLOCK_SIZE = 0x100000L;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public IdBlock getIdBlock(String topic) {
        AtomicCounter counter = topicCounters.computeIfAbsent(topic, storageService::getAtomicCounter);
        return new IdBlock(counter.getAndAdd(DEFAULT_BLOCK_SIZE), DEFAULT_BLOCK_SIZE);
    }
}
