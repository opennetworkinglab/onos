/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.provider.of.device.impl;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.driver.HandlerBehaviour;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowController;
import org.onosproject.openflow.controller.OpenFlowEventListener;
import org.onosproject.openflow.controller.OpenFlowOpticalSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.OpenFlowSwitchListener;
import org.onosproject.openflow.controller.PortDescPropertyType;
import org.onosproject.openflow.controller.RoleState;
import org.osgi.service.component.ComponentContext;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescProp;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescPropOptical;
import org.projectfloodlight.openflow.protocol.OFCalientPortDescStatsEntry;
import org.projectfloodlight.openflow.protocol.OFErrorMsg;
import org.projectfloodlight.openflow.protocol.OFErrorType;
import org.projectfloodlight.openflow.protocol.OFExpPort;
import org.projectfloodlight.openflow.protocol.OFExpPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFExpPortOpticalTransportLayerEntry;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescPropEthernet;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOptical;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortMod;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
import org.projectfloodlight.openflow.protocol.OFPortOpticalTransportLayerClass;
import org.projectfloodlight.openflow.protocol.OFPortOpticalTransportSignalType;
import org.projectfloodlight.openflow.protocol.OFPortReason;
import org.projectfloodlight.openflow.protocol.OFPortState;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFPortStatus;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReplyFlags;
import org.projectfloodlight.openflow.protocol.OFStatsType;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.ver14.OFOpticalPortFeaturesSerializerVer14;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.PortSpeed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.Port.Type.FIBER;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.onosproject.net.optical.device.OduCltPortHelper.oduCltPortDescription;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.onosproject.net.optical.device.OtuPortHelper.otuPortDescription;
import static org.onosproject.openflow.controller.Dpid.dpid;
import static org.onosproject.openflow.controller.Dpid.uri;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider which uses an OpenFlow controller to detect network
 * infrastructure devices.
 */
@Component(immediate = true)
public class OpenFlowDeviceProvider extends AbstractProvider implements DeviceProvider {

    private static final Logger LOG = getLogger(OpenFlowDeviceProvider.class);

    // TODO Some duplicate with one defined in OpticalAnnotations
    // slice out optical specific handling and consolidate.
    /**
     * Annotation key for minimum frequency in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_MIN_FREQ_HZ = "minFrequency";

    /**
     * Annotation key for maximum frequency in Hz.
     * Value is expected be an integer.
     */
    public static final String AK_MAX_FREQ_HZ = "maxFrequency";

    /**
     * Annotation key for grid in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_GRID_HZ = "grid";


    /**
     * Annotation key for minimum frequency in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_TX_MIN_FREQ_HZ = "txMinFrequency";

    /**
     * Annotation key for maximum frequency in Hz.
     * Value is expected be an integer.
     */
    public static final String AK_TX_MAX_FREQ_HZ = "txMaxFrequency";

    /**
     * Annotation key for grid in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_TX_GRID_HZ = "txGrid";

    /**
     * Annotation key for minimum frequency in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_RX_MIN_FREQ_HZ = "rxMinFrequency";

    /**
     * Annotation key for maximum frequency in Hz.
     * Value is expected be an integer.
     */
    public static final String AK_RX_MAX_FREQ_HZ = "rxMaxFrequency";

    /**
     * Annotation key for grid in Hz.
     * Value is expected to be an integer.
     */
    public static final String AK_RX_GRID_HZ = "rxGrid";

    //TODO consider renaming KBPS and MBPS (as they are used to convert by division)
    private static final long KBPS = 1_000;
    private static final long MBPS = 1_000 * 1_000;
    private static final Frequency FREQ50 = Frequency.ofGHz(50);
    private static final Frequency FREQ191_7 = Frequency.ofGHz(191_700);
    private static final Frequency FREQ4_4 = Frequency.ofGHz(4_400);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DriverService driverService;

    private DeviceProviderService providerService;

    private final InternalDeviceProvider listener = new InternalDeviceProvider();

    private static final String POLL_PROP_NAME = "portStatsPollFrequency";
    private static final int POLL_INTERVAL = 5;
    @Property(name = POLL_PROP_NAME, intValue = POLL_INTERVAL,
    label = "Frequency (in seconds) for polling switch Port statistics")
    private int portStatsPollFrequency = POLL_INTERVAL;

    private final Timer timer = new Timer("onos-openflow-portstats-collector");

    private HashMap<Dpid, PortStatsCollector> collectors = Maps.newHashMap();

    /**
     * Creates an OpenFlow device provider.
     */
    public OpenFlowDeviceProvider() {
        super(new ProviderId("of", "org.onosproject.provider.openflow"));
    }

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        providerService = providerRegistry.register(this);
        controller.addListener(listener);
        controller.addEventListener(listener);

        modified(context);

        connectInitialDevices();
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        listener.disable();
        controller.removeListener(listener);
        providerRegistry.unregister(this);
        collectors.values().forEach(PortStatsCollector::stop);
        collectors.clear();
        providerService = null;
        LOG.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context != null ? context.getProperties() : new Properties();
        int newPortStatsPollFrequency;
        try {
            String s = get(properties, POLL_PROP_NAME);
            newPortStatsPollFrequency = isNullOrEmpty(s) ? portStatsPollFrequency : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newPortStatsPollFrequency = portStatsPollFrequency;
        }

        if (newPortStatsPollFrequency != portStatsPollFrequency) {
            portStatsPollFrequency = newPortStatsPollFrequency;
            collectors.values().forEach(psc -> psc.adjustPollInterval(portStatsPollFrequency));
        }

        LOG.info("Settings: portStatsPollFrequency={}", portStatsPollFrequency);
    }

    private void connectInitialDevices() {
        for (OpenFlowSwitch sw : controller.getSwitches()) {
            try {
                listener.switchAdded(new Dpid(sw.getId()));
            } catch (Exception e) {
                LOG.warn("Failed initially adding {} : {}", sw.getStringId(), e.getMessage());
                LOG.debug("Error details:", e);
                // disconnect to trigger switch-add later
                sw.disconnectSwitch();
            }
        }
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        return sw != null && sw.isConnected();
    }

    @Override
    public void triggerProbe(DeviceId deviceId) {
        LOG.debug("Triggering probe on device {}", deviceId);

        final Dpid dpid = dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            LOG.error("Failed to probe device {} on sw={}", deviceId, sw);
            providerService.deviceDisconnected(deviceId);
            return;
        } else {
            LOG.trace("Confirmed device {} connection", deviceId);
        }

        // Prompt an update of port information. We can use any XID for this.
        OFFactory fact = sw.factory();
        switch (fact.getVersion()) {
            case OF_10:
                sw.sendMsg(fact.buildFeaturesRequest().setXid(0).build());
                break;
            case OF_13:
            case OF_14:
            case OF_15:
                sw.sendMsg(fact.buildPortDescStatsRequest().setXid(0).build());
                break;
            default:
                LOG.warn("Unhandled protocol version");
        }
    }

    @Override
    public void roleChanged(DeviceId deviceId, MastershipRole newRole) {
        switch (newRole) {
            case MASTER:
                controller.setRole(dpid(deviceId.uri()), RoleState.MASTER);
                break;
            case STANDBY:
                controller.setRole(dpid(deviceId.uri()), RoleState.EQUAL);
                break;
            case NONE:
                controller.setRole(dpid(deviceId.uri()), RoleState.SLAVE);
                break;
            default:
                LOG.error("Unknown Mastership state : {}", newRole);

        }
        LOG.debug("Accepting mastership role change to {} for device {}", newRole, deviceId);
    }

    @Override
    public void changePortState(DeviceId deviceId, PortNumber portNumber,
                                boolean enable) {
        final Dpid dpid = dpid(deviceId.uri());
        OpenFlowSwitch sw = controller.getSwitch(dpid);
        if (sw == null || !sw.isConnected()) {
            LOG.error("Failed to change portState on device {}", deviceId);
            return;
        }
        OFPortMod.Builder pmb = sw.factory().buildPortMod();
        OFPort port = OFPort.of((int) portNumber.toLong());
        pmb.setPortNo(port);
        Set<OFPortConfig> portConfig = EnumSet.noneOf(OFPortConfig.class);
        if (!enable) {
            portConfig.add(OFPortConfig.PORT_DOWN);
        }
        pmb.setConfig(portConfig);
        Set<OFPortConfig> portMask = EnumSet.noneOf(OFPortConfig.class);
        portMask.add(OFPortConfig.PORT_DOWN);
        pmb.setMask(portMask);
        pmb.setAdvertise(0x0);
        for (OFPortDesc pd : sw.getPorts()) {
            if (pd.getPortNo().equals(port)) {
                pmb.setHwAddr(pd.getHwAddr());
                break;
            }
        }
        sw.sendMsg(Collections.singletonList(pmb.build()));
    }

    private void pushPortMetrics(Dpid dpid, List<OFPortStatsEntry> portStatsEntries) {
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));
        Collection<PortStatistics> stats =
                buildPortStatistics(deviceId, ImmutableList.copyOf(portStatsEntries));
        providerService.updatePortStatistics(deviceId, stats);
    }

    private Collection<PortStatistics> buildPortStatistics(DeviceId deviceId,
                                                           List<OFPortStatsEntry> entries) {
        HashSet<PortStatistics> stats = Sets.newHashSet();

        for (OFPortStatsEntry entry : entries) {
            try {
                if (entry == null || entry.getPortNo() == null || entry.getPortNo().getPortNumber() < 0) {
                    continue;
                }
                DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();
                DefaultPortStatistics stat = builder.setDeviceId(deviceId)
                        .setPort(entry.getPortNo().getPortNumber())
                        .setPacketsReceived(entry.getRxPackets().getValue())
                        .setPacketsSent(entry.getTxPackets().getValue())
                        .setBytesReceived(entry.getRxBytes().getValue())
                        .setBytesSent(entry.getTxBytes().getValue())
                        .setPacketsRxDropped(entry.getRxDropped().getValue())
                        .setPacketsTxDropped(entry.getTxDropped().getValue())
                        .setPacketsRxErrors(entry.getRxErrors().getValue())
                        .setPacketsTxErrors(entry.getTxErrors().getValue())
                        .setDurationSec(entry.getVersion() == OFVersion.OF_10 ? 0 : entry.getDurationSec())
                        .setDurationNano(entry.getVersion() == OFVersion.OF_10 ? 0 : entry.getDurationNsec())
                        .build();

                stats.add(stat);
            } catch (Exception e) {
                LOG.warn("Unable to process port stats", e);
            }
        }

        return Collections.unmodifiableSet(stats);
    }

    private class InternalDeviceProvider implements OpenFlowSwitchListener, OpenFlowEventListener {

        private HashMap<Dpid, List<OFPortStatsEntry>> portStatsReplies = new HashMap<>();
        private boolean isDisabled = false;

        @Override
        public void switchAdded(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            if (sw == null) {
                LOG.error("Switch {} is not found", dpid);
                return;
            }

            ChassisId cId = new ChassisId(dpid.value());

            DefaultAnnotations.Builder annotationsBuilder = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PROTOCOL, sw.factory().getVersion().toString())
                    .set(AnnotationKeys.CHANNEL_ID, sw.channelId())
                    .set(AnnotationKeys.MANAGEMENT_ADDRESS, sw.channelId().split(":")[0]);

            // FIXME following ignores driver specified by name
            Driver driver = driverService.getDriver(sw.manufacturerDescription(),
                    sw.hardwareDescription(),
                    sw.softwareDescription());
            // FIXME: The following breaks the STC tests and will require to be revisited.
//            if (driver != null) {
//                annotationsBuilder.set(AnnotationKeys.DRIVER, driver.name());
//            }

            SparseAnnotations annotations = annotationsBuilder.build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), sw.deviceType(),
                                                 sw.manufacturerDescription(),
                                                 sw.hardwareDescription(),
                                                 sw.softwareDescription(),
                                                 sw.serialNumber(),
                                                 cId, annotations);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(sw));

            PortStatsCollector psc = new PortStatsCollector(timer, sw, portStatsPollFrequency);
            stopCollectorIfNeeded(collectors.put(dpid, psc));
            psc.start();

            //figure out race condition for collectors.remove() and collectors.put()
            if (controller.getSwitch(dpid) == null) {
                switchRemoved(dpid);
            }
        }

        private void stopCollectorIfNeeded(PortStatsCollector collector) {
            if (collector != null) {
                collector.stop();
            }
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            stopCollectorIfNeeded(collectors.remove(dpid));
            if (providerService == null) {
                return;
            }
            providerService.deviceDisconnected(deviceId(uri(dpid)));
        }

        @Override
        public void switchChanged(Dpid dpid) {
            LOG.debug("switchChanged({})", dpid);
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            if (sw == null) {
                LOG.error("Switch {} is not found", dpid);
                return;
            }
            final List<PortDescription> ports = buildPortDescriptions(sw);
            LOG.debug("switchChanged({}) {}", did, ports);
            providerService.updatePorts(did, ports);
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            LOG.debug("portChanged({},{})", dpid, status);
            PortDescription portDescription = buildPortDescription(status);
            if (status.getReason() != OFPortReason.DELETE) {
                providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
            } else {
                providerService.deletePort(deviceId(uri(dpid)), portDescription);
            }
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
            LOG.debug("receivedRoleReply({},{},{})", dpid, requested, response);
            MastershipRole request = roleOf(requested);
            MastershipRole reply = roleOf(response);
            providerService.receivedRoleReply(deviceId(uri(dpid)), request, reply);
        }

        /**
         * Translates a RoleState to the corresponding MastershipRole.
         *
         * @param response role state
         * @return a MastershipRole
         */
        private MastershipRole roleOf(RoleState response) {
            switch (response) {
                case MASTER:
                    return MastershipRole.MASTER;
                case EQUAL:
                    return MastershipRole.STANDBY;
                case SLAVE:
                    return MastershipRole.NONE;
                default:
                    LOG.warn("unknown role {}", response);
                    return null;
            }
        }

        /**
         * Builds a list of port descriptions for a given list of ports.
         *
         * @return list of portdescriptions
         */
        private List<PortDescription> buildPortDescriptions(OpenFlowSwitch sw) {
            final List<PortDescription> portDescs = new ArrayList<>(sw.getPorts().size());
            if (!((Device.Type.ROADM.equals(sw.deviceType())) ||
                    (Device.Type.OTN.equals(sw.deviceType())) ||
                    (Device.Type.OPTICAL_AMPLIFIER.equals(sw.deviceType())))) {
                // build regular (=non-optical) Device ports
                sw.getPorts().forEach(port -> portDescs.add(buildPortDescription(port)));
            }

            // TODO handle Optical Device, but plain OF devices(1.4 and later)

            OpenFlowOpticalSwitch opsw;
            switch (sw.deviceType()) {
                case ROADM:
                case OTN:
                case OPTICAL_AMPLIFIER:
                    opsw = (OpenFlowOpticalSwitch) sw;
                    List<OFPortDesc> ports = opsw.getPorts();
                    LOG.debug("SW ID {} , ETH- ODU CLT Ports {}", opsw.getId(), ports);
                    // ODU client ports are reported as ETH
                    ports.forEach(port -> portDescs.add(buildOduCltPortDescription(port)));

                    opsw.getPortTypes().forEach(type -> {
                    List<? extends OFObject> portsOf = opsw.getPortsOf(type);
                    LOG.debug("Ports Of{}", portsOf);
                    portsOf.forEach(
                        op -> {
                            portDescs.add(buildPortDescription(type, op, opsw));
                        }
                     );
                    });
                    break;
                case FIBER_SWITCH:
                    opsw = (OpenFlowOpticalSwitch) sw;
                    opsw.getPortTypes().forEach(type -> {
                        opsw.getPortsOf(type).forEach(op -> {
                            if (op instanceof OFPortDesc) {
                                // ports using standard optical extension
                                // TODO OFMessage -> PortDescription should
                                // probably be a Behaviour
                                portDescs.add(oms(buildPortDescription((OFPortDesc) op)));
                            } else if (op instanceof OFCalientPortDescStatsEntry) {
                                // calient extension
                                portDescs.add(buildPortDescription((OFCalientPortDescStatsEntry) op));
                            } else {
                                LOG.warn("Unexpected FIBER_SWITCH port {} on {}",
                                         op, sw.getStringId());
                            }
                        });
                    });
                    break;
                default:
                    break;
            }

            return portDescs;
        }

        /**
         * Ensures returned PortDescription is an OMS port.
         *
         * @param descr input PortDescription
         * @return OMS PortDescription
         */
        private PortDescription oms(PortDescription descr) {
            // Hack until OFMessage -> PortDescription transformation
            // becomes a Behaviour
            if (descr.type() == Type.OMS) {
                return descr;
            }

            Builder builder = DefaultAnnotations.builder();
            builder.putAll(descr.annotations());

            // set reasonable default when mandatory key is missing
            if (Strings.isNullOrEmpty(descr.annotations().value(AK_MIN_FREQ_HZ))) {
                builder.set(AK_MIN_FREQ_HZ, String.valueOf(Spectrum.O_BAND_MIN.asHz()));
            }

            if (Strings.isNullOrEmpty(descr.annotations().value(AK_MAX_FREQ_HZ))) {
                builder.set(AK_MAX_FREQ_HZ, String.valueOf(Spectrum.O_BAND_MAX.asHz()));
            }

            if (Strings.isNullOrEmpty(descr.annotations().value(AK_GRID_HZ))) {
                builder.set(AK_GRID_HZ, String.valueOf(Frequency.ofGHz(50).asHz()));
            }

            return DefaultPortDescription.builder(descr)
                    .type(Type.OMS)
                    .annotations(builder.build())
                    .build();
        }

        private PortDescription buildOduCltPortDescription(OFPortDesc port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN) &&
                              !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            Long portSpeedInMbps = portSpeed(port);
            CltSignalType sigType = null;

            switch (portSpeedInMbps.toString()) {
                case "1000":
                    sigType = CltSignalType.CLT_1GBE;
                    break;
                case "10000":
                    sigType = CltSignalType.CLT_10GBE;
                    break;
                case "40000":
                    sigType = CltSignalType.CLT_40GBE;
                    break;
                case "100000":
                    sigType = CltSignalType.CLT_100GBE;
                    break;
                default:
                    throw new RuntimeException("Un recognize OduClt speed: " + portSpeedInMbps.toString());
            }

            SparseAnnotations annotations = buildOduCltAnnotation(port);
            return oduCltPortDescription(portNo, enabled, sigType, annotations);
        }

        private SparseAnnotations buildOduCltAnnotation(OFPortDesc port) {
            SparseAnnotations annotations = null;
            String portName = Strings.emptyToNull(port.getName());
            if (portName != null) {
                 annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, portName)
                        .set(AnnotationKeys.STATIC_PORT, Boolean.TRUE.toString()).build();
            }
            return annotations;
        }

        private PortDescription buildPortDescription(PortDescPropertyType ptype, OFObject port,
                OpenFlowOpticalSwitch opsw) {
            if (port instanceof OFPortOptical) {
                return buildPortDescription(ptype, (OFPortOptical) port, opsw);
            }
            return buildPortDescription(ptype, (OFExpPort) port);
        }

        private boolean matchingOtuPortSignalTypes(OFPortOpticalTransportSignalType sigType,
                OduSignalType oduSignalType) {
            switch (sigType) {
            case OTU2:
                if (oduSignalType == OduSignalType.ODU2) {
                    return true;
                }
                break;
            case OTU4:
                if (oduSignalType == OduSignalType.ODU4) {
                    return true;
                }
                break;
            default:
                break;
            }
            return false;
        }
        /**
         * Build a portDescription from a given a port description describing some
         * Optical port.
         *
         * @param ptype description property type.
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(PortDescPropertyType ptype, OFExpPort port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN)
                    && !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString()).build();

            OFExpPortDescPropOpticalTransport firstProp = port.getProperties().get(0);
            OFPortOpticalTransportSignalType sigType = firstProp.getPortSignalType();

            PortDescription portDes = null;
            switch (sigType) {
            case OMSN:
                portDes = omsPortDescription(portNo, enabled,
                        FREQ191_7, FREQ191_7.add(FREQ4_4), FREQ50, annotations);
                break;
            case OCH:
                OFExpPortOpticalTransportLayerEntry entry = firstProp.getFeatures().get(0).getValue().get(0);
                OFPortOpticalTransportLayerClass layerClass =  entry.getLayerClass();
                if (!OFPortOpticalTransportLayerClass.ODU.equals(layerClass)) {
                    LOG.error("Unsupported layer Class {} ", layerClass);
                    return null;
                }

                // convert to ONOS OduSignalType
                OduSignalType oduSignalType = OpenFlowDeviceValueMapper.
                        lookupOduSignalType((byte) entry.getSignalType());
                //OchSignal is needed for OchPortDescription constructor,
                //yet not relevant for tunable OCH port, creating with default parameters
                OchSignal signalId = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 1, 1);

                portDes = ochPortDescription(portNo, enabled,
                                             oduSignalType, true,
                                             signalId, annotations);

                break;
            case OTU2:
            case OTU4:
                entry = firstProp.getFeatures().get(0).getValue().get(0);
                layerClass =  entry.getLayerClass();
                if (!OFPortOpticalTransportLayerClass.ODU.equals(layerClass)) {
                    LOG.error("Unsupported layer Class {} ", layerClass);
                    return null;
                }

                // convert to ONOS OduSignalType
                OduSignalType oduSignalTypeOtuPort = OpenFlowDeviceValueMapper.
                        lookupOduSignalType((byte) entry.getSignalType());
                if (!matchingOtuPortSignalTypes(sigType, oduSignalTypeOtuPort)) {
                    LOG.error("Wrong oduSignalType {} for OTU Port sigType {} ", oduSignalTypeOtuPort, sigType);
                    return null;
                }
                OtuSignalType otuSignalType =
                        ((sigType == OFPortOpticalTransportSignalType.OTU2) ? OtuSignalType.OTU2 :
                            OtuSignalType.OTU4);
                portDes = otuPortDescription(portNo, enabled, otuSignalType, annotations);
                break;
            default:
                break;
            }

            return portDes;
        }

        /**
         * Creates an annotation builder for the port name if one is available.
         *
         * @param portName the port name
         * @param portMac the port mac
         * @return annotation builder containing port name and/or port MAC if any of
         *          the two is found, empty otherwise
         */
        private DefaultAnnotations.Builder makePortAnnotation(String portName, String portMac) {
            DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
            String pName = Strings.emptyToNull(portName);
            String pMac = Strings.emptyToNull(portMac);
            if (pName != null) {
                builder.set(AnnotationKeys.PORT_NAME, pName);
            }
            if (pMac != null) {
                builder.set(AnnotationKeys.PORT_MAC, pMac);
            }
            return builder;
        }

        private String mhzToAnnotation(long freqMhz) {
            // annotations is in Hz
            return Long.toString(freqMhz * 1_000_000);
        }

        private String lambdaToAnnotationHz(long lambda) {
            // ref. OF1.5: wavelength (lambda) as nm * 100

            long c = 299792458; // speed of light in m/s
            // f = c / λ
            // (m/s) * (nm/m) / (nm * 100) * 100
            // annotations is in Hz
            return Long.toString(c * 1_000_000_000 / lambda * 100);
        }

        private PortDescription buildPortDescription14(OFPortDesc port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled =
                    !port.getState().contains(OFPortState.LINK_DOWN) &&
                    !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            Builder annotations = makePortAnnotation(port.getName(),
                                                     port.getHwAddr().toString());

            Optional<OFPortDescPropEthernet> ether = port.getProperties().stream()
                .filter(OFPortDescPropEthernet.class::isInstance)
                .map(OFPortDescPropEthernet.class::cast)
                .findAny();
            if (ether.isPresent()) {
                // ethernet port
                // TODO parse other part of OFPortDescPropEthernet if necessary
                return new DefaultPortDescription(portNo,
                                                  enabled,
                                                  COPPER,
                                                  portSpeed(port),
                                                  annotations.build());
            }

            Optional<OFPortDescPropOptical> optical = port.getProperties().stream()
                    .filter(OFPortDescPropOptical.class::isInstance)
                    .map(OFPortDescPropOptical.class::cast)
                    .findAny();
            if (optical.isPresent()) {
                // optical port

                // FIXME is there a OF version neutral way to access
                // OFOpticalPortFeaturesSerializerVer14

                // wire value for OFOpticalPortFeatures.USE_FREQ
                long useFreq = OFOpticalPortFeaturesSerializerVer14.USE_FREQ_VAL;
                if ((optical.get().getSupported() & useFreq) != 0) {
                    // unit is in Frequency Mhz
                    long rxMinFreq = optical.get().getRxMinFreqLmda();
                    long rxMaxFreq = optical.get().getRxMaxFreqLmda();
                    long txMinFreq = optical.get().getTxMinFreqLmda();
                    long txMaxFreq = optical.get().getTxMaxFreqLmda();

                    annotations.set(AK_RX_MIN_FREQ_HZ, mhzToAnnotation(rxMinFreq));
                    annotations.set(AK_RX_MAX_FREQ_HZ, mhzToAnnotation(rxMaxFreq));

                    annotations.set(AK_TX_MIN_FREQ_HZ, mhzToAnnotation(txMinFreq));
                    annotations.set(AK_TX_MAX_FREQ_HZ, mhzToAnnotation(txMaxFreq));

                    // FIXME pretty confident this is not going to happen
                    // unless Device models Tx/Rx ports as separate port
                    if (rxMinFreq == txMinFreq) {
                        annotations.set(AK_MIN_FREQ_HZ,
                                        mhzToAnnotation(rxMinFreq));
                    }
                    if (rxMaxFreq == txMaxFreq) {
                        annotations.set(AK_MAX_FREQ_HZ,
                                        mhzToAnnotation(rxMaxFreq));
                    }

                } else {
                    // unit is in Lambda nm * 100
                    long rxMin = optical.get().getRxMinFreqLmda();
                    long rxMax = optical.get().getRxMaxFreqLmda();
                    long txMin = optical.get().getTxMinFreqLmda();
                    long txMax = optical.get().getTxMaxFreqLmda();

                    annotations.set(AK_RX_MIN_FREQ_HZ, lambdaToAnnotationHz(rxMin));
                    annotations.set(AK_RX_MAX_FREQ_HZ, lambdaToAnnotationHz(rxMax));

                    annotations.set(AK_TX_MIN_FREQ_HZ, lambdaToAnnotationHz(txMin));
                    annotations.set(AK_TX_MAX_FREQ_HZ, lambdaToAnnotationHz(txMax));

                    // FIXME pretty confident this is not going to happen
                    // unless Device models Tx/Rx ports as separate port
                    if (rxMin == txMin) {
                        annotations.set(AK_MIN_FREQ_HZ,
                                        lambdaToAnnotationHz(rxMin));
                    }
                    if (rxMax == txMax) {
                        annotations.set(AK_MAX_FREQ_HZ,
                                        lambdaToAnnotationHz(rxMax));
                    }

                }
                // TODO parse other part of OFPortDescPropOptical
                // Tx/Rx tunable, ...
                // Power is configurable or now

                // TODO How to determine appropriate port type?

                return new DefaultPortDescription(portNo,
                                                  enabled,
                                                  FIBER,
                                                  portSpeed(port),
                                                  annotations.build());
            }

            // fall back default
            return new DefaultPortDescription(portNo,
                                              enabled,
                                              COPPER,
                                              portSpeed(port),
                                              annotations.build());

        }

        /**
         * Build a portDescription from a given Ethernet port description.
         *
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(OFPortDesc port) {
            if (port.getVersion().wireVersion >= OFVersion.OF_14.getWireVersion()) {
                return buildPortDescription14(port);
            }
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled =
                    !port.getState().contains(OFPortState.LINK_DOWN) &&
                            !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString()).build();
            return new DefaultPortDescription(portNo, enabled, type,
                                              portSpeed(port), annotations);
        }

        /**
         * Build a portDescription from a given a port description describing some
         * Optical port.
         *
         * @param port description property type.
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(PortDescPropertyType ptype, OFPortOptical port,
                OpenFlowOpticalSwitch opsw) {
            checkArgument(!port.getDesc().isEmpty());

            // Minimally functional fixture. This needs to be fixed as we add better support.
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());

            boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN)
                    && !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString()).build();

            if (port.getVersion() == OFVersion.OF_13
                    && ptype == PortDescPropertyType.OPTICAL_TRANSPORT) {
                // At this point, not much is carried in the optical port message.
                LOG.debug("Optical transport port message {}", port.toString());
            } else {
                // removable once 1.4+ support complete.
                LOG.debug("Unsupported optical port properties");
            }

            OFPortDescPropOpticalTransport desc = port.getDesc().get(0);
            switch (desc.getPortSignalType()) {
                // FIXME: use constants once loxi has full optical extensions
                case 2:     // OMS port
                    // Assume complete optical spectrum and 50 GHz grid
                    Set<OchSignal> signals = null;
                    if (opsw instanceof HandlerBehaviour) {
                        DriverHandler driverHandler = ((HandlerBehaviour) opsw).handler();
                        if (driverHandler != null && driverHandler.hasBehaviour(LambdaQuery.class)) {
                            try {
                                signals = driverHandler.behaviour(LambdaQuery.class).queryLambdas(portNo);
                            } catch (NullPointerException e) {
                                signals = null;
                            }
                        }
                    }
                    Frequency minFreq;
                    Frequency maxFreq;
                    Frequency channelSpacing;
                    if (signals == null || signals.isEmpty()) {
                        minFreq = Spectrum.U_BAND_MIN;
                        maxFreq = Spectrum.O_BAND_MAX;
                        channelSpacing = Frequency.ofGHz(50);
                    } else {
                        Comparator<OchSignal> compare =
                                (OchSignal a, OchSignal b) -> a.spacingMultiplier() - b.spacingMultiplier();
                        OchSignal minOch = Collections.min(signals, compare);
                        OchSignal maxOch = Collections.max(signals, compare);
                        minFreq = minOch.centralFrequency();
                        maxFreq = maxOch.centralFrequency();
                        channelSpacing = minOch.channelSpacing().frequency();
                    }
                    return omsPortDescription(portNo, enabled, minFreq,
                            maxFreq, channelSpacing, annotations);
                case 5:     // OCH port
                    OchSignal signal = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 0, 4);
                    return ochPortDescription(portNo, enabled, OduSignalType.ODU4,
                            true, signal, annotations);
                default:
                    break;
            }

            return new DefaultPortDescription(portNo, enabled, FIBER, 0, annotations);
        }

        /**
         * Build a portDescription from a given port description describing a fiber switch optical port.
         *
         * @param port description property type.
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(OFCalientPortDescStatsEntry port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());

            // Use the alias name if it's available
            String name = port.getName();
            List<OFCalientPortDescProp> props = port.getProperties();
            if (props != null && !props.isEmpty()) {
                OFCalientPortDescPropOptical propOptical = (OFCalientPortDescPropOptical) props.get(0);
                if (propOptical != null) {
                    name = propOptical.getInAlias();
                }
            }

            // FIXME when Calient OF agent reports port status
            boolean enabled = true;
            SparseAnnotations annotations = makePortAnnotation(name, port.getHwAddr().toString()).build();

            // S160 data sheet
            // Wavelength range: 1260 - 1630 nm, grid is irrelevant for this type of switch
            return omsPortDescription(portNo, enabled,
                    Spectrum.O_BAND_MIN, Spectrum.O_BAND_MAX, Frequency.ofGHz(50), annotations);
        }

        private PortDescription buildPortDescription(OFPortStatus status) {
            OFPortDesc port = status.getDesc();
            if (status.getReason() != OFPortReason.DELETE) {
                return buildPortDescription(port);
            } else {
                PortDescription desc = buildPortDescription(port);
                if (desc.isEnabled()) {
                    return DefaultPortDescription.builder(desc)
                            .isEnabled(false)
                            .build();
                }
                return desc;
            }
        }

        /**
         * Returns port speed in Mbps.
         *
         * @param port description to parse
         * @return port speed in Mbps
         */
        private long portSpeed(OFPortDesc port) {
            if (port.getVersion().getWireVersion() >= OFVersion.OF_14.getWireVersion()) {
                // OFPortDescPropEthernet
                return port.getProperties().stream()
                        .filter(OFPortDescPropEthernet.class::isInstance)
                        .map(OFPortDescPropEthernet.class::cast)
                        .mapToLong(OFPortDescPropEthernet::getCurrSpeed)
                        .map(kbps -> kbps / KBPS)
                        .findAny()
                        .orElse(PortSpeed.SPEED_NONE.getSpeedBps() / MBPS);
            }
            if (port.getVersion() == OFVersion.OF_13) {
                // Note: getCurrSpeed() returns a value in kbps (this also applies to OF_11 and OF_12)
                return port.getCurrSpeed() / KBPS;
            }
            // < OF1.3
            PortSpeed portSpeed = PortSpeed.SPEED_NONE;
            for (OFPortFeatures feat : port.getCurr()) {
                portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
            }
            return portSpeed.getSpeedBps() / MBPS;
        }

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            if (isDisabled) {
                return;
            }

            try {
                switch (msg.getType()) {
                    case STATS_REPLY:
                        if (((OFStatsReply) msg).getStatsType() == OFStatsType.PORT) {
                            OFPortStatsReply portStatsReply = (OFPortStatsReply) msg;
                            List<OFPortStatsEntry> portStatsReplyList = portStatsReplies.get(dpid);
                            if (portStatsReplyList == null) {
                                portStatsReplyList = Lists.newCopyOnWriteArrayList();
                            }
                            portStatsReplyList.addAll(portStatsReply.getEntries());
                            portStatsReplies.put(dpid, portStatsReplyList);
                            if (!portStatsReply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                                List<OFPortStatsEntry> statsEntries = portStatsReplies.get(dpid);
                                if (statsEntries != null) {
                                    pushPortMetrics(dpid, statsEntries);
                                    statsEntries.clear();
                                }
                            }
                        } else if (((OFStatsReply) msg).getStatsType() == OFStatsType.EXPERIMENTER) {
                            OpenFlowSwitch sw = controller.getSwitch(dpid);
                            if (sw == null) {
                                LOG.error("Switch {} is not found", dpid);
                                break;
                            }
                            if (sw instanceof OpenFlowOpticalSwitch) {
                                // Optical switch uses experimenter stats message to update power
                                List<PortDescription> portDescs =
                                        ((OpenFlowOpticalSwitch) sw).processExpPortStats(msg);
                                if (!portDescs.isEmpty()) {
                                    providerService.updatePorts(DeviceId.deviceId(Dpid.uri(dpid)), portDescs);
                                }
                            }
                        }
                        break;
                    case ERROR:
                        if (((OFErrorMsg) msg).getErrType() == OFErrorType.PORT_MOD_FAILED) {
                            LOG.error("port mod failed");
                        }
                    default:
                        break;
                }
            } catch (IllegalStateException e) {
                // system is shutting down and the providerService is no longer
                // valid. Messages cannot be processed.
            }
        }

        private void disable() {
            isDisabled = true;
        }
    }


}
