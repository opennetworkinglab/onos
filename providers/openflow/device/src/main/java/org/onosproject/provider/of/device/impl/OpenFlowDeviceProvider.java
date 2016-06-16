/*
 * Copyright 2014-2015 Open Networking Laboratory
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
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.GridType;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.OtuSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceProvider;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.OchPortDescription;
import org.onosproject.net.device.OduCltPortDescription;
import org.onosproject.net.device.OmsPortDescription;
import org.onosproject.net.device.OtuPortDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
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
import org.projectfloodlight.openflow.protocol.OFExpPort;
import org.projectfloodlight.openflow.protocol.OFExpPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFExpPortOpticalTransportLayerEntry;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFObject;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescPropOpticalTransport;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
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
import org.projectfloodlight.openflow.types.PortSpeed;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.Port.Type.FIBER;
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

    //TODO consider renaming KBPS and MBPS (as they are used to convert by division)
    private static final long KBPS = 1_000;
    private static final long MBPS = 1_000 * 1_000;
    private static final Frequency FREQ50 = Frequency.ofGHz(50);
    private static final Frequency FREQ191_7 = Frequency.ofGHz(191_700);
    private static final Frequency FREQ4_4 = Frequency.ofGHz(4_400);

    private static final long OFP_PORT_MOD_PORT_DOWN = 1 << 0;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry providerRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenFlowController controller;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private DeviceProviderService providerService;

    private final InternalDeviceProvider listener = new InternalDeviceProvider();

    // TODO: We need to make the poll interval configurable.
    static final int POLL_INTERVAL = 5;
    @Property(name = "PortStatsPollFrequency", intValue = POLL_INTERVAL,
    label = "Frequency (in seconds) for polling switch Port statistics")
    private int portStatsPollFrequency = POLL_INTERVAL;

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

        connectInitialDevices();
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        controller.removeListener(listener);
        providerRegistry.unregister(this);
        collectors.values().forEach(PortStatsCollector::stop);
        providerService = null;
        LOG.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newPortStatsPollFrequency;
        try {
            String s = get(properties, "PortStatsPollFrequency");
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
            PortStatsCollector psc = new PortStatsCollector(sw, portStatsPollFrequency);
            psc.start();
            collectors.put(new Dpid(sw.getId()), psc);
        }
    }

    @Override
    public boolean isReachable(DeviceId deviceId) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        return sw != null && sw.isConnected();
    }

    @Override
    public void enablePort(DeviceId deviceId, PortNumber portNumber) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        if (sw == null) {
            return;
        }
        OFPortDesc port = getPortForNumber(sw, portNumber);
        if (port == null) {
            return;
        }

        OFFactory fact = sw.factory();
        sw.sendMsg(fact.buildPortMod().setPortNo(port.getPortNo())
                .setHwAddr(port.getHwAddr())
                .setConfig(0).setMask(OFP_PORT_MOD_PORT_DOWN).build());
    }

    /* TODO migrate this to OpenFlowSwitch? */
    private OFPortDesc getPortForNumber(OpenFlowSwitch sw, PortNumber portNumber) {
        List<OFPortDesc> ports = sw.getPorts();
        for (OFPortDesc port : ports) {
            if (port.getPortNo().getPortNumber() == portNumber.toLong()) {
                return port;
            }
        }
        return null;
    }

    @Override
    public void disablePort(DeviceId deviceId, PortNumber portNumber) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        if (sw == null) {
            return;
        }
        OFPortDesc port = getPortForNumber(sw, portNumber);
        if (port == null) {
            return;
        }

        OFFactory fact = sw.factory();
        sw.sendMsg(fact.buildPortMod().setPortNo(port.getPortNo())
                .setHwAddr(port.getHwAddr())
                .setConfig(OFP_PORT_MOD_PORT_DOWN).setMask(OFP_PORT_MOD_PORT_DOWN).build());
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

    private void pushPortMetrics(Dpid dpid, List<OFPortStatsEntry> portStatsEntries) {
        DeviceId deviceId = DeviceId.deviceId(Dpid.uri(dpid));
        Collection<PortStatistics> stats = buildPortStatistics(deviceId, portStatsEntries);
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

        @Override
        public void switchAdded(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            if (sw == null) {
                return;
            }

            ChassisId cId = new ChassisId(dpid.value());

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set(AnnotationKeys.PROTOCOL, sw.factory().getVersion().toString())
                    .set(AnnotationKeys.CHANNEL_ID, sw.channelId())
                    .set(AnnotationKeys.MANAGEMENT_ADDRESS, sw.channelId().split(":")[0])
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), sw.deviceType(),
                                                 sw.manufacturerDescription(),
                                                 sw.hardwareDescription(),
                                                 sw.softwareDescription(),
                                                 sw.serialNumber(),
                                                 cId, annotations);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(sw));

            PortStatsCollector psc =
                    new PortStatsCollector(sw, portStatsPollFrequency);
            psc.start();
            collectors.put(dpid, psc);

            //figure out race condition for collectors.remove() and collectors.put()
            if (controller.getSwitch(dpid) == null) {
                switchRemoved(dpid);
            }
        }

        @Override
        public void switchRemoved(Dpid dpid) {
            if (providerService == null) {
                return;
            }
            providerService.deviceDisconnected(deviceId(uri(dpid)));

            PortStatsCollector collector = collectors.remove(dpid);
            if (collector != null) {
                collector.stop();
            }
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
            providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
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
                    (Device.Type.OTN.equals(sw.deviceType())))) {
                  sw.getPorts().forEach(port -> portDescs.add(buildPortDescription(port)));
            }

            OpenFlowOpticalSwitch opsw;
            switch (sw.deviceType()) {
                case ROADM:
                case OTN:
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
                            portDescs.add(buildPortDescription(type, op));
                        }
                     );
                    });
                    break;
                case FIBER_SWITCH:
                    opsw = (OpenFlowOpticalSwitch) sw;
                    opsw.getPortTypes().forEach(type -> {
                        opsw.getPortsOf(type).forEach(
                                op -> {
                                    portDescs.add(buildPortDescription((OFCalientPortDescStatsEntry) op));
                                }
                        );
                    });
                    break;
                default:
                    break;
            }

            return portDescs;
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
            return new OduCltPortDescription(portNo, enabled, sigType, annotations);
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

        private PortDescription buildPortDescription(PortDescPropertyType ptype, OFObject port) {
            if (port instanceof  OFPortOptical) {
               return buildPortDescription(ptype, (OFPortOptical) port);
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
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());

            OFExpPortDescPropOpticalTransport firstProp = port.getProperties().get(0);
            OFPortOpticalTransportSignalType sigType = firstProp.getPortSignalType();

            DefaultPortDescription portDes = null;
            switch (sigType) {
            case OMSN:
                portDes =  new OmsPortDescription(portNo, enabled,
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

                portDes = new OchPortDescription(portNo, enabled,
                        oduSignalType, true, signalId, annotations);

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
                portDes = new OtuPortDescription(portNo, enabled, otuSignalType, annotations);
                break;
            default:
                break;
            }

            return portDes;
        }

        /**
         * Creates an annotation for the port name if one is available.
         *
         * @param portName the port name
         * @param portMac the port mac
         * @return annotation containing the port name if one is found,
         *         null otherwise
         */
        private SparseAnnotations makePortAnnotation(String portName, String portMac) {
            SparseAnnotations annotations = null;
            String pName = Strings.emptyToNull(portName);
            String pMac = Strings.emptyToNull(portMac);
            if (portName != null) {
                annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, pName)
                        .set(AnnotationKeys.PORT_MAC, pMac).build();
            }
            return annotations;
        }

        /**
         * Build a portDescription from a given Ethernet port description.
         *
         * @param port the port to build from.
         * @return portDescription for the port.
         */
        private PortDescription buildPortDescription(OFPortDesc port) {
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
            boolean enabled =
                    !port.getState().contains(OFPortState.LINK_DOWN) &&
                            !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());
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
        private PortDescription buildPortDescription(PortDescPropertyType ptype, OFPortOptical port) {
            checkArgument(port.getDesc().size() >= 1);

            // Minimally functional fixture. This needs to be fixed as we add better support.
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());

            boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN)
                    && !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());

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
                    // LINC-OE is only supported optical OF device for now
                    return new OmsPortDescription(portNo, enabled,
                            Spectrum.U_BAND_MIN, Spectrum.O_BAND_MAX, Frequency.ofGHz(50), annotations);
                case 5:     // OCH port
                    OchSignal signal = new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ, 0, 4);
                    return new OchPortDescription(portNo, enabled, OduSignalType.ODU4,
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
            if (props != null && props.size() > 0) {
                OFCalientPortDescPropOptical propOptical = (OFCalientPortDescPropOptical) props.get(0);
                if (propOptical != null) {
                    name = propOptical.getInAlias();
                }
            }

            // FIXME when Calient OF agent reports port status
            boolean enabled = true;
            SparseAnnotations annotations = makePortAnnotation(name, port.getHwAddr().toString());

            // S160 data sheet
            // Wavelength range: 1260 - 1630 nm, grid is irrelevant for this type of switch
            return new OmsPortDescription(portNo, enabled,
                    Spectrum.U_BAND_MIN, Spectrum.O_BAND_MAX, Frequency.ofGHz(100), annotations);
        }

        private PortDescription buildPortDescription(OFPortStatus status) {
            OFPortDesc port = status.getDesc();
            if (status.getReason() != OFPortReason.DELETE) {
                return buildPortDescription(port);
            } else {
                PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
                Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
                SparseAnnotations annotations = makePortAnnotation(port.getName(), port.getHwAddr().toString());
                return new DefaultPortDescription(portNo, false, type,
                                                  portSpeed(port), annotations);
            }
        }

        private long portSpeed(OFPortDesc port) {
            if (port.getVersion() == OFVersion.OF_13) {
                // Note: getCurrSpeed() returns a value in kbps (this also applies to OF_11 and OF_12)
                return port.getCurrSpeed() / KBPS;
            }

            PortSpeed portSpeed = PortSpeed.SPEED_NONE;
            for (OFPortFeatures feat : port.getCurr()) {
                portSpeed = PortSpeed.max(portSpeed, feat.getPortSpeed());
            }
            return portSpeed.getSpeedBps() / MBPS;
        }

        @Override
        public void handleMessage(Dpid dpid, OFMessage msg) {
            switch (msg.getType()) {
                case STATS_REPLY:
                    if (((OFStatsReply) msg).getStatsType() == OFStatsType.PORT) {
                        OFPortStatsReply portStatsReply = (OFPortStatsReply) msg;
                        List<OFPortStatsEntry> portStatsReplyList = portStatsReplies.get(dpid);
                        if (portStatsReplyList == null) {
                            portStatsReplyList = Lists.newArrayList();
                        }
                        portStatsReplyList.addAll(portStatsReply.getEntries());
                        portStatsReplies.put(dpid, portStatsReplyList);
                        if (!portStatsReply.getFlags().contains(OFStatsReplyFlags.REPLY_MORE)) {
                            pushPortMetrics(dpid, portStatsReplies.get(dpid));
                            portStatsReplies.get(dpid).clear();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
