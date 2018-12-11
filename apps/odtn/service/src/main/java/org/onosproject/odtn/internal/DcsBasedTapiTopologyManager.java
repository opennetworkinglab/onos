/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.internal;

import org.onosproject.config.DynamicConfigService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.TapiTopologyManager;
import org.onosproject.odtn.utils.tapi.TapiConnectivityContextHandler;
import org.onosproject.odtn.utils.tapi.TapiCepHandler;
import org.onosproject.odtn.utils.tapi.TapiContextHandler;
import org.onosproject.odtn.utils.tapi.TapiLinkHandler;
import org.onosproject.odtn.utils.tapi.TapiNepHandler;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeHandler;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.odtn.utils.tapi.TapiSipHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyContextHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyHandler;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.model.ModelConverter;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OSGi Component for ODTN TAPI topology manager application.
 */
@Component(immediate = true, service = TapiTopologyManager.class)
public class DcsBasedTapiTopologyManager implements TapiTopologyManager {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DynamicConfigService dcs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ModelConverter modelConverter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TapiResolver tapiResolver;

    private DefaultContext context;
    private DefaultTopology topology;

    @Activate
    public void activate() {
        initDcsTapiContext();
        initDcsTapiTopology();
        initDcsTapiConnectivityContext();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addDevice(Device device) {
        log.debug("Add device: {}", device);
        DeviceId deviceId = device.id();
        if (tapiResolver.hasNodeRef(deviceId)) {
            return;
        }
        TapiNodeHandler.create()
                .setTopologyUuid(topology.uuid())
                .setDeviceId(deviceId).add();
    }

    @Override
    public void removeDevice(Device device) {
        //TODO Implement
        log.warn("NOT-IMPLEMENTED, Remove device: {}", device);
    }

    @Override
    public void addLink(Link link) {
        log.debug("Add link: {}", link);

        // TODO: existence check

        // link
        TapiNepRef srcNepRef = tapiResolver.getNepRef(link.src());
        TapiNepRef dstNepRef = tapiResolver.getNepRef(link.dst());

        TapiLinkHandler.create()
                .setTopologyUuid(topology.uuid())
                .addNep(srcNepRef)
                .addNep(dstNepRef).add();
    }

    @Override
    public void removeLink(Link link) {
        //TODO Implement
        log.warn("NOT-IMPLEMENTED, Remove link: {}", link);
    }

    @Override
    public void addPort(Port port) {
        log.debug("Add port: {}", port);

        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        if (tapiResolver.hasNepRef(cp)) {
            return;
        }

        TapiNodeRef nodeRef = tapiResolver.getNodeRef(port.element().id());
        String nodeId = nodeRef.getNodeId();

        // nep
        TapiNepHandler nepBuilder = TapiNepHandler.create()
                .setPort(port)
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(Uuid.fromString(nodeId));

        // cep
        TapiCepHandler cepBuilder = TapiCepHandler.create()
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(Uuid.fromString(nodeId))
                .setNepUuid(nepBuilder.getId())
                .setParentNep();
        nepBuilder.addCep(cepBuilder.getModelObject());

        if (TapiSipHandler.isSip(port)) {
            TapiSipHandler sipBuilder = TapiSipHandler.create().setPort(port);
            nepBuilder.addSip(sipBuilder.getId());

            sipBuilder.add();
        }

        nepBuilder.add();
    }

    @Override
    public void removePort(Port port) {
        //TODO implement
        log.warn("NOT-IMPLEMENTED, Remove port: {}", port);
    }

    /**
     * Add Tapi Context to Dcs store.
     */
    private void initDcsTapiContext() {
        TapiContextHandler contextHandler = TapiContextHandler.create();
        context = contextHandler.getModelObject();
        contextHandler.add();
    }

    /**
     * Add Tapi TopologyContext and Topology to Dcs store.
     *
     * Assumed there is only one topology for ODTN Phase 1.0
     */
    private void initDcsTapiTopology() {
        TapiTopologyContextHandler topologyContextHandler = TapiTopologyContextHandler.create();
        topologyContextHandler.add();
        TapiTopologyHandler topologyHandler = TapiTopologyHandler.create();
        topologyHandler.add();
        topology = topologyHandler.getModelObject();
    }

    /**
     * Add Tapi ConnectivityContext to Dcs store.
     *
     * Assumed there is only one topology for ODTN Phase 1.0
     */
    private void initDcsTapiConnectivityContext() {
        TapiConnectivityContextHandler connectivityContextHandler = TapiConnectivityContextHandler.create();
        connectivityContextHandler.add();
    }

}
