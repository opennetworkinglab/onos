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

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onosproject.config.DynamicConfigService;

import org.onosproject.config.Filter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.odtn.utils.tapi.DcsBasedTapiObjectRefFactory;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.globalclass.Name;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.topologycontext.topology.node.ownednodeedgepoint.DefaultAugmentedTapiTopologyOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.Topology;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.DEVICE_ID;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ODTN_PORT_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiObjectHandler.ONOS_CP;
import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.CONNECTION_ID;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * DCS-dependent Tapi Data producer implementation.
 */
public class DcsBasedTapiDataProducer implements TapiDataProducer {

    private final Logger log = getLogger(getClass());

    protected DynamicConfigService dcs;
    protected ModelConverter modelConverter;

    @Override
    public void init() {
        dcs = getService(DynamicConfigService.class);
        modelConverter = getService(ModelConverter.class);
    }

    @Override
    public void updateCacheRequest(DefaultTapiResolver resolver) {
        updateCache(resolver, readContextModelObject());
    }

    /**
     * Update resolver's cache with Tapi context modelObject.
     *
     * @param resolver TapiResolver
     * @param context Context ModelObject which has all data nodes of Tapi DataTree in Dcs store
     */
    @VisibleForTesting
    protected void updateCache(DefaultTapiResolver resolver, DefaultContext context) {
        updateNodes(resolver, getNodes(context));
        updateNeps(resolver, getNeps(context));
    }

    /**
     * Get Tapi context modelObject from Dcs.
     *
     * @return Tapi context modelObject in Dcs store
     */
    // FIXME update this method using TapiContextHandler
    private DefaultContext readContextModelObject() {
        // read DataNode from DCS
        ModelObjectId mid = ModelObjectId.builder().addChild(DefaultContext.class).build();
        DataNode node = dcs.readNode(getResourceId(mid), Filter.builder().build());

        // convert to ModelObject
        ResourceData data = DefaultResourceData.builder().addDataNode(node)
                .resourceId(ResourceId.builder().build()).build();
        ModelObjectData modelData = modelConverter.createModel(data);
        DefaultContext context = (DefaultContext) modelData.modelObjects().get(0);

        return context;
    }

    /**
     * Extract Tapi Nodes from context modelObject and convert them to NodeRefs.
     *
     * @param context
     * @return List of NodeRef
     */
    private List<TapiNodeRef> getNodes(DefaultContext context) {
        DefaultAugmentedTapiCommonContext topologyContext
                = context.augmentation(DefaultAugmentedTapiCommonContext.class);
        Topology topology = topologyContext.topologyContext().topology().get(0);

        if (topology.node() == null) {
            return Collections.emptyList();
        }
        return topology.node().stream()
                .map(node -> {
                    TapiNodeRef nodeRef = DcsBasedTapiObjectRefFactory.create(topology, node);
                    if (node.name() != null) {
                        String deviceId = node.name().stream()
                                .filter(kv -> kv.valueName().equals(DEVICE_ID))
                                .findFirst().map(Name::value).get();
                        nodeRef.setDeviceId(DeviceId.deviceId(deviceId));
                    }
                    return nodeRef;
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract Tapi Neps from context modelObject and convert them to NepRefs.
     *
     * @param context
     * @return List of TapiNepRef
     */
    private List<TapiNepRef> getNeps(DefaultContext context) {
        DefaultAugmentedTapiCommonContext topologyContext
                = context.augmentation(DefaultAugmentedTapiCommonContext.class);
        Topology topology = topologyContext.topologyContext().topology().get(0);

        if (topology.node() == null) {
            return Collections.emptyList();
        }
        List<TapiNepRef> ret = topology.node().stream()
                .flatMap(node -> {
                            if (node.ownedNodeEdgePoint() == null) {
                                return null;
                            }
                            return node.ownedNodeEdgePoint().stream()
                                    .map(nep -> {
                                        TapiNepRef nepRef = DcsBasedTapiObjectRefFactory.create(topology, node, nep);
                                        if (nep.name() != null) {
                                            Map<String, String> kvs = new HashMap<>();
                                            nep.name().forEach(kv -> kvs.put(kv.valueName(), kv.value()));

                                            String onosConnectPoint = kvs.getOrDefault(ONOS_CP, null);
                                            String portType = kvs.getOrDefault(ODTN_PORT_TYPE, null);
                                            String connectionId = kvs.getOrDefault(CONNECTION_ID, null);
                                            nepRef.setConnectPoint(ConnectPoint.fromString(onosConnectPoint))
                                                    .setPortType(portType)
                                                    .setConnectionId(connectionId);
                                        }
                                        if (nep.mappedServiceInterfacePoint() != null) {
                                            nep.mappedServiceInterfacePoint().stream()
                                                    .forEach(sip -> {
                                                        nepRef.setSipId(sip.serviceInterfacePointUuid().toString());
                                                    });
                                        }

                                        DefaultAugmentedTapiTopologyOwnedNodeEdgePoint augmentNep =
                                                nep.augmentation(DefaultAugmentedTapiTopologyOwnedNodeEdgePoint.class);
                                        try {
                                            if (augmentNep.cepList().connectionEndPoint() != null) {
                                                List<String> cepIds = augmentNep.cepList().connectionEndPoint().stream()
                                                        .map(cep -> cep.uuid().toString()).collect(Collectors.toList());
                                                nepRef.setCepIds(cepIds);
                                            }
                                        } catch (NullPointerException e) {
                                            log.warn("Augmented ownedNodeEdgePoint is not found.");
                                        }
                                        return nepRef;
                                    });
                        }
                ).collect(Collectors.toList());
        return ret;
    }

    /**
     * Update resolver's NodeRef list.
     *
     * @param resolver TapiResolver
     * @param nodes List of NodeRef for update
     */
    private void updateNodes(DefaultTapiResolver resolver, List<TapiNodeRef> nodes) {
        resolver.addNodeRefList(nodes);
    }

    /**
     * Update resolver's NepRef list.
     *
     * @param resolver TapiResolver
     * @param neps List of NepRef for update
     */
    private void updateNeps(DefaultTapiResolver resolver, List<TapiNepRef> neps) {
        resolver.addNepRefList(neps);
    }

    private ResourceId getResourceId(ModelObjectId modelId) {
        ModelObjectData data = DefaultModelObjectData.builder()
                .identifier(modelId)
                .build();
        ResourceData rnode = modelConverter.createDataNode(data);
        return rnode.resourceId();
    }

}
