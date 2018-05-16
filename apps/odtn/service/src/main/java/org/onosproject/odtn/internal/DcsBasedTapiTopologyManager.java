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

import java.util.List;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.XmlString;
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

import static org.onosproject.odtn.utils.YangToolUtil.toCharSequence;
import static org.onosproject.odtn.utils.YangToolUtil.toCompositeData;
import static org.onosproject.odtn.utils.YangToolUtil.toXmlCompositeStream;

import org.onosproject.odtn.utils.tapi.TapiLinkBuilder;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.odtn.utils.tapi.TapiResolver;
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
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
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

    private DefaultContext context = new DefaultContext();
    private DefaultTopology topology = new DefaultTopology();

    private TapiResolver tapiResolver = new TapiResolver();

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
        TapiNodeBuilder builder = TapiNodeBuilder.builder()
                .setTopologyUuid(topology.uuid())
                .setDeviceId(deviceId);
        addModelObjectDataToDcs(builder.build());

        TapiNodeRef nodeRef = new TapiNodeRef(topology.uuid().toString(), builder.getUuid().toString());
        nodeRef.setDeviceId(deviceId);
        tapiResolver.addNodeRef(nodeRef);
    }

    @Override
    public void removeDevice(Device device) {
        log.info("Remove device: {}", device);
    }

    @Override
    public void addLink(Link link) {
        log.info("Add link: {}", link);

        // validation check

        // src nep
        addNep(link.src());
        addNep(link.dst());

        // link
        TapiNepRef srcNepRef = tapiResolver.getNepRef(link.src());
        TapiNepRef dstNepRef = tapiResolver.getNepRef(link.dst());

        TapiLinkBuilder linkBuilder = TapiLinkBuilder.builder()
                .setTopologyUuid(topology.uuid())
                .setNep(srcNepRef)
                .setNep(dstNepRef);
        addModelObjectDataToDcs(linkBuilder.build());
    }

    @Override
    public void removeLink(Link link) {
        log.info("Remove link: {}", link);
    }

    @Override
    public void addPort(Port port) {
        log.info("Add port: {}", port);
        if (tapiResolver.hasNepRef(new ConnectPoint(port.element().id(), port.number()))) {
            return;
        }

        TapiNodeRef nodeRef = tapiResolver.getNodeRef(port.element().id());
        String nodeId = nodeRef.getNodeId();

        // nep
        TapiNepBuilder nepBuilder = TapiNepBuilder.builder()
                .setPort(port)
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(Uuid.fromString(nodeId));

        TapiNepRef nepRef = new TapiNepRef(topology.uuid().toString(), nodeId, nepBuilder.getUuid().toString());
        nepRef.setConnectPoint(nepBuilder.getConnectPoint());

        // sip
        if (TapiSipBuilder.isSip(port)) {
            TapiSipBuilder sipBuilder = TapiSipBuilder.builder().setPort(port);
            nepBuilder.setSip(sipBuilder.getUuid());
            nepRef.setSipId(sipBuilder.getUuid().toString());

            addModelObjectDataToDcs(sipBuilder.build());
        }

        addModelObjectDataToDcs(nepBuilder.build());
        tapiResolver.addNepRef(nepRef);
    }

    @Override
    public void removePort(Port port) {
        log.info("Remove port: {}", port);
    }

    private void addNep(ConnectPoint cp) {

        log.info("device Id: {}", cp.deviceId());
        TapiNodeRef nodeRef = tapiResolver.getNodeRef(cp.deviceId());
        String nodeId = nodeRef.getNodeId();

        TapiNepBuilder nepBuilder = TapiNepBuilder.builder()
                .setConnectPoint(cp)
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(Uuid.fromString(nodeId));
        TapiNepRef nepRef = new TapiNepRef(topology.uuid().toString(), nodeId, nepBuilder.getUuid().toString());
        nepRef.setConnectPoint(cp);

        addModelObjectDataToDcs(nepBuilder.build());
        tapiResolver.addNepRef(nepRef);
    }

    private void initDcsTapiContext() {
        TapiContextBuilder builder = TapiContextBuilder.builder(context);
        addModelObjectDataToDcs(builder.build());
    }

    private void initDcsTapiTopology() {
        TapiTopologyBuilder builder = TapiTopologyBuilder.builder(topology);
        addModelObjectDataToDcs(builder.build());
    }

    // FIXME: move DCS-related methods to DCS

    private void addModelObjectDataToDcs(ModelObjectData input) {

        ResourceData rnode = modelConverter.createDataNode(input);

        // for debug
        CharSequence strNode = toCharSequence(toXmlCompositeStream(toCompositeData(rnode)));
        log.info("XML:\n{}", XmlString.prettifyXml(strNode));

        addResourceDataToDcs(rnode);
    }

    private void addResourceDataToDcs(ResourceData input) {
        addResourceDataToDcs(input, input.resourceId());
    }

    private void addResourceDataToDcs(ResourceData input, ResourceId rid) {
        if (input == null || input.dataNodes() == null) {
            return;
        }
        List<DataNode> dataNodes = input.dataNodes();
        for (DataNode node : dataNodes) {
            dcs.createNode(rid, node);
        }
    }

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
