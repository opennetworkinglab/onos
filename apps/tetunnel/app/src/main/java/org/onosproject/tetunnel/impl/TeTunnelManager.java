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

package org.onosproject.tetunnel.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelAdminService;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetunnel.api.lsp.TeLsp;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.TeTunnelAdminService;
import org.onosproject.tetunnel.api.TeTunnelProviderService;
import org.onosproject.tetunnel.api.TeTunnelService;
import org.onosproject.tetunnel.api.TeTunnelStore;
import org.onosproject.tetunnel.api.tunnel.TeTunnelEndpoint;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of TE tunnel attributes management service.
 */
@Component(immediate = true)
@Service
public class TeTunnelManager implements TeTunnelService, TeTunnelAdminService,
        TeTunnelProviderService {

    private static final String TE_TUNNEL_APP = "onos-app-tetunnel";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelStore store;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TunnelAdminService tunnelAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService teTopologyService;

    private ApplicationId appId;

    @Activate
    public void activate() {
        appId = coreService.registerApplication(TE_TUNNEL_APP);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public TunnelId createTeTunnel(TeTunnel teTunnel) {
        if (!store.addTeTunnel(teTunnel)) {
            log.error("can not add teTunnel: {}", teTunnel);
            return null;
        }

        TunnelId tunnelId = TunnelId.valueOf(teTunnel.teTunnelKey().toString());
        Tunnel tunnel = new DefaultTunnel(ProviderId.NONE,
                                          new TeTunnelEndpoint(teTunnel.srcNode(),
                                                               teTunnel.srcTp()),
                                          new TeTunnelEndpoint(teTunnel.dstNode(),
                                                               teTunnel.dstTp()),
                                          Tunnel.Type.MPLS, new GroupId(0),
                                          tunnelId,
                                          TunnelName.tunnelName(teTunnel.name()),
                                          null,
                                          DefaultAnnotations.builder().build());
        store.setTunnelId(teTunnel.teTunnelKey(), tunnelId);
        TeTopology srcTopology = teTopologyService.teTopology(
                teTopologyService.teNode(teTunnel.srcNode())
                .underlayTeTopologyId());
        if (srcTopology == null) {
            srcTopology = teTopologyService.teTopology(teTunnel.srcNode()
                                                               .teTopologyKey());
        }
        DeviceId domainId = srcTopology.ownerId();
        TunnelId id = tunnelService.setupTunnel(appId, domainId, tunnel, null);
        if (id == null) {
            log.error("can not create tunnel for te {}",
                      teTunnel.teTunnelKey());
            store.removeTeTunnel(teTunnel.teTunnelKey());
            return null;
        }
        if (!id.equals(tunnelId)) {
            //this should not happen
            log.error("tunnelId changed, oldId:{}, newId:{}", tunnelId, id);
            store.setTunnelId(teTunnel.teTunnelKey(), id);
        }
        return id;
    }

    @Override
    public void setTunnelId(TeTunnelKey teTunnelKey, TunnelId tunnelId) {
        store.setTunnelId(teTunnelKey, tunnelId);
    }

    @Override
    public void updateTeTunnel(TeTunnel teTunnel) {
        //TODO: updateTeTunnel
    }

    @Override
    public void updateTunnelState(TeTunnelKey key, Tunnel.State state) {
        tunnelAdminService.updateTunnelState(
                tunnelService.queryTunnel(getTunnelId(key)), state);
    }

    @Override
    public TeLspKey teLspAdded(TeLsp lsp) {
        if (store.addTeLsp(lsp)) {
            return lsp.teLspKey();
        }

        return null;
    }

    @Override
    public void teLspRemoved(TeLsp lsp) {
        store.removeTeLsp(lsp.teLspKey());
    }

    @Override
    public void updateTeLsp(TeLsp lsp) {
        store.updateTeLsp(lsp);
    }

    @Override
    public void removeTeTunnel(TeTunnelKey teTunnelKey) {
        tunnelAdminService.updateTunnelState(
                tunnelService.queryTunnel(getTunnelId(teTunnelKey)),
                Tunnel.State.REMOVING);
        List<TeTunnelKey> segmentTunnels =
                getTeTunnel(teTunnelKey).segmentTunnels();
        if (segmentTunnels == null || segmentTunnels.isEmpty()) {
            // this is a single domain tunnel, removes it right away
            tunnelAdminService.removeTunnel(getTunnelId(teTunnelKey));
        }
    }

    @Override
    public void setSegmentTunnel(TeTunnelKey e2eTunnelKey,
                                 List<TeTunnelKey> segmentTunnels) {
        TeTunnel e2eTunnel = store.getTeTunnel(e2eTunnelKey);
        if (e2eTunnel == null) {
            log.error("unknown e2eTunnelKey: {}", e2eTunnelKey);
            return;
        }
        e2eTunnel.segmentTunnels(segmentTunnels);

        for (TeTunnelKey key : segmentTunnels) {
            TeTunnel segmentTunnel = store.getTeTunnel(key);
            if (segmentTunnel == null) {
                log.warn("unknown segmentTunnel: {}", key);
                continue;
            }
            segmentTunnel.e2eTunnelKey(e2eTunnelKey);
        }
    }

    @Override
    public TeTunnel getTeTunnel(TeTunnelKey key) {
        return store.getTeTunnel(key);
    }

    @Override
    public TeTunnel getTeTunnel(TunnelId id) {
        return store.getTeTunnel(id);
    }

    @Override
    public TunnelId getTunnelId(TeTunnelKey key) {
        return store.getTunnelId(key);
    }

    @Override
    public Collection<TeTunnel> getTeTunnels() {
        return store.getTeTunnels();
    }

    @Override
    public Collection<TeTunnel> getTeTunnels(TeTunnel.Type type) {
        return store.getTeTunnels(type);
    }

    @Override
    public Collection<TeTunnel> getTeTunnels(TeTopologyKey key) {
        return store.getTeTunnels(key);
    }

    @Override
    public TeLsp getTeLsp(TeLspKey key) {
        return store.getTeLsp(key);
    }

    @Override
    public Collection<TeLsp> getTeLsps() {
        return store.getTeLsps();
    }

    @Override
    public TunnelId teTunnelAdded(TeTunnel teTunnel) {
        //TODO teTunnelAdded
        return null;
    }

    @Override
    public void teTunnelRemoved(TeTunnel teTunnel) {
        TeTunnelKey e2eTunnelKey = teTunnel.e2eTunnelKey();
        store.removeTeTunnel(teTunnel.teTunnelKey());

        // it's a segment tunnel
        if (e2eTunnelKey != null) {
            boolean finished = true;
            for (TeTunnelKey key : getTeTunnel(e2eTunnelKey).segmentTunnels()) {
                if (getTeTunnel(key) != null) {
                    // FIXME need a better way to determine whether a segment tunnel is removed.
                    finished = false;
                }
            }
            if (finished) {
                // all segment tunnels are removed
                tunnelAdminService.removeTunnel(getTunnelId(e2eTunnelKey));
                store.removeTeTunnel(e2eTunnelKey);
            }
        }
    }
}
