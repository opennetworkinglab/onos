/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.actn.mdsc.tetunnelctl;

import com.google.common.collect.Lists;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.actn.mdsc.pce.TeTunnelPceService;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelAdminService;
import org.onosproject.incubator.net.tunnel.TunnelEvent;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.TeTunnelAdminService;
import org.onosproject.tetunnel.api.TeTunnelService;
import org.onosproject.tetunnel.api.tunnel.DefaultTeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.onosproject.tetunnel.api.tunnel.path.TePath;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteUnnumberedLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * TE Tunnel controller/processor which manages TE tunnel processing.
 * <p>
 * For example, when creating a cross-domain tunnel from a MDSC, the
 * processor will call a relevant PCE to get an end-to-end cross-domain path,
 * then spits the path into segment tunnels(domain tunnels), and then informs
 * PNCs to setup domain tunnels respectively.
 */
@Component(immediate = true)
public class TeTunnelCtl {

    private static final Logger log = LoggerFactory.getLogger(TeTunnelCtl.class);

    private final TunnelListener tunnelListener = new InternalTunnelListener();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelAdminService tunnelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelService teTunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelAdminService teTunnelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService teTopologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelPceService teTunnelPceService;

    @Activate
    protected void activate() {
        tunnelService.addListener(tunnelListener);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        tunnelService.removeListener(tunnelListener);

        log.info("Stopped");
    }

    private void addTeTunnel(TeTunnel teTunnel) {
        if (teTunnel == null) {
            return;
        }

        Tunnel tunnel = tunnelService.queryTunnel(
                teTunnelService.getTunnelId(teTunnel.teTunnelKey()));
        if (tunnel == null) {
            log.error("tunnel does not exist, {}", teTunnel.teTunnelKey());
            return;
        }
        if (tunnel.state() != Tunnel.State.INIT) {
            log.error("tunnel state error, {}, {}", teTunnel.teTunnelKey(),
                      tunnel.state());
            return;
        }
        tunnelAdminService.updateTunnelState(tunnel, Tunnel.State.ESTABLISHING);

        //TODO support multi-thread
        if (isTeTunnelCrossDomain(teTunnel)) {
            if (!addCrossDomainTeTunnel(teTunnel)) {
                tunnelAdminService.updateTunnelState(tunnel, Tunnel.State.FAILED);
            }
        }
        /*
         * "else" is to do nothing.
         * When adding a single domain tunnel, the TunnelManager will call
         * tunnel providers, then the providers will pass the request to
         * the domain controller. Nothing to do here.
         */
    }

    private boolean isTeTunnelCrossDomain(TeTunnel teTunnel) {
        TeTopology srcTopo = teTopologyService.teTopology(
                teTopologyService.teNode(teTunnel.srcNode())
                .underlayTeTopologyId());
        TeTopology dstTopo = teTopologyService.teTopology(
                teTopologyService.teNode(teTunnel.dstNode())
                .underlayTeTopologyId());
        return (srcTopo != null && dstTopo != null
                && srcTopo.ownerId().equals(dstTopo.ownerId()));
    }

    private boolean addCrossDomainTeTunnel(TeTunnel teTunnel) {
        List<TeRouteSubobject> route = null;
        TePath primaryPath = teTunnel.primaryPaths().get(0);
        if (primaryPath != null &&
                primaryPath.type() == TePath.Type.EXPLICIT) {
            route = primaryPath.explicitRoute();
        } else {
            Collection<List<TeRouteSubobject>> routes =
                    teTunnelPceService.computePaths(teTunnel);
            if (routes == null || routes.isEmpty()) {
                log.error("no available route for {}",
                          teTunnel.teTunnelKey());
                return false;
            }

            //FIXME: try other pce when failed?
            route = routes.iterator().next();
        }

        if (route == null) {
            log.error("no available route for {}",
                      teTunnel.teTunnelKey());
            return false;
        }

        return spitRoute(teTunnel, route);
    }

    //spits route to segment tunnels
    private boolean spitRoute(TeTunnel teTunnel, List<TeRouteSubobject> route) {
        List<TeTunnelKey> segmentTunnels = Lists.newArrayList();
        boolean success = true;
        TeNodeKey srcNode = teTunnel.srcNode();
        TtpKey srcTp = teTunnel.srcTp();
        TeNodeKey dstNode = null;
        TtpKey dstTp = null;

        for (TeRouteSubobject teRouteSubobject : route) {
            if (!(teRouteSubobject instanceof TeRouteUnnumberedLink)) {
                log.error("unsupported type {}", teRouteSubobject.type());
                success = false;
                break;
            }

            TeRouteUnnumberedLink teRouteUnnumberedLink =
                    (TeRouteUnnumberedLink) teRouteSubobject;
            dstNode = teRouteUnnumberedLink.node();
            dstTp = teRouteUnnumberedLink.ttp();
            if (Objects.equals(srcNode, dstNode) &&
                    Objects.equals(srcTp, dstTp)) {
                continue;
            }
            if (Objects.equals(srcNode, dstNode)) {
                if (!addSegmentTunnel(segmentTunnels, teTunnel,
                                      srcNode, srcTp, dstNode, dstTp)) {
                    success = false;
                    break;
                }
            }

            srcNode = dstNode;
            srcTp = dstTp;
        }

        if (success && !(Objects.equals(dstNode, teTunnel.dstNode()) &&
                Objects.equals(dstTp, teTunnel.dstTp()))) {
            srcNode = dstNode;
            srcTp = dstTp;
            dstNode = teTunnel.dstNode();
            dstTp = teTunnel.dstTp();
            if (!addSegmentTunnel(segmentTunnels, teTunnel,
                                  srcNode, srcTp, dstNode, dstTp)) {
                success = false;
            }
        }

        if (!success) {
            // roll back segment tunnels
            for (TeTunnelKey key : segmentTunnels) {
                teTunnelAdminService.removeTeTunnel(key);
            }
        } else {
            teTunnelAdminService.setSegmentTunnel(teTunnel.teTunnelKey(),
                                                  segmentTunnels);
        }
        return success;
    }

    private boolean addSegmentTunnel(List<TeTunnelKey> segmentTunnels,
                                     TeTunnel teTunnel,
                                     TeNodeKey srcNode, TtpKey srcTp,
                                     TeNodeKey dstNode, TtpKey dstTp) {
        TeTunnelKey teTunnelKey = getNextTeTunnelKey(srcNode.teTopologyKey());
        TeTunnel teTunnelSegment = DefaultTeTunnel.builder()
                .teTunnelKey(teTunnelKey)
                .srcNode(srcNode)
                .dstNode(dstNode)
                .srcTp(srcTp)
                .dstTp(dstTp)
                .adminState(teTunnel.adminStatus())
                .lspProtectionType(teTunnel.lspProtectionType())
                .type(teTunnel.type())
                .build();
        TunnelId tunnelId =
                teTunnelAdminService.createTeTunnel(teTunnelSegment);
        if (tunnelId == null) {
            log.error("failed to create segment tunnel: {},{},{},{}",
                      srcNode, srcTp, dstNode, dstTp);
            return false;
        }
        segmentTunnels.add(teTunnelKey);
        return true;
    }

    private TeTunnelKey getNextTeTunnelKey(TeTopologyKey key) {
        //FIXME need a better way to get a te tunnel id
        long teTunnelId = teTunnelService.getTeTunnels(key).size() + 1;
        return new TeTunnelKey(key, teTunnelId);
    }

    private void updateTeTunnel(TeTunnel teTunnel, Tunnel tunnel) {
        if (teTunnel == null) {
            return;
        }

        if (tunnel.state() == Tunnel.State.ESTABLISHED) {
            tunnelEstablished(teTunnel);
        } else if (tunnel.state() == Tunnel.State.REMOVING) {
            removingTunnel(teTunnel);
        }

        //TODO update TE tunnel content
    }

    private void tunnelEstablished(TeTunnel teTunnel) {
        TeTunnel e2eTeTunnel = retriveE2eTunnel(teTunnel);
        if (e2eTeTunnel != null) {
            boolean goodToContinue = true;
            for (TeTunnelKey key : e2eTeTunnel.segmentTunnels()) {
                goodToContinue = checkSegmentTunnel(key);
                if (!goodToContinue) {
                    break;
                }
            }

            if (goodToContinue) {
                tunnelAdminService.updateTunnelState(
                        tunnelService.queryTunnel(
                                teTunnelService.getTunnelId(
                                        teTunnel.teTunnelKey())),
                        Tunnel.State.ESTABLISHED
                );
            }
        }
    }

    private TeTunnel retriveE2eTunnel(TeTunnel segmentTunnel) {
        return teTunnelService.getTeTunnel(segmentTunnel.e2eTunnelKey());
    }

    private boolean checkSegmentTunnel(TeTunnelKey key) {
        Tunnel segmentTunnel = tunnelService.queryTunnel(
                teTunnelService.getTunnelId(key));
        if (segmentTunnel == null ||
                segmentTunnel.state() != Tunnel.State.ESTABLISHED) {
            return false;
        }
        return true;
    }

    private void removingTunnel(TeTunnel teTunnel) {
        List<TeTunnelKey> segmentTunnels = teTunnel.segmentTunnels();
        if (segmentTunnels != null && !segmentTunnels.isEmpty()) {
            for (TeTunnelKey key : segmentTunnels) {
                teTunnelAdminService.removeTeTunnel(key);
            }
        }
    }

    // Listens on tunnel events.
    private class InternalTunnelListener implements TunnelListener {
        @Override
        public void event(TunnelEvent event) {
            switch (event.type()) {
                case TUNNEL_ADDED:
                    addTunnel(event.subject());
                    break;
                case TUNNEL_UPDATED:
                    updateTunnel(event.subject());
                    break;
                //TODO: TE Tunnel remove/... event process
                default:
                    log.warn("unknown event: {}", event.type());
                    break;
            }
        }

        private void addTunnel(Tunnel tunnel) {
            addTeTunnel(teTunnelService.getTeTunnel(tunnel.tunnelId()));
        }

        private void updateTunnel(Tunnel tunnel) {
            updateTeTunnel(teTunnelService.getTeTunnel(tunnel.tunnelId()),
                           tunnel);
        }
    }
}
