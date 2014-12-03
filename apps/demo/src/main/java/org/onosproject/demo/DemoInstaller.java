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
package org.onosproject.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onosproject.cluster.ClusterService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentBatchService;
import org.onosproject.net.intent.IntentOperations;
import org.onosproject.net.intent.IntentService;
import org.slf4j.Logger;


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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


import static org.slf4j.LoggerFactory.getLogger;

/**
 * Application to set up demos.
 */
@Component(immediate = true)
@Service
public class DemoInstaller implements DemoAPI {

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
    protected IntentBatchService intentBatchService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private ExecutorService worker;

    private ExecutorService randomWorker;

    private ApplicationId appId;

    private final Set<Intent> existingIntents = new HashSet<>();
    private RandomInstaller randomInstaller;



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
        if (!randomWorker.isShutdown()) {
            shutdownAndAwaitTermination(randomWorker);
        }
        log.info("Stopped");
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
                if (randomWorker == null || randomWorker.isShutdown()) {
                    randomWorker = Executors.newFixedThreadPool(1,
                                                   new ThreadFactoryBuilder()
                                                           .setNameFormat("random-worker")
                                                           .build());
                    log.debug("Installing random sequence of intents");
                    randomInstaller = new RandomInstaller(runParams);
                    randomWorker.execute(randomInstaller);
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
            TrafficSelector selector = DefaultTrafficSelector.builder().build();
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
            List<Constraint> constraint = Lists.newArrayList();
            List<Host> hosts = Lists.newArrayList(hostService.getHosts());
            while (!hosts.isEmpty()) {
                Host src = hosts.remove(0);
                for (Host dst : hosts) {
                    HostToHostIntent intent = new HostToHostIntent(appId, src.id(), dst.id(),
                                                                   selector, treatment,
                                                                   constraint);
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
            if (!randomWorker.isShutdown()) {
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
            int count = 0;
            while (!latch.await(100, TimeUnit.NANOSECONDS)) {
                if (intentBatchService.getPendingOperations().isEmpty()) {
                    latch.countDown();
                }
                count++;
                if (count > ITERATIONMAX) {
                    log.warn("A batch is stuck processing. " +
                                     "pending : {}",
                             intentBatchService.getPendingOperations());
                    shutdownAndAwaitTermination(randomWorker);
                }
            }
            //if everyting is good proceed.
            if (!randomWorker.isShutdown()) {
                randomWorker.execute(this);
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
            IntentOperations.Builder builder = IntentOperations.builder(appId);
            for (HostPair pair : toInstall) {
                installed.add(pair);
                uninstalledOrWithdrawn.remove(pair);
                builder.addSubmitOperation(pair.h2hIntent());
            }
            intentBatchService.addIntentOperations(builder.build());
        }

        private void uninstallIntents(Collection<HostPair> toRemove) {
            IntentOperations.Builder builder = IntentOperations.builder(appId);
            for (HostPair pair : toRemove) {
                installed.remove(pair);
                uninstalledOrWithdrawn.add(pair);
                builder.addWithdrawOperation(pair.h2hIntent().id());
            }
            intentBatchService.addIntentOperations(builder.build());
        }

        /**
         *  Take everything and remove it all.
         */
        private void cleanUp() {
            List<HostPair> allPairs = Lists.newArrayList(installed);
            allPairs.addAll(uninstalledOrWithdrawn);
            IntentOperations.Builder builder = IntentOperations.builder(appId);
            for (HostPair pair : allPairs) {
                builder.addWithdrawOperation(pair.h2hIntent().id());
            }
            intentBatchService.addIntentOperations(builder.build());
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
            return new Predicate<Host>() {
                @Override
                public boolean apply(Host host) {
                    return mastershipService.getLocalRole(
                            host.location().deviceId()).equals(MastershipRole.MASTER);
                }
            };
        }


        /**
         * Simple class representing a pair of hosts and precomputes the associated
         * h2h intent.
         */
        private class HostPair {

            private final Host src;
            private final Host dst;

            private final TrafficSelector selector = DefaultTrafficSelector.builder().build();
            private final TrafficTreatment treatment = DefaultTrafficTreatment.builder().build();
            private final List<Constraint> constraint = Lists.newArrayList();
            private final HostToHostIntent intent;

            public HostPair(Host src, Host dst) {
                this.src = src;
                this.dst = dst;
                this.intent = new HostToHostIntent(appId, src.id(), dst.id(),
                                                   selector, treatment, constraint);
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

            if (randomWorker != null && !randomWorker.isShutdown()) {
                shutdownAndAwaitTermination(randomWorker);
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

}


