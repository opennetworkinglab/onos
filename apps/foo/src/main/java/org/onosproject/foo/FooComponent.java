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
package org.onosproject.foo;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static org.onlab.util.Tools.namedThreads;
import static org.slf4j.LoggerFactory.getLogger;

import java.nio.ByteBuffer;
import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;



import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.cluster.ClusterEventListener;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.store.service.DatabaseAdminService;
import org.onosproject.store.service.DatabaseException;
import org.onosproject.store.service.DatabaseService;
import org.onosproject.store.service.Lock;
import org.onosproject.store.service.LockService;
import org.onosproject.store.service.VersionedValue;
import org.slf4j.Logger;

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

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_UNARY)
    protected LockService lockService;

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
            //longIncrementor();
            //lockUnlock();
            //executor.scheduleAtFixedRate(new LongIncrementor(), 1, 10, TimeUnit.SECONDS);
            //executor.scheduleAtFixedRate(new LongIncrementor(), 1, 10, TimeUnit.SECONDS);
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
            if (event.type() == IntentEvent.Type.INSTALL_REQ) {
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

    private void lockUnlock() throws InterruptedException {
        try {
            final String locksTable = "onos-locks";

            if (!dbAdminService.listTables().contains(locksTable)) {
                dbAdminService.createTable(locksTable, 10000);
            }
            Lock lock = lockService.create("foo-bar");
            log.info("Requesting lock");
            lock.lock(10000);
            //try {
                //Thread.sleep(5000);
            //} catch (InterruptedException e) {
                //e.printStackTrace();
            //}
            log.info("Acquired Lock");
            log.info("Do I have the lock: {} ", lock.isLocked());
            //lock.unlock();
            log.info("Do I have the lock: {} ", lock.isLocked());
        } finally {
            log.info("Done");
        }
    }

    private void longIncrementor() {
        try {
            final String someTable = "admin";
            final String someKey = "long";

            if (!dbAdminService.listTables().contains(someTable)) {
                dbAdminService.createTable(someTable);
            }

            VersionedValue vv = dbService.get(someTable, someKey);
            if (vv == null) {
                ByteBuffer zero = ByteBuffer.allocate(Long.BYTES).putLong(0);
                if (dbService.putIfAbsent(someTable, someKey, zero.array())) {
                    log.info("Wrote initial value");
                    vv = dbService.get(someTable, someKey);
                } else {
                    log.info("Concurrent write detected.");
                    // concurrent write detected, read and fall through
                    vv = dbService.get(someTable, someKey);
                    if (vv == null) {
                        log.error("Shouldn't reach here");
                    }
                }
            }
            int retry = 1;

            do {
                if (vv == null) {
                    log.error("Shouldn't reach here - value is null");
                    break;
                }
                ByteBuffer prev = ByteBuffer.wrap(vv.value());
                long next = prev.getLong() + 1;
                byte[] newValue = ByteBuffer.allocate(Long.BYTES).putLong(next).array();
                if (dbService.putIfVersionMatches(someTable, someKey, newValue, vv.version())) {
                    log.info("Write success. New value: {}", next);
                    break;
                } else {
                    log.info("Write failed retrying.....{}", retry);
                    vv = dbService.get(someTable, someKey);
                    if (vv == null) {
                        log.error("Shouldn't reach here");
                    }
                }
            } while (retry++ < 5);
        } catch (DatabaseException e) {
            log.debug("DB Exception thrown {}", e.getMessage());
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


