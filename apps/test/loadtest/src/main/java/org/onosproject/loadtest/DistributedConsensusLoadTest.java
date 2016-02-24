/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.loadtest;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.util.concurrent.RateLimiter;

/**
 * Simple application for load testing distributed consensus.
 * <p>
 * This application simply increments as {@link AsyncAtomicCounter} at a configurable rate.
 */
@Component(immediate = true)
@Service(value = DistributedConsensusLoadTest.class)
public class DistributedConsensusLoadTest {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ApplicationId appId;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    private static final int DEFAULT_RATE = 100;

    @Property(name = "rate", intValue = DEFAULT_RATE,
            label = "Total number of increments per second to the atomic counter")
    protected int rate = 0;

    private AtomicLong lastValue = new AtomicLong(0);
    private AtomicLong lastLoggedTime = new AtomicLong(0);
    private AsyncAtomicCounter counter;
    private ExecutorService testExecutor = Executors.newSingleThreadExecutor();

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.loadtest");
        log.info("Started with {}", appId);
        counter = storageService.atomicCounterBuilder()
                                .withName("onos-app-loadtest-counter")
                                .build();
        modified(null);
    }

    private void startTest() {
        stopped.set(false);
        RateLimiter limiter = RateLimiter.create(rate);
        Semaphore s = new Semaphore(100);
        while (!stopped.get()) {
            limiter.acquire();
            s.acquireUninterruptibly();
            counter.incrementAndGet().whenComplete((r, e) -> {
                s.release();
                long delta = System.currentTimeMillis() - lastLoggedTime.get();
                if (e == null) {
                    if (delta > 1000) {
                        long tps = (long) ((r - lastValue.get()) * 1000.0) / delta;
                        lastValue.set(r);
                        lastLoggedTime.set(System.currentTimeMillis());
                        log.info("Rate: {}", tps);
                    }
                }
            });
        }
    }

    private void stopTest() {
        stopped.set(true);
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        configService.unregisterProperties(getClass(), false);
        stopTest();
        testExecutor.shutdown();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        int newRate = DEFAULT_RATE;
        if (context != null) {
            Dictionary properties = context.getProperties();
            try {
                String s = get(properties, "rate");
                newRate = isNullOrEmpty(s)
                        ? rate : Integer.parseInt(s.trim());
            } catch (Exception e) {
                return;
            }
        }
        if (newRate != rate) {
            log.info("Rate changed to {}", newRate);
            rate = newRate;
            stopTest();
            testExecutor.execute(this::startTest);
        }
    }
}
