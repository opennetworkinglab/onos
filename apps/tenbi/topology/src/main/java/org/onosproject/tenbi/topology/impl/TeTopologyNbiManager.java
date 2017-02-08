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
package org.onosproject.tenbi.topology.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.event.AbstractListenerManager;
import org.onosproject.tetopology.management.api.TeTopologyEvent;
import org.onosproject.tetopology.management.api.TeTopologyListener;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.teyang.api.OperationType;
import org.onosproject.teyang.utils.topology.NetworkConverter;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetwork.OnosYangOpType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetworkOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetworkService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.Networks;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworksState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.IetfNetworkTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
        .rev20151208.IetfNetworkTopologyOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology
        .rev20151208.IetfNetworkTopologyService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.IetfTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.IetfTeTopologyOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.IetfTeTopologyService;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .IetfTeTopologyEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology
        .IetfTeTopologyEventListener;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The IETF TE Topology NBI Manager implementation.
 */
@Component(immediate = true)
@Service
public class TeTopologyNbiManager
        extends AbstractListenerManager<IetfTeTopologyEvent, IetfTeTopologyEventListener>
        implements IetfNetworkService, IetfNetworkTopologyService, IetfTeTopologyService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService teTopologyService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;


    /**
     * Activation helper function.
     */
    private void activateBasics() {
        eventDispatcher.addSink(IetfTeTopologyEvent.class, listenerRegistry);
    }

    /**
     * Deactivation helper function.
     */
    private void deactivateBasics() {
        eventDispatcher.removeSink(IetfTeTopologyEvent.class);
    }

    @Activate
    protected void activate() {
        activateBasics();

        // Register 3 services with YMS.
        ymsService.registerService(this, IetfNetworkService.class, null);
        ymsService.registerService(this, IetfNetworkTopologyService.class, null);
        ymsService.registerService(this, IetfTeTopologyService.class, null);

        // Listens to TE Topology events
        teTopologyService.addListener(new InternalTeTopologyListener());

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        deactivateBasics();

        // Unregister 3 services.
        ymsService.unRegisterService(this, IetfNetworkService.class);
        ymsService.unRegisterService(this, IetfNetworkTopologyService.class);
        ymsService.unRegisterService(this, IetfTeTopologyService.class);

        teTopologyService.removeListener(new InternalTeTopologyListener());
        log.info("Stopped");
    }

    @Override
    public IetfNetwork getIetfNetwork(IetfNetworkOpParam ietfNetwork) {
        checkNotNull(ietfNetwork, "getIetfNetwork: ietfNetwork cannot be null");

        // Get the entire data tree from TE Subsystem core.
        org.onosproject.tetopology.management.api.Networks teNetworks = teTopologyService.networks();

        // Build the sample networks for RESTCONF/YMS integration test
//        org.onosproject.tetopology.management.api.Networks teNetworks = new DefaultNetworks(DefaultBuilder
//                .sampleDomain1Networks());

        // Convert the TE Subsystem core data into YANG Objects.
        Networks networks = NetworkConverter
                .teSubsystem2YangNetworks(teNetworks, OperationType.QUERY,
                                          teTopologyService);
        NetworksState networkStates = NetworkConverter.teSubsystem2YangNetworkStates(teNetworks, OperationType.QUERY);

        IetfNetworkOpParam.IetfNetworkBuilder builder = new IetfNetworkOpParam.IetfNetworkBuilder();
        IetfNetwork newNetwork = builder.networks(networks)
                .networksState(networkStates)
                .yangIetfNetworkOpType(OnosYangOpType.NONE)
                .build();

        // processSubtreeFiltering() filters the entire data tree based on the
        // user's query and returns the filtered data.
        IetfNetwork result = ietfNetwork.processSubtreeFiltering(
                newNetwork,
                false);
        log.debug("result is: {}", result);
        return result;
    }

    @Override
    public void setIetfNetwork(IetfNetworkOpParam ietfNetwork) {
        // In H release, topology is discovered from south, no NBI Set is supported.
    }

    @Override
    public IetfTeTopology getIetfTeTopology(IetfTeTopologyOpParam ietfTeTopology) {
        // unused method.
        return ietfTeTopology;
    }

    @Override
    public void setIetfTeTopology(IetfTeTopologyOpParam ietfTeTopology) {
        // unused methods.
    }

    @Override
    public IetfTeTopology getAugmentedIetfTeTopologyTeLinkEvent(IetfTeTopologyOpParam ietfTeTopology) {
        // unused methods.
        return ietfTeTopology;
    }

    @Override
    public void setAugmentedIetfTeTopologyTeLinkEvent(IetfTeTopologyOpParam augmentedIetfTeTopologyTeLinkEvent) {
        // unused methods.
    }

    @Override
    public IetfNetworkTopology getIetfNetworkTopology(IetfNetworkTopologyOpParam ietfNetworkTopology) {
        // unused methods.
        return ietfNetworkTopology;
    }

    @Override
    public void setIetfNetworkTopology(IetfNetworkTopologyOpParam ietfNetworkTopology) {
        // unused methods.
    }

    @Override
    public IetfNetwork getAugmentedIetfNetworkNetworks(IetfNetworkOpParam ietfNetwork) {
        // unused methods.
        return ietfNetwork;
    }

    @Override
    public void setAugmentedIetfNetworkNetworks(IetfNetworkOpParam augmentedIetfNetworkNetworks) {
        // unused methods.
    }

    private class InternalTeTopologyListener implements TeTopologyListener {
        @Override
        public void event(TeTopologyEvent event) {
            IetfTeTopologyEvent yangEvent = NetworkConverter
                    .teTopoEvent2YangIetfTeTopoEvent(event, teTopologyService);
            post(yangEvent);
        }
    }
}
