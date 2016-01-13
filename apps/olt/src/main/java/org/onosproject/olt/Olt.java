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
package org.onosproject.olt;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
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
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.flowobjective.ObjectiveContext;
import org.onosproject.net.flowobjective.ObjectiveError;
import org.onosproject.olt.api.AccessDeviceEvent;
import org.onosproject.olt.api.AccessDeviceListener;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
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

        log.info("Started with Application ID {}", appId.id());
    }

    @Deactivate
    public void deactivate() {
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

    private void provisionVlans(DeviceId deviceId, PortNumber uplinkPort,
                                PortNumber subscriberPort,
                                VlanId subscriberVlan, VlanId deviceVlan,
                                Optional<VlanId> defaultVlan) {

        CompletableFuture<ObjectiveError> downFuture = new CompletableFuture();
        CompletableFuture<ObjectiveError> upFuture = new CompletableFuture();

        TrafficSelector upstream = DefaultTrafficSelector.builder()
                .matchVlanId((defaultVlan.isPresent()) ? defaultVlan.get() : DEFAULT_VLAN)
                .matchInPort(subscriberPort)
                .build();

        TrafficSelector downstream = DefaultTrafficSelector.builder()
                .matchVlanId(deviceVlan)
                .matchInPort(uplinkPort)
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
                .setVlanId((defaultVlan.isPresent()) ? defaultVlan.get() : DEFAULT_VLAN)
                .setOutput(subscriberPort)
                .build();


        ForwardingObjective upFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(upstream)
                .fromApp(appId)
                .withTreatment(upstreamTreatment)
                .add(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        upFuture.complete(null);
                    }

                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        upFuture.complete(error);
                    }
                });

        ForwardingObjective downFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(downstream)
                .fromApp(appId)
                .withTreatment(downstreamTreatment)
                .add(new ObjectiveContext() {
                    @Override
                    public void onSuccess(Objective objective) {
                        downFuture.complete(null);
                    }

                    @Override
                    public void onError(Objective objective, ObjectiveError error) {
                        downFuture.complete(error);
                    }
                });

        flowObjectiveService.forward(deviceId, upFwd);
        flowObjectiveService.forward(deviceId, downFwd);

        upFuture.thenAcceptBothAsync(downFuture, (upStatus, downStatus) -> {
            if (upStatus == null && downStatus == null) {
                post(new AccessDeviceEvent(AccessDeviceEvent.Type.SUBSCRIBER_REGISTERED,
                                           deviceId,
                                           deviceVlan,
                                           subscriberVlan));
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

    @Override
    public void removeSubscriber(ConnectPoint port) {
        throw new UnsupportedOperationException("Not yet implemented");
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
                case PORT_ADDED:
                case PORT_UPDATED:
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
                case DEVICE_UPDATED:
                case DEVICE_SUSPENDED:
                case DEVICE_AVAILABILITY_CHANGED:
                case PORT_REMOVED:
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

}
