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
package org.onosproject.loadtest;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.store.service.AsyncAtomicCounter;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.collect.Lists;
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
    private static final int TOTAL_COUNTERS = 50;

    @Property(name = "rate", intValue = DEFAULT_RATE,
            label = "Total number of increments per second to the atomic counter")
    protected int rate = 0;

    private final AtomicLong previousReportTime = new AtomicLong(0);
    private final AtomicLong previousCount = new AtomicLong(0);
    private final AtomicInteger increments = new AtomicInteger(0);
    private final List<AsyncAtomicCounter> counters = Lists.newArrayList();
    private final ScheduledExecutorService runner = Executors.newSingleThreadScheduledExecutor();
    private final ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor();

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.loadtest");
        log.info("Started with {}", appId);
        for (int i = 0; i < TOTAL_COUNTERS; ++i) {
            AsyncAtomicCounter counter =
                storageService.getAsyncAtomicCounter(String.format("onos-app-loadtest-counter-%d", i));
            counters.add(counter);
        }
        reporter.scheduleWithFixedDelay(() -> {
            Tools.allOf(counters.stream()
                    .map(AsyncAtomicCounter::get)
                    .collect(Collectors.toList()))
                    .whenComplete((r, e) -> {
                        if (e == null) {
                            long newCount = r.stream().reduce(Long::sum).get();
                            long currentTime = System.currentTimeMillis();
                            long delta = currentTime - previousReportTime.getAndSet(currentTime);
                            long rate = (newCount - previousCount.getAndSet(newCount)) * 1000 / delta;
                            log.info("{} updates per second", rate);
                        } else {
                            log.warn(e.getMessage());
                        }
            });
        }, 5, 5, TimeUnit.SECONDS);
        modified(null);
    }

    private void startTest() {
        stopped.set(false);
        RateLimiter limiter = RateLimiter.create(rate);
        Semaphore s = new Semaphore(100);
        while (!stopped.get()) {
            limiter.acquire();
            s.acquireUninterruptibly();
            counters.get(RandomUtils.nextInt(TOTAL_COUNTERS)).incrementAndGet().whenComplete((r, e) -> {
                s.release();
                if (e == null) {
                    increments.incrementAndGet();
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
        runner.shutdown();
        reporter.shutdown();
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
            log.info("Per node rate changed to {}", newRate);
            rate = newRate;
            stopTest();
            runner.execute(this::startTest);
        }
    }
}
