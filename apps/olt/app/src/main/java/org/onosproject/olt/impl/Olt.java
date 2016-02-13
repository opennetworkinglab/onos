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
package org.onosproject.olt.impl;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.EthType;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flowobjective.DefaultFilteringObjective;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.olt.AccessDeviceConfig;
import org.onosproject.olt.AccessDeviceData;
import org.onosproject.olt.AccessDeviceEvent;
import org.onosproject.olt.AccessDeviceListener;
import org.onosproject.olt.AccessDeviceService;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provisions rules on access devices.
 */
@Service
@Component(immediate = true)
public class Olt
        extends AbstractListenerManager<AccessDeviceEvent, AccessDeviceListener>
        implements AccessDeviceService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowObjectiveService flowObjectiveService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry networkConfig;

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private ApplicationId appId;

    private static final VlanId DEFAULT_VLAN = VlanId.vlanId((short) 0);

    private ExecutorService oltInstallers = Executors.newFixedThreadPool(4,
                                                                         groupedThreads("onos/olt-service",
                                                                                        "olt-installer-%d"));

    private Map<DeviceId, AccessDeviceData> oltData = new ConcurrentHashMap<>();

    private Map<ConnectPoint, Set<ForwardingObjective.Builder>> objectives =
            Maps.newConcurrentMap();

    private Map<ConnectPoint, VlanId> subscribers = Maps.newConcurrentMap();

    private InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();
    private static final Class<AccessDeviceConfig> CONFIG_CLASS =
            AccessDeviceConfig.class;

    private ConfigFactory<DeviceId, AccessDeviceConfig> configFactory =
            new ConfigFactory<DeviceId, AccessDeviceConfig>(
                    SubjectFactories.DEVICE_SUBJECT_FACTORY, CONFIG_CLASS, "accessDevice") {
                @Override
                public AccessDeviceConfig createConfig() {
                    return new AccessDeviceConfig();
                }
            };


    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.olt");

        eventDispatcher.addSink(AccessDeviceEvent.class, listenerRegistry);

        networkConfig.registerConfigFactory(configFactory);
        networkConfig.addListener(configListener);


        networkConfig.getSubjects(DeviceId.class, AccessDeviceConfig.class).forEach(
                subject -> {
                    AccessDeviceConfig config = networkConfig.getConfig(subject, AccessDeviceConfig.class);
                    if (config != null) {
                        AccessDeviceData data = config.getOlt();
                        oltData.put(data.deviceId(), data);
                    }
                }
        );

        oltData.keySet().stream()
                .flatMap(did -> deviceService.getPorts(did).stream())
                .filter(p -> !oltData.get(p.element().id()).uplink().equals(p.number()))
                .filter(p -> p.isEnabled())
                .forEach(p -> processFilteringObjectives((DeviceId) p.element().id(),
                                                         p.number(), true));

        deviceService.addListener(deviceListener);

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        networkConfig.removeListener(configListener);
        networkConfig.unregisterConfigFactory(configFactory);
        log.info("Stopped");
    }

    @Override
    public void provisionSubscriber(ConnectPoint port, VlanId vlan) {
        AccessDeviceData olt = oltData.get(port.deviceId());

        if (olt == null) {
            log.warn("No data found for OLT device {}", port.deviceId());
            return;
        }

        provisionVlans(olt.deviceId(), olt.uplink(), port.port(), vlan, olt.vlan(),
                       olt.defaultVlan());
    }

    @Override
    public void removeSubscriber(ConnectPoint port) {
        AccessDeviceData olt = oltData.get(port.deviceId());

        if (olt == null) {
            log.warn("No data found for OLT device {}", port.deviceId());
            return;
        }

        unprovisionSubscriber(olt.deviceId(), olt.uplink(), port.port(), olt.vlan());

    }

    private void unprovisionSubscriber(DeviceId deviceId, PortNumber uplink,
                                       PortNumber subscriberPort, VlanId deviceVlan) {

        //FIXME: This method is slightly ugly but it'll do until we have a better
        // way to remove flows from the flow store.

        CompletableFuture<ObjectiveError> downFuture = new CompletableFuture();
        CompletableFuture<ObjectiveError> upFuture = new CompletableFuture();

        ConnectPoint cp = new ConnectPoint(deviceId, subscriberPort);

        VlanId subscriberVlan = subscribers.remove(cp);

        Set<ForwardingObjective.Builder> fwds = objectives.remove(cp);

        if (fwds == null || fwds.size() != 2) {
            log.warn("Unknown or incomplete subscriber at {}", cp);
            return;
        }


        fwds.stream().forEach(
                fwd -> flowObjectiveService.forward(deviceId,
                                                    fwd.remove(new ObjectiveContext() {
                                                        @Override
                                                        public void onSuccess(Objective objective) {
                                                            upFuture.complete(null);
                                                        }

                                                        @Override
                                                        public void onError(Objective objective, ObjectiveError error) {
                                                            upFuture.complete(error);
                                                        }
                                                    })));

        upFuture.thenAcceptBothAsync(downFuture, (upStatus, downStatus) -> {
            if (upStatus == null && downStatus == null) {
                post(new AccessDeviceEvent(AccessDeviceEvent.Type.SUBSCRIBER_UNREGISTERED,
                                           deviceId,
                                           deviceVlan,
                                           subscriberVlan));
                processFilteringObjectives(deviceId, subscriberPort, true);
            } else if (downStatus != null) {
                log.error("Subscriber with vlan {} on device {} " +
                                  "on port {} failed downstream uninstallation: {}",
                          subscriberVlan, deviceId, subscriberPort, downStatus);
            } else if (upStatus != null) {
                log.error("Subscriber with vlan {} on device {} " +
                                  "on port {} failed upstream uninstallation: {}",
                          subscriberVlan, deviceId, subscriberPort, upStatus);
            }
        }, oltInstallers);

    }

    private void provisionVlans(DeviceId deviceId, PortNumber uplinkPort,
                                PortNumber subscriberPort,
                                VlanId subscriberVlan, VlanId deviceVlan,
                                Optional<VlanId> defaultVlan) {

        CompletableFuture<ObjectiveError> downFuture = new CompletableFuture();
        CompletableFuture<ObjectiveError> upFuture = new CompletableFuture();

        TrafficSelector upstream = DefaultTrafficSelector.builder()
                .matchVlanId(defaultVlan.orElse(DEFAULT_VLAN))
                .matchInPort(subscriberPort)
                .build();

        TrafficSelector downstream = DefaultTrafficSelector.builder()
                .matchVlanId(deviceVlan)
                .matchInPort(uplinkPort)
                .matchInnerVlanId(subscriberVlan)
                .build();

        TrafficTreatment upstreamTreatment = DefaultTrafficTreatment.builder()
                .pushVlan()
                .setVlanId(subscriberVlan)
                .pushVlan()
                .setVlanId(deviceVlan)
                .setOutput(uplinkPort)
                .build();

        TrafficTreatment downstreamTreatment = DefaultTrafficTreatment.builder()
                .popVlan()
                .setVlanId(defaultVlan.orElse(DEFAULT_VLAN))
                .setOutput(subscriberPort)
                .build();


        ForwardingObjective.Builder upFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(upstream)
                .fromApp(appId)
                .withTreatment(upstreamTreatment);


        ForwardingObjective.Builder downFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(downstream)
                .fromApp(appId)
                .withTreatment(downstreamTreatment);

        ConnectPoint cp = new ConnectPoint(deviceId, subscriberPort);

        subscribers.put(cp, subscriberVlan);
        objectives.put(cp, Sets.newHashSet(upFwd, downFwd));


        flowObjectiveService.forward(deviceId, upFwd.add(new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                upFuture.complete(null);
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                upFuture.complete(error);
            }
        }));


        flowObjectiveService.forward(deviceId, downFwd.add(new ObjectiveContext() {
            @Override
            public void onSuccess(Objective objective) {
                downFuture.complete(null);
            }

            @Override
            public void onError(Objective objective, ObjectiveError error) {
                downFuture.complete(error);
            }
        }));

        upFuture.thenAcceptBothAsync(downFuture, (upStatus, downStatus) -> {
            if (upStatus == null && downStatus == null) {
                post(new AccessDeviceEvent(AccessDeviceEvent.Type.SUBSCRIBER_REGISTERED,
                                           deviceId,
                                           deviceVlan,
                                           subscriberVlan));

                processFilteringObjectives(deviceId, subscriberPort, false);
            } else if (downStatus != null) {
                log.error("Subscriber with vlan {} on device {} " +
                                  "on port {} failed downstream installation: {}",
                          subscriberVlan, deviceId, subscriberPort, downStatus);
            } else if (upStatus != null) {
                log.error("Subscriber with vlan {} on device {} " +
                                  "on port {} failed upstream installation: {}",
                          subscriberVlan, deviceId, subscriberPort, upStatus);
            }
        }, oltInstallers);

    }

    private void processFilteringObjectives(DeviceId devId, PortNumber port, boolean install) {
        DefaultFilteringObjective.Builder builder = DefaultFilteringObjective.builder();

        FilteringObjective eapol = (install ? builder.permit() : builder.deny())
                .withKey(Criteria.matchInPort(port))
                .addCondition(Criteria.matchEthType(EthType.EtherType.EAPOL.ethType()))
                .withMeta(DefaultTrafficTreatment.builder()
                                  .setOutput(PortNumber.CONTROLLER).build())
                .fromApp(appId)
                .withPriority(1000)
                .add(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        log.info("Eapol filter for {} on {} installed.",
                                 devId, port);
                    }

                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        log.info("Eapol filter for {} on {} failed because {}",
                                 devId, port, error);
                    }
                });

        flowObjectiveService.filter(devId, eapol);

    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId devId = event.subject().id();
            if (!oltData.containsKey(devId)) {
                log.debug("Device {} is not an OLT", devId);
                return;
            }
            switch (event.type()) {
                //TODO: Port handling and bookkeeping should be inproved once
                // olt firmware handles correct behaviour.
                case PORT_ADDED:
                    if (!oltData.get(devId).uplink().equals(event.port().number()) &&
                            event.port().isEnabled()) {
                        processFilteringObjectives(devId, event.port().number(), true);
                    }
                    break;
                case PORT_REMOVED:
                    AccessDeviceData olt = oltData.get(devId);
                    unprovisionSubscriber(devId, olt.uplink(),
                                          event.port().number(),
                                          olt.vlan());
                    if (!oltData.get(devId).uplink().equals(event.port().number()) &&
                            event.port().isEnabled()) {
                        processFilteringObjectives(devId, event.port().number(), false);
                    }
                    break;
                case PORT_UPDATED:
                    if (oltData.get(devId).uplink().equals(event.port().number())) {
                        break;
                    }
                    if (event.port().isEnabled()) {
                        processFilteringObjectives(devId, event.port().number(), true);
                    } else {
                        processFilteringObjectives(devId, event.port().number(), false);
                    }
                    break;
                case DEVICE_ADDED:
                    post(new AccessDeviceEvent(
                            AccessDeviceEvent.Type.DEVICE_CONNECTED, devId,
                            null, null));
                    break;
                case DEVICE_REMOVED:
                    post(new AccessDeviceEvent(
                            AccessDeviceEvent.Type.DEVICE_DISCONNECTED, devId,
                            null, null));
                    break;
                case DEVICE_AVAILABILITY_CHANGED:
                    if (deviceService.isAvailable(devId)) {
                        post(new AccessDeviceEvent(
                                AccessDeviceEvent.Type.DEVICE_CONNECTED, devId,
                                null, null));
                    } else {
                        post(new AccessDeviceEvent(
                                AccessDeviceEvent.Type.DEVICE_DISCONNECTED, devId,
                                null, null));
                    }
                    break;
                case DEVICE_UPDATED:
                case DEVICE_SUSPENDED:
                case PORT_STATS_UPDATED:
                default:
                    return;
            }
        }
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {
        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {

                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    if (event.configClass().equals(CONFIG_CLASS)) {
                        AccessDeviceConfig config =
                                networkConfig.getConfig((DeviceId) event.subject(), CONFIG_CLASS);
                        if (config != null) {
                            oltData.put(config.getOlt().deviceId(), config.getOlt());
                            provisionDefaultFlows((DeviceId) event.subject());
                        }
                    }
                    break;
                case CONFIG_UNREGISTERED:
                case CONFIG_REMOVED:
                default:
                    break;
            }
        }
    }

    private void provisionDefaultFlows(DeviceId deviceId) {
        List<Port> ports = deviceService.getPorts(deviceId);

        ports.stream()
                .filter(p -> !oltData.get(p.element().id()).uplink().equals(p.number()))
                .filter(p -> p.isEnabled())
                .forEach(p -> processFilteringObjectives((DeviceId) p.element().id(),
                                                         p.number(), true));

    }

}
