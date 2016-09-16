/**
 * Copyright 2016-present Open Networking Laboratory
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at

 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.kafkaintegration.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.kafkaintegration.api.KafkaEventStorageService;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.store.service.AtomicValue;
import org.onosproject.store.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component(immediate = false)
public class KafkaStorageManager implements KafkaEventStorageService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private TreeMap<Long, OnosEvent> kafkaEventStore;

    private AtomicValue<Long> lastPublishedEvent;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ScheduledExecutorService gcExService;

    private InternalGarbageCollector gcTask;

    // Thread scheduler parameters.
    private final long delay = 0;
    private final long period = 1;

    @Activate
    protected void activate() {
        kafkaEventStore = new TreeMap<Long, OnosEvent>();
        lastPublishedEvent = storageService.<Long>atomicValueBuilder()
                .withName("onos-app-kafka-published-seqNumber").build()
                .asAtomicValue();

        startGC();

        log.info("Started");
    }

    private void startGC() {
        log.info("Starting Garbage Collection Service");
        gcExService = Executors.newSingleThreadScheduledExecutor();
        gcTask = new InternalGarbageCollector();
        gcExService.scheduleAtFixedRate(gcTask, delay, period,
                                        TimeUnit.SECONDS);
    }

    @Deactivate
    protected void deactivate() {
        stopGC();
        log.info("Stopped");
    }

    private void stopGC() {
        log.info("Stopping Garbage Collection Service");
        gcExService.shutdown();
    }

    @Override
    public boolean insertCacheEntry(OnosEvent e) {
        // TODO: Fill in the code once the event carries timestamp info.
        return true;
    }

    @Override
    public void updateLastPublishedEntry(Long sequenceNumber) {
        this.lastPublishedEvent.set(sequenceNumber);
    }

    /**
     * Removes events from the Kafka Event Store which have been published.
     *
     */
    private class InternalGarbageCollector implements Runnable {

        @Override
        public void run() {
            kafkaEventStore.headMap(lastPublishedEvent.get(), true).clear();
        }
    }

}
