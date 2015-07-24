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
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
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
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortConfig;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortFeatures;
import org.projectfloodlight.openflow.protocol.OFPortOptical;
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
    private static final long MBPS = 1_000 * 1_000;

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
        LOG.info("Started");
    }

    @Deactivate
    public void deactivate(ComponentContext context) {
        cfgService.unregisterProperties(getClass(), false);
        providerRegistry.unregister(this);
        controller.removeListener(listener);
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

    @Override
    public boolean isReachable(DeviceId deviceId) {
        OpenFlowSwitch sw = controller.getSwitch(dpid(deviceId.uri()));
        if (sw == null || !sw.isConnected()) {
            return false;
        }
        return true;
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
        LOG.debug("Accepting mastership role change for device {}", deviceId);
    }

    private void pushPortMetrics(Dpid dpid, List<OFPortStatsEntry> portStatsEntries) {
        DeviceId deviceId = DeviceId.deviceId(dpid.uri(dpid));
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

            Device.Type deviceType = sw.isOptical() ? Device.Type.ROADM :
                    Device.Type.SWITCH;
            ChassisId cId = new ChassisId(dpid.value());

            SparseAnnotations annotations = DefaultAnnotations.builder()
                    .set("protocol", sw.factory().getVersion().toString())
                    .set("channelId", sw.channelId())
                    .build();

            DeviceDescription description =
                    new DefaultDeviceDescription(did.uri(), deviceType,
                                                 sw.manufacturerDescription(),
                                                 sw.hardwareDescription(),
                                                 sw.softwareDescription(),
                                                 sw.serialNumber(),
                                                 cId, annotations);
            providerService.deviceConnected(did, description);
            providerService.updatePorts(did, buildPortDescriptions(sw));

            PortStatsCollector psc =
                    new PortStatsCollector(controller.getSwitch(dpid), portStatsPollFrequency);
            psc.start();
            collectors.put(dpid, psc);
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
            if (providerService == null) {
                return;
            }
            DeviceId did = deviceId(uri(dpid));
            OpenFlowSwitch sw = controller.getSwitch(dpid);
            providerService.updatePorts(did, buildPortDescriptions(sw));
        }

        @Override
        public void portChanged(Dpid dpid, OFPortStatus status) {
            PortDescription portDescription = buildPortDescription(status);
            providerService.portStatusChanged(deviceId(uri(dpid)), portDescription);
        }

        @Override
        public void receivedRoleReply(Dpid dpid, RoleState requested, RoleState response) {
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
            sw.getPorts().forEach(port -> portDescs.add(buildPortDescription(port)));
            if (sw.isOptical()) {
                OpenFlowOpticalSwitch opsw = (OpenFlowOpticalSwitch) sw;
                opsw.getPortTypes().forEach(type -> {
                    opsw.getPortsOf(type).forEach(
                        op -> {
                            portDescs.add(buildPortDescription(type, (OFPortOptical) op));
                        }
                    );
                });
            }
            return portDescs;
        }

        /**
         * Creates an annotation for the port name if one is available.
         *
         * @param port description of the port
         * @return annotation containing the port name if one is found,
         *         null otherwise
         */
        private SparseAnnotations makePortNameAnnotation(String port) {
            SparseAnnotations annotations = null;
            String portName = Strings.emptyToNull(port);
            if (portName != null) {
                annotations = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, portName).build();
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
            SparseAnnotations annotations = makePortNameAnnotation(port.getName());
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
            // Minimally functional fixture. This needs to be fixed as we add better support.
            PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());

            boolean enabled = !port.getState().contains(OFPortState.LINK_DOWN)
                    && !port.getConfig().contains(OFPortConfig.PORT_DOWN);
            SparseAnnotations annotations = makePortNameAnnotation(port.getName());

            if (port.getVersion() == OFVersion.OF_13
                    && ptype == PortDescPropertyType.OPTICAL_TRANSPORT) {
                // At this point, not much is carried in the optical port message.
                LOG.debug("Optical transport port message {}", port.toString());
            } else {
                // removable once 1.4+ support complete.
                LOG.debug("Unsupported optical port properties");
            }
            return new DefaultPortDescription(portNo, enabled, FIBER, 0, annotations);
        }

        private PortDescription buildPortDescription(OFPortStatus status) {
            OFPortDesc port = status.getDesc();
            if (status.getReason() != OFPortReason.DELETE) {
                return buildPortDescription(port);
            } else {
                PortNumber portNo = PortNumber.portNumber(port.getPortNo().getPortNumber());
                Port.Type type = port.getCurr().contains(OFPortFeatures.PF_FIBER) ? FIBER : COPPER;
                SparseAnnotations annotations = makePortNameAnnotation(port.getName());
                return new DefaultPortDescription(portNo, false, type,
                                                  portSpeed(port), annotations);
            }
        }

        private long portSpeed(OFPortDesc port) {
            if (port.getVersion() == OFVersion.OF_13) {
                return port.getCurrSpeed() / MBPS;
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
