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
package org.onosproject.store.core.impl;

import com.google.common.collect.Maps;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.core.IdBlock;
import org.onosproject.core.IdBlockStore;
import org.onosproject.store.service.AtomicCounter;
import org.onosproject.store.service.StorageException;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;

import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of {@code IdBlockStore} using {@code AtomicCounter}.
 */
@Component(immediate = true, enabled = true)
@Service
public class ConsistentIdBlockStore implements IdBlockStore {

    private static final int MAX_TRIES = 5;
    private static final int RETRY_DELAY_MS = 2_000;

    private final Logger log = getLogger(getClass());
    private final Map<String, AtomicCounter> topicCounters = Maps.newConcurrentMap();

    private static final long DEFAULT_BLOCK_SIZE = 0x100000L;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
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
        AtomicCounter counter = topicCounters
                .computeIfAbsent(topic,
                                 name -> storageService.atomicCounterBuilder()
                                         .withName(name)
                                         .build());
        Long blockBase = Tools.retryable(counter::getAndAdd,
                StorageException.class,
                MAX_TRIES,
                RETRY_DELAY_MS).apply(DEFAULT_BLOCK_SIZE);
        return new IdBlock(blockBase, DEFAULT_BLOCK_SIZE);
    }
}
