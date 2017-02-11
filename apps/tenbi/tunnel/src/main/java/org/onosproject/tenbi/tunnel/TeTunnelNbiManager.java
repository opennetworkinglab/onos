/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.tenbi.tunnel;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetunnel.api.TeTunnelAdminService;
import org.onosproject.tetunnel.api.TeTunnelService;
import org.onosproject.tetunnel.api.tunnel.DefaultTeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTeOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTeService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.IetfTeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.IetfTeEventListener;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.tunnels.Tunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.IetfTeTypes;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.tetopology.management.api.TeTopology.BIT_MERGED;
import static org.onosproject.teyang.utils.tunnel.TunnelConverter.buildIetfTeWithTunnels;
import static org.onosproject.teyang.utils.tunnel.TunnelConverter.te2YangTunnelConverter;
import static org.onosproject.teyang.utils.tunnel.TunnelConverter.yang2TeTunnel;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * The IETF TE Tunnel NBI Manager implementation.
 */
@Component(immediate = true)
@Service
public class TeTunnelNbiManager
        extends AbstractListenerManager<IetfTeEvent, IetfTeEventListener>
        implements IetfTeService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelService tunnelService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService toplogyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTunnelAdminService tunnelAdminService;

    @Activate
    protected void activate() {
        ymsService.registerService(this, IetfTeService.class, null);
        ymsService.registerService(null, IetfTeTypes.class, null);
        log.info("started");
    }

    @Deactivate
    protected void deactivate() {
        ymsService.unRegisterService(this, IetfTeService.class);
        ymsService.unRegisterService(null, IetfTeTypes.class);
        log.info("stopped");
    }

    @Override
    public IetfTe getIetfTe(IetfTeOpParam ietfTe) {
        List<Tunnel> tunnels = new ArrayList<>();
        Collection<TeTunnel> teTunnels = tunnelService.getTeTunnels();
        teTunnels.forEach(teTunnel -> {
            Tunnel tunnel = te2YangTunnelConverter(teTunnel, false);
            tunnels.add(tunnel);
        });
        IetfTe newIetfTe = buildIetfTeWithTunnels(tunnels);
        return ietfTe.processSubtreeFiltering(newIetfTe, false);
    }

    @Override
    public void setIetfTe(IetfTeOpParam ietfTe) {
        checkNotNull(ietfTe, "Ietf te params should not be null");
        //FIXME use topology id configured by user
        // for there is no topology id param in the definition of te tunnel
        // we use the merged topology id as the default topology where we create
        // the tunnel, need to talk with the ietf-te draft writer.
        TeTopologyKey topologyKey = getTopologyKey();
        if (topologyKey == null) {
            log.error("No usable topology now!");
            return;
        }

        ietfTe.te().tunnels().tunnel().forEach(tunnel -> {
            DefaultTeTunnel teTunnel = yang2TeTunnel(tunnel, topologyKey);
            tunnelAdminService.createTeTunnel(teTunnel);
        });
    }

    @Override
    public void globalsRpc() {

    }

    @Override
    public void interfacesRpc() {

    }

    @Override
    public void tunnelsRpc() {
        //TODO add implement for the te tunnel rpc
    }

    private TeTopologyKey getTopologyKey() {
        TeTopologyKey key = null;
        Optional<TeTopology> teTopology = toplogyService
                .teTopologies()
                .teTopologies()
                .values()
                .stream()
                .filter(topology -> topology.flags().get(BIT_MERGED))
                .findFirst();
        if (teTopology.isPresent()) {
            TeTopology topology = teTopology.get();
            key = topology.teTopologyId();
        }
        return key;
    }
}
