/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.l2lb.app;


import com.google.common.collect.Sets;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.l2lb.api.L2Lb;
import org.onosproject.l2lb.api.L2LbEvent;
import org.onosproject.l2lb.api.L2LbAdminService;
import org.onosproject.l2lb.api.L2LbId;
import org.onosproject.l2lb.api.L2LbListener;
import org.onosproject.l2lb.api.L2LbMode;
import org.onosproject.l2lb.api.L2LbService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultNextObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.NextObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.net.packet.PacketService;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.MapEventListener;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

@Component(
    immediate = true,
    service = {
        L2LbService.class,
        L2LbAdminService.class
    }
)
public class L2LbManager implements L2LbService, L2LbAdminService {

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private InterfaceService interfaceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private StorageService storageService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private FlowObjectiveService flowObjService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    private DeviceService deviceService;

    private static final Logger log = getLogger(L2LbManager.class);
    private static final String APP_NAME = "org.onosproject.l2lb";

    private ApplicationId appId;
    private ConsistentMap<L2LbId, L2Lb> l2LbStore;
    private ConsistentMap<L2LbId, Integer> l2LbNextStore;
    private Set<L2LbListener> listeners = Sets.newConcurrentHashSet();

    private ExecutorService l2LbEventExecutor;
    private ExecutorService l2LbProvExecutor;
    private MapEventListener<L2LbId, L2Lb> l2LbStoreListener;
    // TODO build CLI to view and clear the next store
    private MapEventListener<L2LbId, Integer> l2LbNextStoreListener;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(APP_NAME);

        l2LbEventExecutor = Executors.newSingleThreadExecutor(groupedThreads("l2lb-event", "%d", log));
        l2LbProvExecutor = Executors.newSingleThreadExecutor(groupedThreads("l2lb-prov", "%d", log));
        l2LbStoreListener = new L2LbStoreListener();
        l2LbNextStoreListener = new L2LbNextStoreListener();

        KryoNamespace serializer = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .register(L2Lb.class)
                .register(L2LbId.class)
                .register(L2LbMode.class)
                .build();
        l2LbStore = storageService.<L2LbId, L2Lb>consistentMapBuilder()
                .withName("onos-l2lb-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        l2LbStore.addListener(l2LbStoreListener);
        l2LbNextStore = storageService.<L2LbId, Integer>consistentMapBuilder()
                .withName("onos-l2lb-next-store")
                .withRelaxedReadConsistency()
                .withSerializer(Serializer.using(serializer))
                .build();
        l2LbNextStore.addListener(l2LbNextStoreListener);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        l2LbStore.removeListener(l2LbStoreListener);
        l2LbNextStore.removeListener(l2LbNextStoreListener);

        l2LbEventExecutor.shutdown();

        log.info("Stopped");
    }

    @Override
    public void addListener(L2LbListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(L2LbListener listener) {
        listeners.remove(listener);
    }

    @Override
    public L2Lb createOrUpdate(DeviceId deviceId, int key, Set<PortNumber> ports, L2LbMode mode) {
        L2LbId l2LbId = new L2LbId(deviceId, key);
        log.debug("Putting {} -> {} {} into L2 load balancer store", l2LbId, mode, ports);
        return Versioned.valueOrNull(l2LbStore.put(l2LbId, new L2Lb(l2LbId, ports, mode)));
    }

    @Override
    public L2Lb remove(DeviceId deviceId, int key) {
        L2LbId l2LbId = new L2LbId(deviceId, key);
        log.debug("Removing {} from L2 load balancer store", l2LbId);
        return Versioned.valueOrNull(l2LbStore.remove(l2LbId));
    }

    @Override
    public Map<L2LbId, L2Lb> getL2Lbs() {
        return l2LbStore.asJavaMap();
    }

    @Override
    public L2Lb getL2Lb(DeviceId deviceId, int key) {
        return Versioned.valueOrNull(l2LbStore.get(new L2LbId(deviceId, key)));
    }

    @Override
    public Map<L2LbId, Integer> getL2LbNexts() {
        return l2LbNextStore.asJavaMap();
    }

    @Override
    public int getL2LbNexts(DeviceId deviceId, int key) {
        return Versioned.valueOrNull(l2LbNextStore.get(new L2LbId(deviceId, key)));
    }

    private class L2LbStoreListener implements MapEventListener<L2LbId, L2Lb> {
        public void event(MapEvent<L2LbId, L2Lb> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("L2Lb {} insert new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.ADDED, event.newValue().value(), null));
                    populateL2Lb(event.newValue().value());
                    break;
                case REMOVE:
                    log.debug("L2Lb {} remove new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.REMOVED, null, event.oldValue().value()));
                    revokeL2Lb(event.oldValue().value());
                    break;
                case UPDATE:
                    log.debug("L2Lb {} update new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    post(new L2LbEvent(L2LbEvent.Type.UPDATED, event.newValue().value(),
                            event.oldValue().value()));
                    updateL2Lb(event.newValue().value(), event.oldValue().value());
                    break;
                default:
                    break;
            }
        }
    }

    private class L2LbNextStoreListener implements MapEventListener<L2LbId, Integer> {
        public void event(MapEvent<L2LbId, Integer> event) {
            switch (event.type()) {
                case INSERT:
                    log.debug("L2Lb next {} insert new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                case REMOVE:
                    log.debug("L2Lb next {} remove new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                case UPDATE:
                    log.debug("L2Lb next {} update new={}, old={}", event.key(), event.newValue(), event.oldValue());
                    break;
                default:
                    break;
            }
        }
    }

    private void post(L2LbEvent l2LbEvent) {
        l2LbEventExecutor.execute(() -> {
            for (L2LbListener l : listeners) {
                l.event(l2LbEvent);
            }
        });
    }

    // TODO repopulate when device reconnect
    private void populateL2Lb(L2Lb l2Lb) {
        DeviceId deviceId = l2Lb.l2LbId().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not the master of {}. Skip populateL2Lb {}", deviceId, l2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            L2LbObjectiveContext context = new L2LbObjectiveContext(l2Lb.l2LbId());
            NextObjective nextObj = nextObjBuilder(l2Lb.l2LbId(), l2Lb.ports()).add(context);

            flowObjService.next(deviceId, nextObj);
            l2LbNextStore.put(l2Lb.l2LbId(), nextObj.id());
        });
    }

    private void revokeL2Lb(L2Lb l2Lb) {
        DeviceId deviceId = l2Lb.l2LbId().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not the master of {}. Skip revokeL2Lb {}", deviceId, l2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            l2LbNextStore.remove(l2Lb.l2LbId());
            // NOTE group is not removed and we rely on the garbage collection mechanism
        });
    }

    private void updateL2Lb(L2Lb newL2Lb, L2Lb oldL2Lb) {
        DeviceId deviceId = newL2Lb.l2LbId().deviceId();
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.debug("Not the master of {}. Skip updateL2Lb {}", deviceId, newL2Lb.l2LbId());
            return;
        }

        l2LbProvExecutor.execute(() -> {
            L2LbObjectiveContext context = new L2LbObjectiveContext(newL2Lb.l2LbId());
            Set<PortNumber> portsToBeAdded = Sets.difference(newL2Lb.ports(), oldL2Lb.ports());
            Set<PortNumber> portsToBeRemoved = Sets.difference(oldL2Lb.ports(), newL2Lb.ports());

            flowObjService.next(deviceId, nextObjBuilder(newL2Lb.l2LbId(), portsToBeAdded).addToExisting(context));
            flowObjService.next(deviceId, nextObjBuilder(newL2Lb.l2LbId(), portsToBeRemoved)
                    .removeFromExisting(context));
        });
    }

    private NextObjective.Builder nextObjBuilder(L2LbId l2LbId, Set<PortNumber> ports) {
        return nextObjBuilder(l2LbId, ports, flowObjService.allocateNextId());
    }

    private NextObjective.Builder nextObjBuilder(L2LbId l2LbId, Set<PortNumber> ports, int nextId) {
        // TODO replace logical l2lb port
        TrafficSelector meta = DefaultTrafficSelector.builder()
                .matchInPort(PortNumber.portNumber(l2LbId.key())).build();
        NextObjective.Builder nextObjBuilder = DefaultNextObjective.builder()
                .withId(nextId)
                .withMeta(meta)
                .withType(NextObjective.Type.HASHED)
                .fromApp(appId);
        ports.forEach(port -> {
            TrafficTreatment treatment = DefaultTrafficTreatment.builder().setOutput(port).build();
            nextObjBuilder.addTreatment(treatment);
        });
        return nextObjBuilder;
    }

    private final class L2LbObjectiveContext implements ObjectiveContext {
        private final L2LbId l2LbId;

        private L2LbObjectiveContext(L2LbId l2LbId) {
            this.l2LbId = l2LbId;
        }

        @Override
        public void onSuccess(Objective objective) {
            NextObjective nextObj = (NextObjective) objective;
            log.debug("Added nextobj {} for L2 load balancer {}", nextObj, l2LbId);
        }

        @Override
        public void onError(Objective objective, ObjectiveError error) {
            NextObjective nextObj = (NextObjective) objective;
            log.debug("Failed to add nextobj {} for L2 load balancer {}", nextObj, l2LbId);
        }

    }
}