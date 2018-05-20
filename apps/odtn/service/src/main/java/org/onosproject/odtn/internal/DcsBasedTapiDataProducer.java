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
import java.util.List;
import java.util.stream.Collectors;
import org.onosproject.config.DynamicConfigService;

import org.onosproject.config.Filter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.odtn.utils.tapi.DcsBasedTapiNepRef;
import org.onosproject.odtn.utils.tapi.DcsBasedTapiNodeRef;
import org.onosproject.odtn.utils.tapi.TapiNepRef;
import org.onosproject.odtn.utils.tapi.TapiNodeRef;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.globalclass.Name;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.context.DefaultAugmentedTapiCommonContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.Topology;
import org.onosproject.yang.model.Augmentable;
import org.onosproject.yang.model.DataNode;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ModelConverter;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.ResourceId;
import org.slf4j.Logger;

import static org.onlab.osgi.DefaultServiceDirectory.getService;
import static org.onosproject.odtn.utils.tapi.TapiInstanceBuilder.DEVICE_ID;
import static org.onosproject.odtn.utils.tapi.TapiInstanceBuilder.ONOS_CP;
import static org.slf4j.LoggerFactory.getLogger;

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
        ModelObject context = readContextModelObject();
        updateCache(resolver, context);
    }

    @VisibleForTesting
    protected void updateCache(DefaultTapiResolver resolver, ModelObject context) {
        updateNodes(resolver, getNodes(context));
        updateNeps(resolver, getNeps(context));
    }

    private ModelObject readContextModelObject() {
        // read DataNode from DCS
        ModelObjectId mid = ModelObjectId.builder().addChild(DefaultContext.class).build();
        DataNode node = dcs.readNode(getResourceId(mid), Filter.builder().build());

        // convert to ModelObject
        ResourceData data = DefaultResourceData.builder().addDataNode(node)
                .resourceId(ResourceId.builder().build()).build();
        ModelObjectData modelData = modelConverter.createModel(data);
        ModelObject context = modelData.modelObjects().get(0);

        return context;
    }

    private List<TapiNodeRef> getNodes(ModelObject context) {
        Augmentable augmentedContext = (Augmentable) context;
        DefaultAugmentedTapiCommonContext topologyContext
                = augmentedContext.augmentation(DefaultAugmentedTapiCommonContext.class);
        Topology topology = topologyContext.topology().get(0);

        if (topology.node() == null) {
            return Collections.emptyList();
        }
        return topology.node().stream()
                .map(node -> {
                    String deviceId = node.name().stream()
                            .filter(kv -> kv.valueName().equals(DEVICE_ID))
                            .findFirst().map(Name::value).get();
                    return DcsBasedTapiNodeRef.create(topology, node)
                            .setDeviceId(DeviceId.deviceId(deviceId));
                })
                .collect(Collectors.toList());
    }

    private List<TapiNepRef> getNeps(ModelObject context) {
        Augmentable augmentedContext = (Augmentable) context;
        DefaultAugmentedTapiCommonContext topologyContext
                = augmentedContext.augmentation(DefaultAugmentedTapiCommonContext.class);
        Topology topology = topologyContext.topology().get(0);

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
                                        String onosConnectPoint = nep.name().stream()
                                                .filter(kv -> kv.valueName().equals(ONOS_CP))
                                                .findFirst().map(Name::value).get();
                                        TapiNepRef nepRef = DcsBasedTapiNepRef.create(topology, node, nep)
                                                .setConnectPoint(ConnectPoint.fromString(onosConnectPoint));
                                        if (nep.mappedServiceInterfacePoint() != null) {
                                            nep.mappedServiceInterfacePoint().stream()
                                                    .forEach(sip -> {
                                                        nepRef.setSipId(sip.serviceInterfacePointId().toString());
                                                    });
                                        }
                                        return nepRef;
                                    });
                        }
                ).collect(Collectors.toList());
        return ret;
    }

    private void updateNodes(DefaultTapiResolver resolver, List<TapiNodeRef> nodes) {
        resolver.addNodeRefList(nodes);
    }

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
