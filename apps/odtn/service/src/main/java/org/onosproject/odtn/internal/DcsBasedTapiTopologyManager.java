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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.FailedException;
import org.onosproject.config.Filter;
import org.onosproject.d.config.DeviceResourceIds;

import static org.onosproject.d.config.DeviceResourceIds.DCS_NAMESPACE;

import org.onosproject.d.config.ResourceIds;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;

import org.onosproject.odtn.utils.tapi.TapiLinkBuilder;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.odtn.utils.tapi.TapiContextBuilder;
import org.onosproject.odtn.utils.tapi.TapiNepBuilder;
import org.onosproject.odtn.utils.tapi.TapiNodeBuilder;
import org.onosproject.odtn.utils.tapi.TapiSipBuilder;
import org.onosproject.odtn.utils.tapi.TapiTopologyBuilder;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.InnerNode;
import org.onosproject.yang.model.ModelConverter;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * OSGi Component for ODTN Tapi manager application.
 */
@Component(immediate = true)
@Service
public class DcsBasedTapiTopologyManager implements TapiTopologyManager {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dcs;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ModelConverter modelConverter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TapiResolver tapiResolver;

    private DefaultContext context = new DefaultContext();
    private DefaultTopology topology = new DefaultTopology();

    @Activate
    public void activate() {
        initDcsIfRootNotExist();
        initDcsTapiContext();
        initDcsTapiTopology();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public void addDevice(Device device) {
        log.info("Add device: {}", device);
        DeviceId deviceId = device.id();
        if (tapiResolver.hasNodeRef(deviceId)) {
            return;
        }
        TapiNodeBuilder.builder()
                .setTopologyUuid(topology.uuid())
                .setDeviceId(deviceId).build();
    }

    @Override
    public void removeDevice(Device device) {
        log.info("Remove device: {}", device);
    }

    @Override
    public void addLink(Link link) {
        log.info("Add link: {}", link);

        // TODO: existence check

        // link
        TapiNepRef srcNepRef = tapiResolver.getNepRef(link.src());
        TapiNepRef dstNepRef = tapiResolver.getNepRef(link.dst());

        TapiLinkBuilder.builder()
                .setTopologyUuid(topology.uuid())
                .addNep(srcNepRef)
                .addNep(dstNepRef).build();
    }

    @Override
    public void removeLink(Link link) {
        log.info("Remove link: {}", link);
    }

    @Override
    public void addPort(Port port) {
        log.info("Add port: {}", port);

        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        if (tapiResolver.hasNepRef(cp)) {
            return;
        }

        TapiNodeRef nodeRef = tapiResolver.getNodeRef(port.element().id());
        String nodeId = nodeRef.getNodeId();

        // nep
        TapiNepBuilder nepBuilder = TapiNepBuilder.builder()
                .setConnectPoint(cp)
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(Uuid.fromString(nodeId));

        // sip
        if (TapiSipBuilder.isSip(cp)) {

            TapiSipBuilder sipBuilder = TapiSipBuilder.builder().setConnectPoint(cp);
            nepBuilder.addSip(sipBuilder.getUuid());
            sipBuilder.build();
        }
        nepBuilder.build();
    }

    @Override
    public void removePort(Port port) {
        log.info("Remove port: {}", port);
    }

    private void initDcsTapiContext() {
        TapiContextBuilder.builder(context).build();
    }

    private void initDcsTapiTopology() {
        TapiTopologyBuilder.builder(topology).build();
    }

    // FIXME: move DCS-related methods to DCS

    private void initDcsIfRootNotExist() {

        log.info("read root:");
        try {
            DataNode all = dcs.readNode(ResourceIds.ROOT_ID, Filter.builder().build());
            log.info("all: {}", all);
        } catch (FailedException e) {
            // FIXME debug this issue
            log.info("nothing retrievable in DCS?");
            //e.printStackTrace(System.out);
        }
        if (!dcs.nodeExist(ResourceIds.ROOT_ID)) {
            log.info("Root node does not exist!, creating...");
            try {
                log.info("create 'root' node");
                dcs.createNode(null,
                        InnerNode.builder(DeviceResourceIds.ROOT_NAME, DCS_NAMESPACE)
                                .type(DataNode.Type.SINGLE_INSTANCE_NODE).build());
            } catch (FailedException e) {
                log.info("Failed to create root???");
                //e.printStackTrace(System.out);
            }
        }
        if (!dcs.nodeExist(ResourceIds.ROOT_ID)) {
            log.info("'root' was created without error, but still not there. WTF!");
        }
    }

}
