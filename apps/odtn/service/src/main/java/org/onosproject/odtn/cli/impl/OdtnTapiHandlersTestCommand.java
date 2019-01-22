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

package org.onosproject.odtn.cli.impl;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.XmlString;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.odtn.utils.tapi.TapiCepHandler;
import org.onosproject.odtn.utils.tapi.TapiCepRefHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectionHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectivityContextHandler;
import org.onosproject.odtn.utils.tapi.TapiConnectivityServiceHandler;
import org.onosproject.odtn.utils.tapi.TapiContextHandler;
import org.onosproject.odtn.utils.tapi.TapiNepHandler;
import org.onosproject.odtn.utils.tapi.TapiNodeHandler;
import org.onosproject.odtn.utils.tapi.TapiSepHandler;
import org.onosproject.odtn.utils.tapi.TapiSipHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyContextHandler;
import org.onosproject.odtn.utils.tapi.TapiTopologyHandler;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.tapicontext.DefaultServiceInterfacePoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.DefaultConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.DefaultConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivityservice.DefaultEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.augmentedtapicommoncontext.DefaultConnectivityContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.DefaultOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.model.Augmentable;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import static org.onosproject.odtn.utils.YangToolUtil.*;
import static org.slf4j.LoggerFactory.getLogger;

@Service
@Command(scope = "onos", name = "odtn-tapi-handlers-test",
         description = "TAPI Handlers test command")
public class OdtnTapiHandlersTestCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(OdtnTapiHandlersTestCommand.class);
    private DynamicConfigService dcs;
    private ModelConverter modelConverter;
    private TapiContextHandler contextHandler;

    @Override
    public void doExecute() {
        dcs = get(DynamicConfigService.class);
        modelConverter = get(ModelConverter.class);

        setupTapiContext();
        DataNode data = contextHandler.getDataNode();

        ResourceId empty = ResourceId.builder().build();
        CharSequence strNode = toCharSequence(toXmlCompositeStream(toCompositeData(toResourceData(empty, data))));
        StringBuilder exp = loadXml("/test-tapi-context.xml");

        if (XmlString.prettifyXml(strNode).toString().contentEquals(exp)) {
            printlog("result: ok");
        } else {
            printlog("result: failed");
        }
    }

    private void printlog(String format, Object... objs) {
        print(format.replaceAll(Pattern.quote("{}"), "%s"), objs);
        log.debug(format, objs);
    }

    private static StringBuilder loadXml(final String fileName) {

        InputStream inputStream = OdtnTapiHandlersTestCommand.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder result = new StringBuilder();
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void setupTapiContext() {

        DeviceId did1 = DeviceId.deviceId("netconf:127.0.0.1:11001");
        DeviceId did2 = DeviceId.deviceId("netconf:127.0.0.1:11002");

        ConnectPoint cp11 = new ConnectPoint(did1, PortNumber.portNumber(1, "TRANSCEIVER"));
        ConnectPoint cp12 = new ConnectPoint(did1, PortNumber.portNumber(2, "TRANSCEIVER"));
        ConnectPoint cp21 = new ConnectPoint(did2, PortNumber.portNumber(1, "TRANSCEIVER"));
        ConnectPoint cp22 = new ConnectPoint(did2, PortNumber.portNumber(2, "TRANSCEIVER"));

        // context
        contextHandler = TapiContextHandler.create();
        DefaultContext context = contextHandler.getModelObject();

        // context augmentation
        Augmentable augmentableContext = context;

        // context augmentation with topologyContext
        org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context
                .DefaultAugmentedTapiCommonContext augmentedTopologyContext
                = new org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology
                .context.DefaultAugmentedTapiCommonContext();
        augmentableContext.addAugmentation(augmentedTopologyContext);

        // context augmentation with connectivityServiceContext
        org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context
                .DefaultAugmentedTapiCommonContext augmentedConnectivityContext
                = new org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity
                .context.DefaultAugmentedTapiCommonContext();
        augmentableContext.addAugmentation(augmentedConnectivityContext);

        // topology context
        DefaultTopologyContext topologyContext = TapiTopologyContextHandler.create().getModelObject();
        augmentedTopologyContext.topologyContext(topologyContext);

        // topology
        TapiTopologyHandler topologyHandler = TapiTopologyHandler.create();
        topologyHandler.setId(Uuid.of("00000000-0000-0000-0000-000000000001"));
        DefaultTopology topology = topologyHandler.getModelObject();
        topologyContext.addToTopology(topology);

        // nodes
        TapiNodeHandler nodeHandler1 = TapiNodeHandler.create();
        nodeHandler1.setId(Uuid.of("00000000-0000-0000-0001-000000000001"));
        DefaultNode node1 = nodeHandler1.setTopologyUuid(topology.uuid())
                .setDeviceId(did1)
                .getModelObject();
        topology.addToNode(node1);

        TapiNodeHandler nodeHandler2 = TapiNodeHandler.create();
        nodeHandler2.setId(Uuid.of("00000000-0000-0000-0001-000000000002"));
        DefaultNode node2 = nodeHandler2.setTopologyUuid(topology.uuid())
                .setDeviceId(did2)
                .getModelObject();
        topology.addToNode(node2);

        // sips
        TapiSipHandler sipHandler1 = TapiSipHandler.create();
        sipHandler1.setId(Uuid.of("00000000-0000-0000-0002-000000000001"));
        DefaultServiceInterfacePoint sip1 = sipHandler1.setConnectPoint(cp11)
                .getModelObject();
        context.addToServiceInterfacePoint(sip1);

        TapiSipHandler sipHandler2 = TapiSipHandler.create();
        sipHandler2.setId(Uuid.of("00000000-0000-0000-0002-000000000002"));
        DefaultServiceInterfacePoint sip2 = sipHandler2.setConnectPoint(cp21)
                .getModelObject();
        context.addToServiceInterfacePoint(sip2);

        // neps
        TapiNepHandler nepHandler11 = TapiNepHandler.create();
        nepHandler11.setId(Uuid.of("00000000-0000-0000-0003-000000000011"));
        DefaultOwnedNodeEdgePoint nep11 = nepHandler11
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setConnectPoint(cp11)
                .addSip(sip1.uuid())
                .getModelObject();
        nodeHandler1.addNep(nep11);

        TapiNepHandler nepHandler12 = TapiNepHandler.create();
        nepHandler12.setId(Uuid.of("00000000-0000-0000-0003-000000000012"));
        DefaultOwnedNodeEdgePoint nep12 = nepHandler12
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setConnectPoint(cp12)
                .getModelObject();
        nodeHandler1.addNep(nep12);

        TapiNepHandler nepHandler21 = TapiNepHandler.create();
        nepHandler21.setId(Uuid.of("00000000-0000-0000-0003-000000000021"));
        DefaultOwnedNodeEdgePoint nep21 = nepHandler21
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node2.uuid())
                .setConnectPoint(cp21)
                .addSip(sip2.uuid())
                .getModelObject();
        nodeHandler2.addNep(nep21);

        TapiNepHandler nepHandler22 = TapiNepHandler.create();
        nepHandler22.setId(Uuid.of("00000000-0000-0000-0003-000000000022"));
        DefaultOwnedNodeEdgePoint nep22 = nepHandler22
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node2.uuid())
                .setConnectPoint(cp22)
                .getModelObject();
        nodeHandler2.addNep(nep22);

        // connectivity context
        DefaultConnectivityContext connectivityContext = TapiConnectivityContextHandler.create().getModelObject();
        augmentedConnectivityContext.connectivityContext(connectivityContext);

        // connectivityService
        TapiConnectivityServiceHandler connectivityServiceHandler = TapiConnectivityServiceHandler.create();
        connectivityServiceHandler.setId(Uuid.of("00000000-0000-0000-0004-000000000001"));
        DefaultConnectivityService connectivityService = connectivityServiceHandler.getModelObject();
        connectivityContext.addToConnectivityService(connectivityService);

        // connection
        TapiConnectionHandler connectionHandler = TapiConnectionHandler.create();
        connectionHandler.setId(Uuid.of("00000000-0000-0000-0005-000000000001"));
        DefaultConnection connection1 = connectionHandler.getModelObject();
        connectivityServiceHandler.addConnection(connection1.uuid());
        connectivityContext.addToConnection(connection1);


        // seps
        TapiSepHandler sepHandler1 = TapiSepHandler.create();
        sepHandler1.setId(Uuid.of("00000000-0000-0000-0006-000000000001"));
        DefaultEndPoint sep1 = sepHandler1.getModelObject();
        connectivityServiceHandler.addSep(sep1);

        TapiSepHandler sepHandler2 = TapiSepHandler.create();
        sepHandler2.setId(Uuid.of("00000000-0000-0000-0006-000000000002"));
        DefaultEndPoint sep2 = sepHandler2.getModelObject();
        connectivityServiceHandler.addSep(sep2);

        // ceps
        TapiCepHandler cepHandler11 = TapiCepHandler.create();
        cepHandler11.setId(Uuid.of("00000000-0000-0000-0007-000000000011"));
        org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity
                .ceplist.DefaultConnectionEndPoint
                cep11 = cepHandler11
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setNepUuid(nep11.uuid())
                .setParentNep()
                .getModelObject();
        nepHandler11.addCep(cep11);
        org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity
                .connection.DefaultConnectionEndPoint
                cepRef11 = TapiCepRefHandler.create()
                .setCep(cep11)
                .getModelObject();
        connectionHandler.addCep(cepRef11);

        TapiCepHandler cepHandler21 = TapiCepHandler.create();
        cepHandler21.setId(Uuid.of("00000000-0000-0000-0007-000000000021"));
        org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity
                .ceplist.DefaultConnectionEndPoint
                cep21 = cepHandler21
                .setTopologyUuid(topology.uuid())
                .setNodeUuid(node1.uuid())
                .setNepUuid(nep21.uuid())
                .setParentNep()
                .getModelObject();
        nepHandler21.addCep(cep21);
        org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity
                .connection.DefaultConnectionEndPoint
                cepRef21 = TapiCepRefHandler.create()
                .setCep(cep21)
                .getModelObject();
        connectionHandler.addCep(cepRef21);

    }
}
