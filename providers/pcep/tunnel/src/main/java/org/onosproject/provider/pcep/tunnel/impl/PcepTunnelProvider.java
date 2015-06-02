/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.provider.pcep.tunnel.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.DefaultGroupId;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelProviderRegistry;
import org.onosproject.incubator.net.tunnel.TunnelProviderService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.api.PcepController;
import org.onosproject.pcep.api.PcepDpid;
import org.onosproject.pcep.api.PcepHopNodeDescription;
import org.onosproject.pcep.api.PcepOperator.OperationType;
import org.onosproject.pcep.api.PcepTunnel;
import org.onosproject.pcep.api.PcepTunnel.PathState;
import org.onosproject.pcep.api.PcepTunnel.PATHTYPE;
import org.onosproject.pcep.api.PcepTunnelListener;
import org.slf4j.Logger;

import static org.onosproject.pcep.api.PcepDpid.*;

/**
 * Provider which uses an PCEP controller to detect, update, create network
 * tunnels.
 */
@Component(immediate = true)
@Service
public class PcepTunnelProvider extends AbstractProvider
        implements TunnelProvider {

    private static final Logger log = getLogger(PcepTunnelProvider.class);
    private static final long MAX_BANDWIDTH = 99999744;
    private static final long MIN_BANDWIDTH = 64;
    private static final String BANDWIDTH_UINT = "kbps";
    static final String PROVIDER_ID = "org.onosproject.provider.tunnel.default";

    private static final String TUNNLE_NOT_NULL = "Create failed,The given port may be wrong or has been occupied.";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelProviderRegistry tunnelProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PcepController controller;

    TunnelProviderService service;

    HashMap<String, TunnelId> tunnelMap = new HashMap<String, TunnelId>();

    private InnerTunnerProvider listener = new InnerTunnerProvider();

    /**
     * Creates a Tunnel provider.
     */
    public PcepTunnelProvider() {
        super(new ProviderId("default", PROVIDER_ID));
    }

    @Activate
    public void activate() {
        service = tunnelProviderRegistry.register(this);
        controller.addTunnelListener(listener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        tunnelProviderRegistry.unregister(this);
        controller.removeTunnelListener(listener);
        log.info("Stopped");
    }

    @Override
    public void setupTunnel(Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setupTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void releaseTunnel(Tunnel tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void releaseTunnel(ElementId srcElement, Tunnel tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTunnel(Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateTunnel(ElementId srcElement, Tunnel tunnel, Path path) {
        // TODO Auto-generated method stub

    }

    @Override
    public TunnelId tunnelAdded(TunnelDescription tunnel) {

        long bandwidth = Long
                .parseLong(tunnel.annotations().value("bandwidth"));

        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            error("Update failed, invalid bandwidth.");
            return null;
        }

        // endpoints
        OpticalTunnelEndPoint src = (org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint) tunnel
                .src();
        OpticalTunnelEndPoint dst = (OpticalTunnelEndPoint) tunnel.dst();
        // devices
        DeviceId srcId = (DeviceId) src.elementId().get();
        DeviceId dstId = (DeviceId) dst.elementId().get();

        // ports
        long srcPort = src.portNumber().get().toLong();
        long dstPort = dst.portNumber().get().toLong();

        // type
        if (tunnel.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel creation.");
            return null;
        }

        PcepTunnel pcepTunnel = controller.applyTunnel(srcId, dstId, srcPort,
                                                       dstPort, bandwidth,
                                                       tunnel.tunnelName()
                                                               .value());

        checkNotNull(pcepTunnel, TUNNLE_NOT_NULL);
        TunnelDescription tunnelAdded = buildOpticalTunnel(pcepTunnel, null);
        TunnelId tunnelId = service.tunnelAdded(tunnelAdded);

        tunnelMap.put(String.valueOf(pcepTunnel.id()), tunnelId);
        return tunnelId;
    }

    @Override
    public void tunnelRemoved(TunnelDescription tunnel) {
        Tunnel tunnelOld = tunnelQueryById(tunnel.id());
        checkNotNull(tunnelOld, "The tunnel id is not exsited.");
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel deletion.");
            return;
        }
        String pcepTunnelId = getPCEPTunnelKey(tunnel.id());
        checkNotNull(pcepTunnelId, "The tunnel id is not exsited.");
        if (!controller.deleteTunnel(pcepTunnelId)) {
            error("Delete tunnel failed, Maybe some devices have been disconnected.");
            return;
        }
        tunnelMap.remove(pcepTunnelId);
        service.tunnelRemoved(tunnel);
    }

    @Override
    public void tunnelUpdated(TunnelDescription tunnel) {

        Tunnel tunnelOld = tunnelQueryById(tunnel.id());
        if (tunnelOld.type() != Tunnel.Type.VLAN) {
            error("Illegal tunnel type. Only support VLAN tunnel update.");
            return;
        }
        long bandwidth = Long
                .parseLong(tunnel.annotations().value("bandwidth"));
        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            error("Update failed, invalid bandwidth.");
            return;
        }
        String pcepTunnelId = getPCEPTunnelKey(tunnel.id());

        checkNotNull(pcepTunnelId, "Invalid tunnel id");
        if (!controller.updateTunnelBandwidth(pcepTunnelId, bandwidth)) {

            error("Update failed,maybe invalid bandwidth.");
            return;

        }
        service.tunnelUpdated(tunnel);
    }

    private void error(String info) {
        System.err.println(info);
    }

    // Short-hand for creating a connection point.
    private ConnectPoint connectPoint(PcepDpid id, long port) {
        return new ConnectPoint(deviceId(uri(id)), portNumber(port));
    }

    // Short-hand for creating a link.
    private Link link(PcepDpid src, long sp, PcepDpid dst, long dp) {
        return new DefaultLink(id(), connectPoint(src, sp), connectPoint(dst,
                                                                         dp),
                               Link.Type.TUNNEL);
    }

    // Creates a path that leads through the given devices.
    private Path createPath(List<PcepHopNodeDescription> hopList,
                            PATHTYPE pathtype, PathState pathState) {
        if (hopList == null || hopList.size() == 0) {
            return null;
        }
        List<Link> links = new ArrayList<>();
        for (int i = 1; i < hopList.size() - 1; i = i + 2) {
            links.add(link(hopList.get(i).getDeviceId(), hopList.get(i)
                    .getPortNum(), hopList.get(i + 1).getDeviceId(), hopList
                    .get(i + 1).getPortNum()));
        }

        int hopNum = hopList.size() - 2;
        DefaultAnnotations extendAnnotations = DefaultAnnotations.builder()
                .set("pathNum", String.valueOf(hopNum))
                .set("pathState", String.valueOf(pathState))
                .set("pathType", String.valueOf(pathtype)).build();
        return new DefaultPath(id(), links, hopNum, extendAnnotations);
    }

    // convert the path description to a string.
    public String pathToString(List<Link> links) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        for (Link link : links) {
            builder.append("(Device:" + link.src().deviceId() + "  Port:"
                    + link.src().port().toLong());
            builder.append(" Device:" + link.dst().deviceId() + "  Port:"
                    + link.dst().port().toLong());
            builder.append(")");
        }
        builder.append("}");
        return builder.toString();
    }

    // build a TunnelDescription.
    private TunnelDescription buildOpticalTunnel(PcepTunnel pcepTunnel,
                                                 TunnelId tunnelId) {
        TunnelEndPoint srcPoint = null;
        TunnelEndPoint dstPoint = null;
        Tunnel.Type tunnelType = null;
        TunnelName name = TunnelName.tunnelName(pcepTunnel.name());

        // add path after codes of tunnel's path merged
        Path path = createPath(pcepTunnel.getHopList(),
                               pcepTunnel.getPathType(),
                               pcepTunnel.getPathState());

        OpticalTunnelEndPoint.Type endPointType = null;
        switch (pcepTunnel.type()) {
        case OCH:
            tunnelType = Tunnel.Type.OCH;
            endPointType = OpticalTunnelEndPoint.Type.LAMBDA;
            break;

        case OTN:
            tunnelType = Tunnel.Type.ODUK;
            endPointType = OpticalTunnelEndPoint.Type.TIMESLOT;
            break;

        case UNI:
            tunnelType = Tunnel.Type.VLAN;
            endPointType = null;
            break;

        default:
            break;
        }
        DeviceId srcDid = deviceId(uri(pcepTunnel.srcDeviceID()));
        DeviceId dstDid = deviceId(uri(pcepTunnel.dstDeviceId()));
        PortNumber srcPort = PortNumber.portNumber(pcepTunnel.srcPort());
        PortNumber dstPort = PortNumber.portNumber(pcepTunnel.dstPort());

        srcPoint = new DefaultOpticalTunnelEndPoint(id(), Optional.of(srcDid),
                                                    Optional.of(srcPort), null,
                                                    endPointType,
                                                    OpticalLogicId.logicId(0),
                                                    true);
        dstPoint = new DefaultOpticalTunnelEndPoint(id(), Optional.of(dstDid),
                                                    Optional.of(dstPort), null,
                                                    endPointType,
                                                    OpticalLogicId.logicId(0),
                                                    true);

        // basic annotations
        DefaultAnnotations annotations = DefaultAnnotations
                .builder()
                .set("SLA", String.valueOf(pcepTunnel.getSla()))
                .set("bandwidth",
                     String.valueOf(pcepTunnel.bandWidth()) + BANDWIDTH_UINT)
                .set("index", String.valueOf(pcepTunnel.id())).build();

        // a VLAN tunnel always carry OCH tunnel, this annotation is the index
        // of a OCH tunnel.
        if (pcepTunnel.underlayTunnelId() != 0) {
            DefaultAnnotations extendAnnotations = DefaultAnnotations
                    .builder()
                    .set("underLayTunnelIndex",
                         String.valueOf(pcepTunnel.underlayTunnelId())).build();
            annotations = DefaultAnnotations.merge(annotations,
                                                   extendAnnotations);

        }
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                tunnelId,
                                                                srcPoint,
                                                                dstPoint,
                                                                tunnelType,
                                                                new DefaultGroupId(
                                                                                   0),
                                                                id(), name,
                                                                path,
                                                                annotations);
        return tunnel;

    }

    /**
     * Get the tunnelID according to the tunnel key.
     *
     * @param tunnelKey tunnel key
     * @return corresponding tunnel id of the a tunnel key.
     */
    private TunnelId getTunnelId(String tunnelKey) {

        for (String key : tunnelMap.keySet()) {
            if (key.equals(tunnelKey)) {
                return tunnelMap.get(key);
            }
        }
        return null;
    }

    /**
     * Get the tunnel key according to the tunnelID.
     *
     * @param tunnelId tunnel id
     * @return corresponding a tunnel key of the tunnel id.
     */
    private String getPCEPTunnelKey(TunnelId tunnelId) {
        for (String key : tunnelMap.keySet()) {
            if (tunnelMap.get(key).id() == tunnelId.id()) {
                return key;
            }
        }
        return null;

    }

    private class InnerTunnerProvider implements PcepTunnelListener {

        @Override
        public void handlePCEPTunnel(PcepTunnel pcepTunnel) {

            TunnelDescription tunnel = null;
            // instance and id identify a tunnel together
            String tunnelKey = String.valueOf(pcepTunnel.getInstance())
                    + String.valueOf(pcepTunnel.id());

            if (tunnelKey == null || "".equals(tunnelKey)) {
                log.error("Invalid PCEP tunnel");
                return;
            }

            TunnelId tunnelId = getTunnelId(tunnelKey);

            tunnel = buildOpticalTunnel(pcepTunnel, tunnelId);

            OperationType operType = pcepTunnel.getOperationType();
            switch (operType) {
            case ADD:
                tunnelId = service.tunnelAdded(tunnel);
                tunnelMap.put(tunnelKey, tunnelId);
                break;

            case UPDATE:
                service.tunnelUpdated(tunnel);
                break;

            case DELETE:
                service.tunnelRemoved(tunnel);
                tunnelMap.remove(tunnelKey);
                break;

            default:
                log.error("Invalid tunnel operation");

            }

        }
    }

    @Override
    public Tunnel tunnelQueryById(TunnelId tunnelId) {
        return service.tunnelQueryById(tunnelId);
    }

}
