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

import com.google.common.base.Strings;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.VlanId;
import org.onlab.util.Tools;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
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
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.FlowObjectiveService;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provisions rules on access devices.
 */
@Service
@Component(immediate = true)
public class Olt implements AccessDeviceService {
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
    public static final int OFFSET = 200;

    public static final int UPLINK_PORT = 129;
    public static final int GFAST_UPLINK_PORT = 100;

    public static final String OLT_DEVICE = "of:90e2ba82f97791e9";
    public static final String GFAST_DEVICE = "of:0011223344551357";

    @Property(name = "uplinkPort", intValue = UPLINK_PORT,
            label = "The OLT's uplink port number")
    private int uplinkPort = UPLINK_PORT;

    @Property(name = "gfastUplink", intValue = GFAST_UPLINK_PORT,
            label = "The OLT's uplink port number")
    private int gfastUplink = GFAST_UPLINK_PORT;

    //TODO: replace this with an annotation lookup
    @Property(name = "oltDevice", value = OLT_DEVICE,
            label = "The OLT device id")
    private String oltDevice = OLT_DEVICE;

    @Property(name = "gfastDevice", value = GFAST_DEVICE,
            label = "The gfast device id")
    private String gfastDevice = GFAST_DEVICE;

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

        /*deviceService.addListener(deviceListener);

        deviceService.getPorts(DeviceId.deviceId(oltDevice)).stream().forEach(
                port -> {
                    if (!port.number().isLogical() && port.isEnabled()) {
                        short vlanId = fetchVlanId(port.number());
                        if (vlanId > 0) {
                            provisionVlanOnPort(oltDevice, uplinkPort, port.number(), (short) 7);
                            provisionVlanOnPort(oltDevice, uplinkPort, port.number(), vlanId);
                        }
                    }
                }
        );*/


        deviceService.getPorts(DeviceId.deviceId(gfastDevice)).stream()
                .filter(port -> !port.number().isLogical())
                .filter(Port::isEnabled)
                .forEach(port -> {
                            short vlanId = (short) (fetchVlanId(port.number()) + OFFSET);
                            if (vlanId > 0) {
                                provisionVlanOnPort(gfastDevice, gfastUplink, port.number(), vlanId);
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

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String s = Tools.get(properties, "uplinkPort");
        uplinkPort = Strings.isNullOrEmpty(s) ? UPLINK_PORT : Integer.parseInt(s);

        s = Tools.get(properties, "oltDevice");
        oltDevice = Strings.isNullOrEmpty(s) ? OLT_DEVICE : s;
    }

    private short fetchVlanId(PortNumber port) {
        long p = port.toLong() + OFFSET;
        if (p > 4095) {
            log.warn("Port Number {} exceeds vlan max", port);
            return -1;
        }
        return (short) p;
    }

    private void provisionVlanOnPort(String deviceId, int uplinkPort, PortNumber p, short vlanId) {
        DeviceId did = DeviceId.deviceId(deviceId);

        TrafficSelector upstream = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId(vlanId))
                .matchInPort(p)
                .build();

        TrafficSelector downStream = DefaultTrafficSelector.builder()
                .matchVlanId(VlanId.vlanId(vlanId))
                .matchInPort(PortNumber.portNumber(uplinkPort))
                .build();

        TrafficTreatment upstreamTreatment = DefaultTrafficTreatment.builder()
                .setOutput(PortNumber.portNumber(uplinkPort))
                .build();

        TrafficTreatment downStreamTreatment = DefaultTrafficTreatment.builder()
                .setOutput(p)
                .build();


        ForwardingObjective upFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(upstream)
                .fromApp(appId)
                .withTreatment(upstreamTreatment)
                .add();

        ForwardingObjective downFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(downStream)
                .fromApp(appId)
                .withTreatment(downStreamTreatment)
                .add();

        flowObjectiveService.forward(did, upFwd);
        flowObjectiveService.forward(did, downFwd);
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

        TrafficSelector upstream = DefaultTrafficSelector.builder()
                .matchVlanId((defaultVlan.isPresent()) ? defaultVlan.get() : DEFAULT_VLAN)
                .matchInPort(subscriberPort)
                .build();

        TrafficSelector downstream = DefaultTrafficSelector.builder()
                .matchVlanId(deviceVlan)
                .matchInPort(uplinkPort)
                .build();

        TrafficTreatment upstreamTreatment = DefaultTrafficTreatment.builder()
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
                .add();

        ForwardingObjective downFwd = DefaultForwardingObjective.builder()
                .withFlag(ForwardingObjective.Flag.VERSATILE)
                .withPriority(1000)
                .makePermanent()
                .withSelector(downstream)
                .fromApp(appId)
                .withTreatment(downstreamTreatment)
                .add();

        flowObjectiveService.forward(deviceId, upFwd);
        flowObjectiveService.forward(deviceId, downFwd);
    }

    @Override
    public void removeSubscriber(ConnectPoint port) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceId devId = DeviceId.deviceId(oltDevice);
            switch (event.type()) {
                case PORT_ADDED:
                case PORT_UPDATED:
                    if (devId.equals(event.subject().id()) && event.port().isEnabled()) {
                        short vlanId = fetchVlanId(event.port().number());
                        provisionVlanOnPort(gfastDevice, uplinkPort, event.port().number(), vlanId);
                    }
                    break;
                case DEVICE_ADDED:
                case DEVICE_UPDATED:
                case DEVICE_REMOVED:
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
