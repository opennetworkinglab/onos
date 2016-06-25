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
package org.onosproject.demo;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.commons.lang.math.RandomUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.MacAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Device;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleOperations;
import org.onosproject.net.flow.FlowRuleOperationsContext;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Predicate;
import com.google.common.base.Stopwatch;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to set up demos.
 */
@Component(immediate = true)
@Service
public class DemoInstaller implements DemoApi {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowService;

    private ExecutorService worker;

    private ExecutorService installWorker;

    private ApplicationId appId;

    private final Set<Intent> existingIntents = new HashSet<>();
    private RandomInstaller randomInstaller;

    private ObjectMapper mapper = new ObjectMapper();



    @Activate
    public void activate() {
        String nodeId = clusterService.getLocalNode().ip().toString();
        appId = coreService.registerApplication("org.onosproject.demo.installer."
                                                        + nodeId);
        worker = Executors.newFixedThreadPool(1,
                                              new ThreadFactoryBuilder()
                                                      .setNameFormat("demo-app-worker")
                                                      .build());
        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        shutdownAndAwaitTermination(worker);
        if (installWorker != null && !installWorker.isShutdown()) {
            shutdownAndAwaitTermination(installWorker);
        }
        log.info("Stopped");
    }

    @Override
    public JsonNode flowTest(Optional<JsonNode> params) {
        int flowsPerDevice = 1000;
        int neighbours = 0;
        boolean remove = true;
        if (params.isPresent()) {
            flowsPerDevice = params.get().get("flowsPerDevice").asInt();
            neighbours = params.get().get("neighbours").asInt();
            remove = params.get().get("remove").asBoolean();
        }

        Future<JsonNode> future = worker.submit(new FlowTest(flowsPerDevice, neighbours, remove));

        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            ObjectNode node = mapper.createObjectNode();
            node.put("Error", e.getMessage());
            return node;
        }
    }

    @Override
    public void setup(InstallType type, Optional<JsonNode> runParams) {
        switch (type) {
            case MESH:
                log.debug("Installing mesh intents");
                worker.execute(new MeshInstaller());
                break;
            case RANDOM:
                //check that we do not have a random installer running
                if (installWorker == null || installWorker.isShutdown()) {
                    installWorker = Executors.newFixedThreadPool(1,
                                                                 new ThreadFactoryBuilder()
                                                                         .setNameFormat("random-worker")
                                                                         .build());
                    log.debug("Installing random sequence of intents");
                    randomInstaller = new RandomInstaller(runParams);
                    installWorker.execute(randomInstaller);
                } else {
                    log.warn("Random installer is already running");
                }
                break;
            default:
                throw new IllegalArgumentException("What is it you want exactly?");
        }
    }

    @Override
    public void tearDown() {
        worker.submit(new UnInstaller());
    }


    /**
     * Simply installs a mesh of intents from all the hosts existing in the network.
     */
    private class MeshInstaller implements Runnable {

        @Override
        public void run() {
            TrafficSelector selector = DefaultTrafficSelector.emptySelector();
            TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
            List<Constraint> constraint = Lists.newArrayList();
            List<Host> hosts = Lists.newArrayList(hostService.getHosts());
            while (!hosts.isEmpty()) {
                Host src = hosts.remove(0);
                for (Host dst : hosts) {
                    HostToHostIntent intent = HostToHostIntent.builder()
                            .appId(appId)
                            .one(src.id())
                            .two(dst.id())
                            .selector(selector)
                            .treatment(treatment)
                            .constraints(constraint)
                            .build();
                    existingIntents.add(intent);
                    intentService.submit(intent);
                }
            }
        }
    }

    /**
     * Randomly installs and withdraws intents.
     */
    private class RandomInstaller implements Runnable {

        private final boolean isLocal;
        private final Set<Host> hosts;

        private final Random random = new Random(System.currentTimeMillis());

        private Set<HostPair> uninstalledOrWithdrawn;
        private Set<HostPair> installed;

        private CountDownLatch latch;

        //used to wait on a batch to be processed.
        private static final int ITERATIONMAX = 50000000;


        public RandomInstaller(Optional<JsonNode> runParams) {
            /*
                Check if we have params and honour them. Otherwise
                    set defaults to processing only local stuff and
                    all local hosts.
             */
            if (runParams.isPresent()) {
                JsonNode node = runParams.get();
                isLocal = node.get("local").asBoolean();
                hosts = node.get("hosts") == null ? Sets.newHashSet(hostService.getHosts()) :
                        constructHostIds(node.get("hosts").elements());
            } else {
                isLocal = true;
                hosts = Sets.newHashSet(hostService.getHosts());
            }

            //construct list of intents.
            installed = Sets.newHashSet();
            if (isLocal) {
                uninstalledOrWithdrawn = buildPairs(pruneHostsByMasterShip());
            } else {
                uninstalledOrWithdrawn = buildPairs(hosts);
            }

        }

        private Set<Host> constructHostIds(Iterator<JsonNode> elements) {
            Set<Host> hostIds = Sets.newHashSet();
            JsonNode n;
            while (elements.hasNext()) {
                n = elements.next();
                hostIds.add(hostService.getHost(HostId.hostId(n.textValue())));
            }
            return hostIds;
        }

        @Override
        public void run() {
            if (!installWorker.isShutdown()) {
                randomize();
                latch = new CountDownLatch(1);
                try {
                    trackIntents();
                } catch (InterruptedException e) {
                    shutdown();
                }
            }

        }


        /**
         *   Check whether the previously submitted batch is in progress
         *   and if yes submit the next one. If things hang, wait for at
         *   most 5 seconds and bail.
         * @throws InterruptedException if the thread go interupted
         */
        private void trackIntents() throws InterruptedException {
            //FIXME
            // TODO generate keys for each set of intents to allow manager to throttle
            // TODO may also look into the store to see how many operations are pending

            //if everything is good proceed.
            if (!installWorker.isShutdown()) {
                installWorker.execute(this);
            }

        }

        public void shutdown() {
            log.warn("Shutting down random installer!");
            cleanUp();
        }


        /**
         *   Shuffle the uninstalled and installed list (separately) and select
         *   a random number of them and install or uninstall them respectively.
         */
        private void randomize() {
            List<HostPair> hostList = new LinkedList<>(uninstalledOrWithdrawn);
            Collections.shuffle(hostList);
            List<HostPair> toInstall = hostList.subList(0,
                                                        random.nextInt(hostList.size() - 1));
            List<HostPair> toRemove;
            if (!installed.isEmpty()) {
                hostList = new LinkedList<>(installed);
                Collections.shuffle(hostList);
                toRemove = hostList.subList(0,
                                            random.nextInt(hostList.size() - 1));
                uninstallIntents(toRemove);
            }
            installIntents(toInstall);

        }

        private void installIntents(List<HostPair> toInstall) {
            for (HostPair pair : toInstall) {
                installed.add(pair);
                uninstalledOrWithdrawn.remove(pair);
                intentService.submit(pair.h2hIntent());
            }
        }

        private void uninstallIntents(Collection<HostPair> toRemove) {
            for (HostPair pair : toRemove) {
                installed.remove(pair);
                uninstalledOrWithdrawn.add(pair);
                intentService.withdraw(pair.h2hIntent());
            }
        }

        /**
         *  Take everything and remove it all.
         */
        private void cleanUp() {
            List<HostPair> allPairs = Lists.newArrayList(installed);
            allPairs.addAll(uninstalledOrWithdrawn);
            for (HostPair pair : allPairs) {
                intentService.withdraw(pair.h2hIntent());
            }
        }


        private Set<HostPair> buildPairs(Set<Host> hosts) {
            Set<HostPair> pairs = Sets.newHashSet();
            Iterator<Host> it = Sets.newHashSet(hosts).iterator();
            while (it.hasNext()) {
                Host src = it.next();
                it.remove();
                for (Host dst : hosts) {
                    pairs.add(new HostPair(src, dst));
                }
            }
            return pairs;
        }

        private Set<Host> pruneHostsByMasterShip() {
            return FluentIterable.from(hosts)
                    .filter(hasLocalMaster())
                    .toSet();

        }

        private Predicate<? super Host> hasLocalMaster() {
            return host -> mastershipService.getLocalRole(
                    host.location().deviceId()).equals(MastershipRole.MASTER);
        }


        /**
         * Simple class representing a pair of hosts and precomputes the associated
         * h2h intent.
         */
        private class HostPair {

            private final Host src;
            private final Host dst;

            private final TrafficSelector selector = DefaultTrafficSelector.emptySelector();
            private final TrafficTreatment treatment = DefaultTrafficTreatment.emptyTreatment();
            private final List<Constraint> constraint = Lists.newArrayList();
            private final HostToHostIntent intent;

            public HostPair(Host src, Host dst) {
                this.src = src;
                this.dst = dst;
                this.intent = HostToHostIntent.builder()
                        .appId(appId)
                        .one(src.id())
                        .two(dst.id())
                        .selector(selector)
                        .treatment(treatment)
                        .constraints(constraint)
                        .build();
            }

            public HostToHostIntent h2hIntent() {
                return intent;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }

                HostPair hostPair = (HostPair) o;

                return Objects.equals(src, hostPair.src) &&
                        Objects.equals(dst, hostPair.dst);

            }

            @Override
            public int hashCode() {
                return Objects.hash(src, dst);
            }


        }

    }

    /**
     * Remove anything that is running and clear it all out.
     */
    private class UnInstaller implements Runnable {
        @Override
        public void run() {
            if (!existingIntents.isEmpty()) {
                clearExistingIntents();
            }

            if (installWorker != null && !installWorker.isShutdown()) {
                shutdownAndAwaitTermination(installWorker);
                randomInstaller.shutdown();
            }
        }

        private void clearExistingIntents() {
            for (Intent i : existingIntents) {
                intentService.withdraw(i);
            }
            existingIntents.clear();
        }
    }

    /**
     * Shutdown a pool cleanly if possible.
     *
     * @param pool an executorService
     */
    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) {
                    log.error("Pool did not terminate");
                }
            }
        } catch (Exception ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }

    private class FlowTest implements Callable<JsonNode> {
        private final int flowPerDevice;
        private final int neighbours;
        private final boolean remove;
        private FlowRuleOperations.Builder adds;
        private FlowRuleOperations.Builder removes;

        public FlowTest(int flowsPerDevice, int neighbours, boolean remove) {
            this.flowPerDevice = flowsPerDevice;
            this.neighbours = neighbours;
            this.remove = remove;
            prepareInstallation();
        }

        private void prepareInstallation() {
            Set<ControllerNode> instances = Sets.newHashSet(clusterService.getNodes());
            instances.remove(clusterService.getLocalNode());
            Set<NodeId> acceptableNodes = Sets.newHashSet();
            if (neighbours >= instances.size()) {
                instances.forEach(instance -> acceptableNodes.add(instance.id()));
            } else {
                Iterator<ControllerNode> nodes = instances.iterator();
                for (int i = neighbours; i > 0; i--) {
                    acceptableNodes.add(nodes.next().id());
                }
            }
            acceptableNodes.add(clusterService.getLocalNode().id());

            Set<Device> devices = Sets.newHashSet();
            for (Device dev : deviceService.getDevices()) {
                if (acceptableNodes.contains(
                        mastershipService.getMasterFor(dev.id()))) {
                    devices.add(dev);
                }
            }

            TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                    .setOutput(PortNumber.portNumber(RandomUtils.nextInt())).build();
            TrafficSelector.Builder sbuilder;
            FlowRuleOperations.Builder rules = FlowRuleOperations.builder();
            FlowRuleOperations.Builder remove = FlowRuleOperations.builder();

            for (Device d : devices) {
                for (int i = 0; i < this.flowPerDevice; i++) {
                    sbuilder = DefaultTrafficSelector.builder();

                    sbuilder.matchEthSrc(MacAddress.valueOf(RandomUtils.nextInt() * i))
                            .matchEthDst(MacAddress.valueOf((Integer.MAX_VALUE - i) * RandomUtils.nextInt()));


                    int randomPriority = RandomUtils.nextInt(FlowRule.MAX_PRIORITY);
                    FlowRule f = DefaultFlowRule.builder()
                            .forDevice(d.id())
                            .withSelector(sbuilder.build())
                            .withTreatment(treatment)
                            .withPriority(randomPriority)
                            .fromApp(appId)
                            .makeTemporary(10)
                            .build();
                    rules.add(f);
                    remove.remove(f);

                }
            }

            this.adds = rules;
            this.removes = remove;
        }

        @Override
        public JsonNode call() throws Exception {
            ObjectNode node = mapper.createObjectNode();
            CountDownLatch latch = new CountDownLatch(1);
            flowService.apply(adds.build(new FlowRuleOperationsContext() {

                private final Stopwatch timer = Stopwatch.createStarted();

                @Override
                public void onSuccess(FlowRuleOperations ops) {

                    long elapsed = timer.elapsed(TimeUnit.MILLISECONDS);
                    node.put("elapsed", elapsed);


                    latch.countDown();
                }
            }));

            latch.await(10, TimeUnit.SECONDS);
            if (this.remove) {
                flowService.apply(removes.build());
            }
            return node;
        }
    }
}


