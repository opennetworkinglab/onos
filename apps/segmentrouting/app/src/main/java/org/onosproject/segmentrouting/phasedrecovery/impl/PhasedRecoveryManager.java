/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.segmentrouting.phasedrecovery.impl;

import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceAdminService;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.phasedrecovery.api.Phase;
import org.onosproject.segmentrouting.phasedrecovery.api.PhasedRecoveryService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.segmentrouting.phasedrecovery.api.OsgiPropertyConstants.PHASED_RECOVERY_DEFAULT;
import static org.onosproject.segmentrouting.phasedrecovery.api.OsgiPropertyConstants.PROP_PHASED_RECOVERY;

@Component(
        immediate = true,
        service = PhasedRecoveryService.class,
        property = {
                PROP_PHASED_RECOVERY + ":Boolean=" + PHASED_RECOVERY_DEFAULT
        }
)
public class PhasedRecoveryManager implements PhasedRecoveryService {
    private static final Logger log = LoggerFactory.getLogger(PhasedRecoveryManager.class);
    private static final String APP_NAME = "org.onosproject.phasedrecovery";

    // TODO Make these configurable via Component Config
    // Amount of time delayed to wait for port description (in second)
    private static final int PORT_CHECKER_INTERVAL = 1;
    // Max number of retry for port checker
    private static final int PORT_CHECKER_RETRIES = 5;
    // RoutingStableChecker interval (in second)
    private static final int ROUTING_CHECKER_DELAY = 3;
    // RoutingStableChecker timeout (in second)
    private static final int ROUTING_CHECKER_TIMEOUT = 15;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private ComponentConfigService compCfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceAdminService deviceAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    volatile SegmentRoutingService srService;

    /** Enabling phased recovery. */
    boolean phasedRecovery = PHASED_RECOVERY_DEFAULT;

    private ApplicationId appId;
    private ConsistentMap<DeviceId, Phase> phasedRecoveryStore;
    private ScheduledExecutorService executor = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors(), groupedThreads("onos/sr/pr", "executor"));

    @Activate
    protected void activate(ComponentContext context) {
        appId = coreService.registerApplication(APP_NAME);

        KryoNamespace.Builder serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(Phase.class);
        phasedRecoveryStore = storageService.<DeviceId, Phase>consistentMapBuilder()
                .withName("onos-sr-phasedrecovery")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer.build()))
                .build();

        compCfgService.registerProperties(getClass());
        modified(context);
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        phasedRecoveryStore.destroy();
        compCfgService.unregisterProperties(getClass(), false);
        log.info("Stopped");
    }

    @Modified
    protected void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        if (properties == null) {
            return;
        }

        String strPhasedRecovery = Tools.get(properties, PROP_PHASED_RECOVERY);
        boolean expectPhasedRecovery = Boolean.parseBoolean(strPhasedRecovery);
        if (expectPhasedRecovery != phasedRecovery) {
            phasedRecovery = expectPhasedRecovery;
            log.info("{} phased recovery", phasedRecovery ? "Enabling" : "Disabling");
        }
    }

    @Override
    public boolean isEnabled() {
        return phasedRecovery;
    }

    @Override
    public boolean init(DeviceId deviceId) {
        if (this.srService == null) {
            log.info("SegmentRoutingService is not ready");
            return false;
        }
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.info("Not master of {}", deviceId);
            return false;
        }

        Phase phase = Optional.ofNullable(phasedRecoveryStore.putIfAbsent(deviceId, Phase.PENDING))
                .map(Versioned::value).orElse(null);

        if (phase != null) {
            log.info("{} has been initialized already. Skipping.", deviceId);
            return false;
        } else {
            Phase nextPhase = (phasedRecovery && this.srService.getPairDeviceId(deviceId).isPresent()) ?
                    Phase.PAIR : Phase.EDGE;
            if (nextPhase == Phase.PAIR) {
                // Wait for the PORT_STAT before entering next phase.
                // Note: Unlikely, when the device init fails due to PORT_STATS timeout,
                //       it requires operator to manually move the device to the next phase by CLI command.
                executor.schedule(new PortChecker(deviceId, PORT_CHECKER_RETRIES),
                        PORT_CHECKER_INTERVAL, TimeUnit.SECONDS);
            } else {
                // We assume that all ports will be reported as enabled on devices that don't require phased recovery
                setPhase(deviceId, Phase.EDGE);
            }
            return true;
        }
    }

    @Override
    public boolean reset(DeviceId deviceId) {
        if (this.srService == null) {
            log.info("SegmentRoutingService is not ready");
            return false;
        }
        // FIXME Skip mastership checking since master will not be available when a device goes offline
        //       Improve this when persistent mastership is introduced

        Phase result = Optional.ofNullable(phasedRecoveryStore.remove(deviceId))
                .map(Versioned::value).orElse(null);
        if (result != null) {
            log.info("{} is reset", deviceId);
        }
        return result != null;
    }

    @Override
    public Map<DeviceId, Phase> getPhases() {
        return phasedRecoveryStore.asJavaMap();
    }

    @Override
    public Phase getPhase(DeviceId deviceId) {
        return Optional.ofNullable(phasedRecoveryStore.get(deviceId)).map(Versioned::value).orElse(null);
    }

    @Override
    public Phase setPhase(DeviceId deviceId, Phase newPhase) {
        if (this.srService == null) {
            log.info("SegmentRoutingService is not ready");
            return null;
        }
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.info("Not master of {}", deviceId);
            return null;
        }

        return Optional.ofNullable(phasedRecoveryStore.compute(deviceId, (k, v) -> {
            if (v == null && newPhase == Phase.PENDING) {
                log.info("Initializing {}", deviceId);
                return newPhase;
            } else if (v == Phase.PENDING && newPhase == Phase.PAIR) {
                srService.initHost(deviceId);
                // RouteHandler init is intentionally skipped when phased recovery is on.
                // Edge ports remain down in this phase. Therefore, no nexthop will be discovered on the given device.
                // The flow on given device will be programmed later by hostHandler.processHostMovedEvent()
                changePairPort(deviceId, true);
                log.info("Transitioning {} from PENDING to PAIR", deviceId);
                return newPhase;
            } else if (v == Phase.PAIR && newPhase == Phase.INFRA) {
                changeInfraPorts(deviceId, true);
                srService.initRoute(deviceId);
                log.info("Transitioning {} from PAIR to INFRA", deviceId);
                monitorRoutingStability(deviceId);
                return newPhase;
            } else if (v == Phase.INFRA && newPhase == Phase.EDGE) {
                changeEdgePorts(deviceId, true);
                log.info("Transitioning {} from INFRA to EDGE", deviceId);
                return newPhase;
            } else if (v == Phase.PENDING && newPhase == Phase.EDGE) {
                changeAllPorts(deviceId, true);
                srService.initHost(deviceId);
                srService.initRoute(deviceId);
                log.info("Transitioning {} from PENDING to EDGE", deviceId);
                return newPhase;
            } else {
                log.debug("Ignore illegal state transition on {} from {} to {}", deviceId, v, newPhase);
                return v;
            }
        })).map(Versioned::value).orElse(null);
    }

    private void monitorRoutingStability(DeviceId deviceId) {
        CompletableFuture<Void> checkerFuture = new CompletableFuture<>();
        CompletableFuture<Void> timeoutFuture =
                Tools.completeAfter(ROUTING_CHECKER_TIMEOUT, TimeUnit.SECONDS);
        RoutingStabilityChecker checker = new RoutingStabilityChecker(checkerFuture);

        checkerFuture.runAfterEitherAsync(timeoutFuture, () -> {
            if (checkerFuture.isDone()) {
                log.info("Routing stable. Move {} to the next phase", deviceId);
            } else {
                log.info("Timeout reached. Move {} to the next phase", deviceId);
                // Mark the future as completed to signify the termination of periodical checker
                checkerFuture.complete(null);
            }
            setPhase(deviceId, Phase.EDGE);
        });

        executor.schedule(checker, ROUTING_CHECKER_DELAY, TimeUnit.SECONDS);
    }

    @Override
    public Set<PortNumber> changeAllPorts(DeviceId deviceId, boolean enabled) {
        if (this.srService == null) {
            log.warn("SegmentRoutingService is not ready. Unable to changeAllPorts({}) to {}", deviceId, enabled);
            return Sets.newHashSet();
        }
        Set<PortNumber> portsToBeEnabled = deviceAdminService.getPorts(deviceId)
                .stream().map(Port::number).collect(Collectors.toSet());
        changePorts(deviceId, portsToBeEnabled, enabled);
        return portsToBeEnabled;
    }

    @Override
    public Set<PortNumber> changePairPort(DeviceId deviceId, boolean enabled) {
        if (this.srService == null) {
            log.warn("SegmentRoutingService is not ready. Unable to changePairPort({}) to {}", deviceId, enabled);
            return Sets.newHashSet();
        }
        Set<PortNumber> portsToBeEnabled = this.srService.getPairLocalPort(deviceId)
                .map(Sets::newHashSet).orElse(Sets.newHashSet());
        changePorts(deviceId, portsToBeEnabled, enabled);
        return portsToBeEnabled;
    }

    @Override
    public Set<PortNumber> changeInfraPorts(DeviceId deviceId, boolean enabled) {
        if (this.srService == null) {
            log.warn("SegmentRoutingService is not ready. Unable to changeInfraPorts({}) to {}", deviceId, enabled);
            return Sets.newHashSet();
        }
        Set<PortNumber> portsToBeEnabled = this.srService.getInfraPorts(deviceId);
        changePorts(deviceId, portsToBeEnabled, enabled);
        return portsToBeEnabled;
    }

    @Override
    public Set<PortNumber> changeEdgePorts(DeviceId deviceId, boolean enabled) {
        if (this.srService == null) {
            log.warn("SegmentRoutingService is not ready. Unable to changeEdgePorts({}) to {}", deviceId, enabled);
            return Sets.newHashSet();
        }
        Set<PortNumber> portsToBeEnabled = this.srService.getEdgePorts(deviceId);
        changePorts(deviceId, portsToBeEnabled, enabled);
        return portsToBeEnabled;
    }

    private void changePorts(DeviceId deviceId, Set<PortNumber> portNumbers, boolean enabled) {
        log.info("{} {} on {}", enabled ? "Enabled" : "Disabled", portNumbers, deviceId);
        portNumbers.forEach(portNumber ->
            deviceAdminService.changePortState(deviceId, portNumber, enabled));
    }

    private class PortChecker implements Runnable {
        int retries;
        DeviceId deviceId;

        PortChecker(DeviceId deviceId, int retries) {
            this.deviceId = deviceId;
            this.retries = retries;
        }

        @Override
        public void run() {
            retries -= 1;
            if (retries < 0) {
                log.warn("PORT_STATS timeout. Unable to initialize {}", deviceId);
                return;
            }

            if (!deviceAdminService.getPorts(deviceId).isEmpty()) {
                log.info("{} reported PORT_STATS", deviceId);
                setPhase(deviceId, Phase.PAIR);
            }
            log.info("{} still waiting for PORT_STATS", deviceId);
            executor.schedule(this, PORT_CHECKER_INTERVAL, TimeUnit.SECONDS);
        }
    }

    private class RoutingStabilityChecker implements Runnable {
        private final CompletableFuture<Void> future;

        RoutingStabilityChecker(CompletableFuture<Void> future) {
            this.future = future;
        }

        @Override
        public void run() {
            // Do not continue if the future has been completed
            if (future.isDone()) {
                log.trace("RouteStabilityChecker is done. Stop checking");
                return;
            }

            if (srService.isRoutingStable()) {
                log.trace("Routing is stable");
                future.complete(null);
            } else {
                log.trace("Routing is not yet stable");
                executor.schedule(this, ROUTING_CHECKER_DELAY, TimeUnit.SECONDS);
            }
        }
    }
}
