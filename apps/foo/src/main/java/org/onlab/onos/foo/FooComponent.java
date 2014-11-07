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
package org.onlab.onos.foo;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.onos.cluster.ClusterEvent;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.mastership.MastershipEvent;
import org.onlab.onos.mastership.MastershipListener;
import org.onlab.onos.mastership.MastershipService;
import org.onlab.onos.net.device.DeviceEvent;
import org.onlab.onos.net.device.DeviceListener;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.intent.IntentEvent;
import org.onlab.onos.net.intent.IntentListener;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.store.service.DatabaseAdminService;
import org.onlab.onos.store.service.DatabaseException;
import org.onlab.onos.store.service.DatabaseService;
import org.onlab.onos.store.service.OptionalResult;
import org.onlab.onos.store.service.PreconditionFailedException;
import org.onlab.onos.store.service.ReadRequest;
import org.onlab.onos.store.service.ReadResult;
import org.onlab.onos.store.service.WriteRequest;
import org.onlab.onos.store.service.WriteResult;
import org.slf4j.Logger;

import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;
import static java.util.concurrent.Executors.newScheduledThreadPool;

/**
 * Playground app component.
 */
@Component(immediate = true)
public class FooComponent {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    protected DatabaseAdminService dbAdminService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    protected DatabaseService dbService;

    private final ClusterEventListener clusterListener = new InnerClusterListener();
    private final DeviceListener deviceListener = new InnerDeviceListener();
    private final IntentListener intentListener = new InnerIntentListener();
    private final MastershipListener mastershipListener = new InnerMastershipListener();

    private ScheduledExecutorService executor;

    @Activate
    public void activate() {
        executor = newScheduledThreadPool(4, namedThreads("foo-executor-%d"));

        clusterService.addListener(clusterListener);
        deviceService.addListener(deviceListener);
        intentService.addListener(intentListener);
        mastershipService.addListener(mastershipListener);

        if (dbService == null || dbAdminService == null) {
            log.info("Couldn't find DB service");
        } else {
            log.info("Found DB service");
//            longIncrementor();
//            executor.scheduleAtFixedRate(new LongIncrementor(), 1, 10, TimeUnit.SECONDS);
//            executor.scheduleAtFixedRate(new LongIncrementor(), 1, 10, TimeUnit.SECONDS);
        }
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        executor.shutdown();
        clusterService.removeListener(clusterListener);
        deviceService.removeListener(deviceListener);
        intentService.removeListener(intentListener);
        mastershipService.removeListener(mastershipListener);
        log.info("Stopped");
    }

    private class InnerClusterListener implements ClusterEventListener {
        @Override
        public void event(ClusterEvent event) {
            log.info("WOOOOT! {}", event);
        }
    }

    private class InnerDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            log.info("YEEEEHAAAAW! {}", event);
        }
    }

    private class InnerIntentListener implements IntentListener {
        @Override
        public void event(IntentEvent event) {
            String message;
            if (event.type() == IntentEvent.Type.SUBMITTED) {
                message = "WOW! It looks like someone has some intentions: {}";
            } else if (event.type() == IntentEvent.Type.INSTALLED) {
                message = "AWESOME! So far things are going great: {}";
            } else if (event.type() == IntentEvent.Type.WITHDRAWN) {
                message = "HMMM! Ambitions are fading apparently: {}";
            } else {
                message = "CRAP!!! Things are not turning out as intended: {}";
            }
            log.info(message, event.subject());
        }
    }

    private class InnerMastershipListener implements MastershipListener {
        @Override
        public void event(MastershipEvent event) {
            final NodeId myId = clusterService.getLocalNode().id();
            if (myId.equals(event.roleInfo().master())) {
                log.info("I have control/I wish you luck {}", event);
            } else {
                log.info("you have control {}", event);
            }
        }
    }

    private void longIncrementor() {
        try {
            final String someTable = "admin";
            final String someKey = "long";

            dbAdminService.createTable(someTable);

            ReadResult read = dbService.read(ReadRequest.get(someTable, someKey));
            if (!read.valueExists()) {
                ByteBuffer zero = ByteBuffer.allocate(Long.BYTES).putLong(0);
                try {
                    dbService.write(WriteRequest
                                    .putIfAbsent(someTable,
                                                 someKey,
                                                 zero.array()));
                    log.info("Wrote initial value");
                    read = dbService.read(ReadRequest.get(someTable, someKey));
                } catch (PreconditionFailedException e) {
                    log.info("Concurrent write detected.", e);

                    // concurrent write detected, read and fall through
                    read = dbService.read(ReadRequest.get(someTable, someKey));
                    if (!read.valueExists()) {
                        log.error("Shouldn't reach here");
                    }
                }
            }
            int retry = 5;
            do {
                ByteBuffer prev = ByteBuffer.wrap(read.value().value());
                long next = prev.getLong() + 1;
                byte[] newValue = ByteBuffer.allocate(Long.BYTES).putLong(next).array();
                OptionalResult<WriteResult, DatabaseException> result
                = dbService.writeNothrow(WriteRequest
                                         .putIfVersionMatches(someTable,
                                                              someKey,
                                                              newValue,
                                                              read.value().version()));
                if (result.hasValidResult()) {
                    log.info("Write success {} -> {}", result.get().previousValue(), next);
                    break;
                } else {
                    log.info("Write failed trying to write{}", next);
                }
            } while(retry-- > 0);
        } catch (Exception e) {
            log.error("Exception thrown", e);
        }
    }

    private final class LongIncrementor implements Runnable {

        @Override
        public void run() {
            longIncrementor();
        }

    }
}


