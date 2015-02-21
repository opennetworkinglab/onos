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
package org.onosproject.intentperf;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.onlab.util.Counter;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.PointToPointIntent;
import org.slf4j.Logger;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkState;
import static java.lang.String.format;
import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.intent.IntentEvent.Type.INSTALLED;
import static org.onosproject.net.intent.IntentEvent.Type.WITHDRAWN;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to set up demos.
 */
@Component(immediate = true)
public class IntentPerfInstaller {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected DeviceService deviceService;

    private ExecutorService worker;
    private ApplicationId appId;
    private Listener listener;
    private Set<Intent> intents;
    private Set<Intent> submitted;
    private Set<Intent> withdrawn;
    private boolean stopped;

    private static final long REPORT_PERIOD = 5000L; //ms
    private Timer reportTimer;

    //FIXME make this configurable
    private static final int NUM_KEYS = 10_000;

    @Activate
    public void activate() {
        String nodeId = clusterService.getLocalNode().ip().toString();
        appId = coreService.registerApplication("org.onosproject.intentperf."
                                                        + nodeId);
        intents = Sets.newHashSet();
        submitted = Sets.newHashSet();
        withdrawn = Sets.newHashSet();

        worker = Executors.newFixedThreadPool(1, groupedThreads("onos/intent-perf", "worker"));
        log.info("Started with Application ID {}", appId.id());
        start(); //FIXME
    }

    @Deactivate
    public void deactivate() {
        stop();
        log.info("Stopped");
    }

    public void start() {
        // perhaps we want to prime before listening...
        // we will need to discard the first few results for priming and warmup
        listener = new Listener();
        intentService.addListener(listener);

        long delay = System.currentTimeMillis() % REPORT_PERIOD;
        reportTimer = new Timer("onos-intent-perf-reporter");
        reportTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                listener.report();
            }
        }, delay, REPORT_PERIOD);

        stopped = false;
        worker.submit(() -> {
            delay(2000); // take a breath to start
            createIntents(NUM_KEYS, 2); //FIXME
            prime();
            while (!stopped) {
                cycle();
                delay(800); // take a breath
            }
        });

    }

    public void stop() {
        if (listener != null) {
            reportTimer.cancel();
            intentService.removeListener(listener);
            listener = null;
            reportTimer = null;
        }
        stopped = true;
        try {
            worker.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to stop worker.");
        }
    }


    private void cycle() {
        long start = System.currentTimeMillis();
        subset(submitted).forEach(this::withdraw);
        subset(withdrawn).forEach(this::submit);
        long delta = System.currentTimeMillis() - start;
        if (delta > 1000 || delta < 0) {
            log.warn("Cycle took {} ms", delta);
        }
    }

    private Iterable<Intent> subset(Set<Intent> intents) {
        List<Intent> subset = Lists.newArrayList(intents);
        Collections.shuffle(subset);
        return subset.subList(0, subset.size() / 2);
    }

    private void submit(Intent intent) {
        intentService.submit(intent);
        submitted.add(intent);
        withdrawn.remove(intent); //TODO could check result here...
    }

    private void withdraw(Intent intent) {
        intentService.withdraw(intent);
        withdrawn.add(intent);
        submitted.remove(intent); //TODO could check result here...
    }

    private void createIntents(int numberOfKeys, int pathLength) {
        Iterator<Device> deviceItr = deviceService.getAvailableDevices().iterator();

        Device ingressDevice = null;
        while (deviceItr.hasNext()) {
            Device device = deviceItr.next();
            if (deviceService.getRole(device.id()) == MastershipRole.MASTER) {
                ingressDevice = device;
                break;
            }
        }
        checkState(ingressDevice != null, "There are no local devices");

        for (int local = 0, i = 0; local < numberOfKeys; i++) {
            Key key = Key.of(i, appId);
            if (!intentService.isLocal(key)) {
                continue;
            }
            TrafficSelector selector = DefaultTrafficSelector.builder().build();

            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
            //FIXME
            ConnectPoint ingress = new ConnectPoint(ingressDevice.id(), PortNumber.portNumber(1));
            ConnectPoint egress = new ConnectPoint(ingressDevice.id(), PortNumber.portNumber(2));

            Intent intent = new PointToPointIntent(appId, key,
                                                   selector, treatment,
                                                   ingress, egress,
                                                   Collections.emptyList());
            intents.add(intent);
            local++;
            if (i % 1000 == 0) {
                log.info("Building intents... {} ({})", local, i);
            }
        }
        log.info("Created {} intents", numberOfKeys);
    }

    private void prime() {
        int i = 0;
        withdrawn.addAll(intents);
        for (Intent intent : intents) {
            submit(intent);
            // only submit half of the intents to start
            if (i++ >= intents.size() / 2) {
                break;
            }
        }
    }

    class Listener implements IntentListener {

        private final Map<IntentEvent.Type, Counter> counters;
        private final Counter runningTotal = new Counter();

        public Listener() {
            counters = initCounters();
        }

        private Map<IntentEvent.Type, Counter> initCounters() {
            Map<IntentEvent.Type, Counter> map = Maps.newHashMap();
            for (IntentEvent.Type type : IntentEvent.Type.values()) {
                map.put(type, new Counter());
            }
            return map;
        }

        @Override
        public void event(IntentEvent event) {
            if (event.subject().appId().equals(appId)) {
                counters.get(event.type()).add(1);
            }
        }

        public void report() {
            StringBuilder stringBuilder = new StringBuilder();
            Counter installed = counters.get(INSTALLED);
            Counter withdrawn = counters.get(WITHDRAWN);
            double current = installed.throughput() + withdrawn.throughput();
            runningTotal.add(installed.total() + withdrawn.total());
            for (IntentEvent.Type type : IntentEvent.Type.values()) {
                stringBuilder.append(printCounter(type)).append("; ");
            }
            log.info("Throughput: OVERALL={}; CURRENT={}; {}",
                     format("%.2f", runningTotal.throughput()),
                     format("%.2f", current), stringBuilder);
        }

        private String printCounter(IntentEvent.Type event) {
            Counter counter = counters.get(event);
            String result = format("%s=%.2f", event, counter.throughput());
            counter.reset();
            return result;
        }
    }
}
