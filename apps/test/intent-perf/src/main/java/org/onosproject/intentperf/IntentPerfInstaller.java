/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onlab.util.Counter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
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
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static org.apache.felix.scr.annotations.ReferenceCardinality.MANDATORY_UNARY;
import static org.onlab.util.Tools.*;
import static org.onosproject.net.intent.IntentEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to test sustained intent throughput.
 */
@Component(immediate = true)
@Service(value = IntentPerfInstaller.class)
public class IntentPerfInstaller {

    private final Logger log = getLogger(getClass());

    private static final int DEFAULT_NUM_WORKERS = 1;

    private static final int DEFAULT_NUM_KEYS = 40000;
    private static final int DEFAULT_GOAL_CYCLE_PERIOD = 1000; //ms

    private static final int DEFAULT_NUM_NEIGHBORS = 0;

    private static final int START_DELAY = 5_000; // ms
    private static final int REPORT_PERIOD = 1_000; //ms

    private static final String START = "start";
    private static final String STOP = "stop";
    private static final MessageSubject CONTROL = new MessageSubject("intent-perf-ctl");

    //FIXME add path length

    @Property(name = "numKeys", intValue = DEFAULT_NUM_KEYS,
            label = "Number of keys (i.e. unique intents) to generate per instance")
    private int numKeys = DEFAULT_NUM_KEYS;

    //TODO implement numWorkers property
//    @Property(name = "numThreads", intValue = DEFAULT_NUM_WORKERS,
//              label = "Number of installer threads per instance")
//    private int numWokers = DEFAULT_NUM_WORKERS;

    @Property(name = "cyclePeriod", intValue = DEFAULT_GOAL_CYCLE_PERIOD,
            label = "Goal for cycle period (in ms)")
    private int cyclePeriod = DEFAULT_GOAL_CYCLE_PERIOD;

    @Property(name = "numNeighbors", intValue = DEFAULT_NUM_NEIGHBORS,
            label = "Number of neighbors to generate intents for")
    private int numNeighbors = DEFAULT_NUM_NEIGHBORS;

    @Reference(cardinality = MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected WorkPartitionService partitionService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected ComponentConfigService configService;

    @Reference(cardinality = MANDATORY_UNARY)
    protected IntentPerfCollector sampleCollector;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService communicationService;

    private ExecutorService messageHandlingExecutor;

    private ExecutorService workers;
    private ApplicationId appId;
    private Listener listener;
    private boolean stopped = true;

    private Timer reportTimer;

    // FIXME this variable isn't shared properly between multiple worker threads
    private int lastKey = 0;

    private IntentPerfUi perfUi;
    private NodeId nodeId;
    private TimerTask reporterTask;

    @Activate
    public void activate(ComponentContext context) {
        configService.registerProperties(getClass());

        nodeId = clusterService.getLocalNode().id();
        appId = coreService.registerApplication("org.onosproject.intentperf." + nodeId.toString());

        // TODO: replace with shared timer
        reportTimer = new Timer("onos-intent-perf-reporter");
        workers = Executors.newFixedThreadPool(DEFAULT_NUM_WORKERS, groupedThreads("onos/intent-perf", "worker-%d"));

        // TODO: replace with shared executor
        messageHandlingExecutor = Executors.newSingleThreadExecutor(
                groupedThreads("onos/perf", "command-handler"));

        communicationService.addSubscriber(CONTROL, String::new, new InternalControl(),
                                           messageHandlingExecutor);

        listener = new Listener();
        intentService.addListener(listener);

        // TODO: investigate why this seems to be necessary for configs to get picked up on initial activation
        modify(context);
    }

    @Deactivate
    public void deactivate() {
        stopTestRun();

        configService.unregisterProperties(getClass(), false);
        messageHandlingExecutor.shutdown();
        communicationService.removeSubscriber(CONTROL);

        if (listener != null) {
            reportTimer.cancel();
            intentService.removeListener(listener);
            listener = null;
            reportTimer = null;
        }
    }

    @Modified
    public void modify(ComponentContext context) {
        if (context == null) {
            logConfig("Reconfigured");
            return;
        }

        Dictionary<?, ?> properties = context.getProperties();
        int newNumKeys, newCyclePeriod, newNumNeighbors;
        try {
            String s = get(properties, "numKeys");
            newNumKeys = isNullOrEmpty(s) ? numKeys : Integer.parseInt(s.trim());

            s = get(properties, "cyclePeriod");
            newCyclePeriod = isNullOrEmpty(s) ? cyclePeriod : Integer.parseInt(s.trim());

            s = get(properties, "numNeighbors");
            newNumNeighbors = isNullOrEmpty(s) ? numNeighbors : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            log.warn("Malformed configuration detected; using defaults", e);
            newNumKeys = DEFAULT_NUM_KEYS;
            newCyclePeriod = DEFAULT_GOAL_CYCLE_PERIOD;
            newNumNeighbors = DEFAULT_NUM_NEIGHBORS;
        }

        if (newNumKeys != numKeys || newCyclePeriod != cyclePeriod || newNumNeighbors != numNeighbors) {
            numKeys = newNumKeys;
            cyclePeriod = newCyclePeriod;
            numNeighbors = newNumNeighbors;
            logConfig("Reconfigured");
        }
    }

    public void start() {
        if (stopped) {
            stopped = false;
            communicationService.broadcast(START, CONTROL, str -> str.getBytes());
            startTestRun();
        }
    }

    public void stop() {
        if (!stopped) {
            communicationService.broadcast(STOP, CONTROL, str -> str.getBytes());
            stopTestRun();
        }
    }

    private void logConfig(String prefix) {
        log.info("{} with appId {}; numKeys = {}; cyclePeriod = {} ms; numNeighbors={}",
                 prefix, appId.id(), numKeys, cyclePeriod, numNeighbors);
    }

    private void startTestRun() {
        sampleCollector.clearSamples();

        // adjust numNeighbors and generate list of neighbors
        numNeighbors = Math.min(clusterService.getNodes().size() - 1, numNeighbors);

        // Schedule reporter task on report period boundary
        reporterTask = new ReporterTask();
        reportTimer.scheduleAtFixedRate(reporterTask,
                                        REPORT_PERIOD - currentTimeMillis() % REPORT_PERIOD,
                                        REPORT_PERIOD);

        // Submit workers
        stopped = false;
        for (int i = 0; i < DEFAULT_NUM_WORKERS; i++) {
            workers.submit(new Submitter(createIntents(numKeys, /*FIXME*/ 2, lastKey)));
        }
        log.info("Started test run");
    }

    private void stopTestRun() {
        if (reporterTask != null) {
            reporterTask.cancel();
            reporterTask = null;
        }

        try {
            workers.awaitTermination(5 * cyclePeriod, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to stop worker", e);
        }

        sampleCollector.recordSample(0, 0);
        sampleCollector.recordSample(0, 0);
        stopped = true;

        log.info("Stopped test run");
    }

    private List<NodeId> getNeighbors() {
        List<NodeId> nodes = clusterService.getNodes().stream()
                .map(ControllerNode::id)
                .collect(Collectors.toCollection(ArrayList::new));
        // sort neighbors by id
        Collections.sort(nodes, (node1, node2) ->
                node1.toString().compareTo(node2.toString()));
        // rotate the local node to index 0
        Collections.rotate(nodes, -1 * nodes.indexOf(clusterService.getLocalNode().id()));
        log.debug("neighbors (raw): {}", nodes); //TODO remove
        // generate the sub-list that will contain local node and selected neighbors
        nodes = nodes.subList(0, numNeighbors + 1);
        log.debug("neighbors: {}", nodes); //TODO remove
        return nodes;
    }

    private Intent createIntent(Key key, long mac, NodeId node, Multimap<NodeId, Device> devices) {
        // choose a random device for which this node is master
        List<Device> deviceList = devices.get(node).stream().collect(Collectors.toList());
        Device device = deviceList.get(RandomUtils.nextInt(deviceList.size()));

        //FIXME we currently ignore the path length and always use the same device
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.valueOf(mac)).build();
        TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
        ConnectPoint ingress = new ConnectPoint(device.id(), PortNumber.portNumber(1));
        ConnectPoint egress = new ConnectPoint(device.id(), PortNumber.portNumber(2));

        return PointToPointIntent.builder()
                .appId(appId)
                .key(key)
                .selector(selector)
                .treatment(treatment)
                .ingressPoint(ingress)
                .egressPoint(egress)
                .build();
    }

    /**
     * Creates a specified number of intents for testing purposes.
     *
     * @param numberOfKeys number of intents
     * @param pathLength   path depth
     * @param firstKey     first key to attempt
     * @return set of intents
     */
    private Set<Intent> createIntents(int numberOfKeys, int pathLength, int firstKey) {
        List<NodeId> neighbors = getNeighbors();

        Multimap<NodeId, Device> devices = ArrayListMultimap.create();
        deviceService.getAvailableDevices()
                .forEach(device -> devices.put(mastershipService.getMasterFor(device.id()), device));

        // ensure that we have at least one device per neighbor
        neighbors.forEach(node -> checkState(!devices.get(node).isEmpty(),
                                             "There are no devices for {}", node));

        // TODO pull this outside so that createIntent can use it
        // prefix based on node id for keys generated on this instance
        long keyPrefix = ((long) clusterService.getLocalNode().ip().getIp4Address().toInt()) << 32;

        int maxKeysPerNode = (int) Math.ceil((double) numberOfKeys / neighbors.size());
        Multimap<NodeId, Intent> intents = ArrayListMultimap.create();

        for (int count = 0, k = firstKey; count < numberOfKeys; k++) {
            Key key = Key.of(keyPrefix + k, appId);

            NodeId leader = partitionService.getLeader(key, Key::hash);
            if (!neighbors.contains(leader) || intents.get(leader).size() >= maxKeysPerNode) {
                // Bail if we are not sending to this node or we have enough for this node
                continue;
            }
            intents.put(leader, createIntent(key, keyPrefix + k, leader, devices));

            // Bump up the counter and remember this as the last key used.
            count++;
            lastKey = k;
            if (count % 1000 == 0) {
                log.info("Building intents... {} (attempt: {})", count, lastKey);
            }
        }
        checkState(intents.values().size() == numberOfKeys,
                   "Generated wrong number of intents");
        log.info("Created {} intents", numberOfKeys);
        intents.keySet().forEach(node -> log.info("\t{}\t{}", node, intents.get(node).size()));

        return Sets.newHashSet(intents.values());
    }

    final Set<Intent> submitted = Sets.newConcurrentHashSet();
    final Set<Intent> withdrawn = Sets.newConcurrentHashSet();

    // Submits intent operations.
    final class Submitter implements Runnable {

        private long lastDuration;
        private int lastCount;

        private Set<Intent> intents = Sets.newHashSet();


        private Submitter(Set<Intent> intents) {
            this.intents = intents;
            lastCount = numKeys / 4;
            lastDuration = 1_000; // 1 second
        }

        @Override
        public void run() {
            prime();
            while (!stopped) {
                try {
                    cycle();
                } catch (Exception e) {
                    log.warn("Exception during cycle", e);
                }
            }
            clear();
        }

        private Iterable<Intent> subset(Set<Intent> intents) {
            List<Intent> subset = Lists.newArrayList(intents);
            Collections.shuffle(subset);
            return subset.subList(0, Math.min(subset.size(), lastCount));
        }

        // Submits the specified intent.
        private void submit(Intent intent) {
            intentService.submit(intent);
            withdrawn.remove(intent); //TODO could check result here...
        }

        // Withdraws the specified intent.
        private void withdraw(Intent intent) {
            intentService.withdraw(intent);
            submitted.remove(intent); //TODO could check result here...
        }

        // Primes the cycle.
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

        private void clear() {
            submitted.forEach(this::withdraw);
        }

        // Runs a single operation cycle.
        private void cycle() {
            //TODO consider running without rate adjustment
            adjustRates();

            long start = currentTimeMillis();
            subset(submitted).forEach(this::withdraw);
            subset(withdrawn).forEach(this::submit);
            long delta = currentTimeMillis() - start;

            if (delta > cyclePeriod * 3 || delta < 0) {
                log.warn("Cycle took {} ms", delta);
            }

            int difference = cyclePeriod - (int) delta;
            if (difference > 0) {
                delay(difference);
            }

            lastDuration = delta;
        }

        int cycleCount = 0;

        private void adjustRates() {

            int addDelta = Math.max(1000 - cycleCount, 10);
            double multRatio = Math.min(0.8 + cycleCount * 0.0002, 0.995);

            //FIXME need to iron out the rate adjustment
            //FIXME we should taper the adjustments over time
            //FIXME don't just use the lastDuration, take an average
            if (++cycleCount % 5 == 0) { //TODO: maybe use a timer (we should do this every 5-10 sec)
                if (listener.requestThroughput() - listener.processedThroughput() <= 2000 && //was 500
                        lastDuration <= cyclePeriod) {
                    lastCount = Math.min(lastCount + addDelta, intents.size() / 2);
                } else {
                    lastCount *= multRatio;
                }
                log.info("last count: {}, last duration: {} ms (sub: {} vs inst: {})",
                         lastCount, lastDuration, listener.requestThroughput(), listener.processedThroughput());
            }

        }
    }

    // Event listener to monitor throughput.
    final class Listener implements IntentListener {

        private final Counter runningTotal = new Counter();
        private volatile Map<IntentEvent.Type, Counter> counters;

        private volatile double processedThroughput = 0;
        private volatile double requestThroughput = 0;

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

        public double processedThroughput() {
            return processedThroughput;
        }

        public double requestThroughput() {
            return requestThroughput;
        }

        @Override
        public void event(IntentEvent event) {
            if (event.subject().appId().equals(appId)) {
                if (event.type() == INSTALLED) {
                    submitted.add(event.subject());
                }
                if (event.type() == WITHDRAWN) {
                    withdrawn.add(event.subject());
                }
                counters.get(event.type()).add(1);
            }
        }

        public void report() {
            Map<IntentEvent.Type, Counter> reportCounters = counters;
            counters = initCounters();

            // update running total and latest throughput
            Counter installed = reportCounters.get(INSTALLED);
            Counter withdrawn = reportCounters.get(WITHDRAWN);
            processedThroughput = installed.throughput() + withdrawn.throughput();
            runningTotal.add(installed.total() + withdrawn.total());

            Counter installReq = reportCounters.get(INSTALL_REQ);
            Counter withdrawReq = reportCounters.get(WITHDRAW_REQ);
            requestThroughput = installReq.throughput() + withdrawReq.throughput();

            // build the string to report
            StringBuilder stringBuilder = new StringBuilder();
            for (IntentEvent.Type type : IntentEvent.Type.values()) {
                Counter counter = reportCounters.get(type);
                stringBuilder.append(format("%s=%.2f;", type, counter.throughput()));
            }
            log.info("Throughput: OVERALL={}; CURRENT={}; {}",
                     format("%.2f", runningTotal.throughput()),
                     format("%.2f", processedThroughput),
                     stringBuilder);

            sampleCollector.recordSample(runningTotal.throughput(),
                                         processedThroughput);
        }
    }

    private class InternalControl implements Consumer<String> {
        @Override
        public void accept(String cmd) {
            log.info("Received command {}", cmd);
            if (cmd.equals(START)) {
                startTestRun();
            } else {
                stopTestRun();
            }
        }
    }

    private class ReporterTask extends TimerTask {
        @Override
        public void run() {
            //adjustRates(); // FIXME we currently adjust rates in the cycle thread
            listener.report();
        }
    }

}
