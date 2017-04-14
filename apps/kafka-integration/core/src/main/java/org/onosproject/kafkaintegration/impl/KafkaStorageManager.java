/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.kafkaintegration.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.kafkaintegration.api.KafkaEventStorageService;
import org.onosproject.kafkaintegration.api.dto.OnosEvent;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Task;
import org.onosproject.store.service.WorkQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component(immediate = false)
@Service
public class KafkaStorageManager implements KafkaEventStorageService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String KAFKA_WORK_QUEUE = "Kafka-Work-Queue";

    private WorkQueue<OnosEvent> queue;

    @Activate
    protected void activate() {
        queue = storageService.<OnosEvent>getWorkQueue(KAFKA_WORK_QUEUE,
                                                       Serializer.using(KryoNamespaces.API,
                                                                        OnosEvent.class,
                                                                        OnosEvent.Type.class));

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        queue = null;
        log.info("Stopped");
    }

    @Override
    public void publishEvent(OnosEvent e) {
        queue.addOne(e);
        log.debug("Published {} Event to Distributed Work Queue", e.type());
    }

    @Override
    public OnosEvent consumeEvent() {
        Task<OnosEvent> task = null;

        CompletableFuture<Task<OnosEvent>> future = queue.take();
        try {
            task = future.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        if (task != null) {
            queue.complete(task.taskId());
            log.debug("Consumed {} Event from Distributed Work Queue with id {}",
                     task.payload().type(), task.taskId());
            return task.payload();
        }

        return null;
    }

}
