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
 *
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 * Contact: Ramon Casellas <ramon.casellas@cttc.es>
 */

package org.onosproject.odtn.impl;

import java.util.List;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.yang.model.SchemaContextProvider;
import org.onosproject.yang.runtime.YangRuntimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.Filter;
import org.onosproject.config.FailedException;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_ADDED;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_DELETED;

// import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180216.TapiConnectivity;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.TapiConnectivityService;

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.
    tapiconnectivity.createconnectivityservice.CreateConnectivityServiceInput;

import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.
    tapiconnectivity.createconnectivityservice.createconnectivityserviceinput.EndPoint;



// onos-yang-tools
import org.onosproject.yang.model.DataNode;

import org.onosproject.yang.model.NodeKey;
import org.onosproject.yang.model.ResourceData;
import org.onosproject.yang.model.DefaultResourceData;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelConverter;

import org.onosproject.yang.model.ResourceId;
import org.onosproject.yang.model.SchemaId;

import org.onosproject.yang.model.RpcRegistry;
import org.onosproject.yang.model.RpcService;
import org.onosproject.yang.model.RpcInput;
import org.onosproject.yang.model.RpcOutput;


/**
 * OSGi Component for ODTN Service application.
 */
@Component(immediate = true)
public class ServiceApplicationComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dynConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netcfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YangRuntimeService yangRuntime;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected SchemaContextProvider schemaContextProvider;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RpcRegistry rpcRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ModelConverter modelConverter;



    // Listener for events from the DCS
    private final DynamicConfigListener dynamicConfigServiceListener =
        new InternalDynamicConfigListener();

    // Rpc Service for TAPI Connectivity
    private final RpcService rpcTapiConnectivity =
        new TapiConnectivityRpc();


    @Activate
    protected void activate() {
        log.info("Started");
        dynConfigService.addListener(dynamicConfigServiceListener);
        rpcRegistry.registerRpcService(rpcTapiConnectivity);
    }


    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
        rpcRegistry.unregisterRpcService(rpcTapiConnectivity);
        dynConfigService.removeListener(dynamicConfigServiceListener);
    }




    /**
     * Representation of internal listener, listening for dynamic config event.
     */
    private class InternalDynamicConfigListener implements DynamicConfigListener {

        /**
         * Check if the DCS event should be further processed.
         *
         * @param event config event
         * @return true if event is supported; false otherwise
         */
        @Override
        public boolean isRelevant(DynamicConfigEvent event) {
            // Only care about add and delete
            if ((event.type() != NODE_ADDED) &&
                (event.type() != NODE_DELETED)) {
                return false;
            }
            return true;
        }




        /**
         * Process an Event from the Dynamic Configuration Store.
         *
         * @param event config event
         */
        @Override
        public void event(DynamicConfigEvent event) {
            ResourceId rsId = event.subject();
            DataNode node;
            try {
                Filter filter = Filter.builder().addCriteria(rsId).build();
                node = dynConfigService.readNode(rsId, filter);
            } catch (FailedException e) {
                node = null;
            }
            switch (event.type()) {
                case NODE_ADDED:
                    onDcsNodeAdded(rsId, node);
                    break;

                case NODE_DELETED:
                    onDcsNodeDeleted(node);
                    break;

                default:
                    log.warn("Unknown Event", event.type());
                    break;
            }
        }



        /**
         * Process the event that a node has been added to the DCS.
         *
         * @param rsId ResourceId of the added node
         * @param node added node. Access the key and value
         */
        private void onDcsNodeAdded(ResourceId rsId, DataNode node) {
            switch (node.type()) {
                case SINGLE_INSTANCE_NODE:
                    break;
                case MULTI_INSTANCE_NODE:
                    break;
                case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                    break;
                case MULTI_INSTANCE_LEAF_VALUE_NODE:
                    break;
                default:
                    break;
            }

            NodeKey dataNodeKey = node.key();
            SchemaId schemaId = dataNodeKey.schemaId();
            if (!schemaId.namespace().contains("tapi")) {
                return;
            }

            // Consolidate events
            log.info("namespace {}", schemaId.namespace());
        }


        /**
         * Process the event that a node has been deleted from the DCS.
         *
         * @param dataNode data node
         */
        private void onDcsNodeDeleted(DataNode dataNode) {
            // TODO: Implement release logic
        }

    }





    private class TapiConnectivityRpc implements TapiConnectivityService {


        /**
         * Service interface of createConnectivityService.
         *
         * @param inputVar input of service interface createConnectivityService
         * @return rpcOutput output of service interface createConnectivityService
         */
        @Override
        public RpcOutput createConnectivityService(RpcInput inputVar) {
            DataNode data = inputVar.data();
            ResourceId rid = inputVar.id();

            log.info("RpcInput Data {}", data);
            log.info("RpcInput ResourceId {}", rid);

            for (ModelObject mo : getModelObjects(data, rid)) {
                if (mo instanceof CreateConnectivityServiceInput) {
                    CreateConnectivityServiceInput i = (CreateConnectivityServiceInput) mo;
                    log.info("i {}", i);
                    List<EndPoint> epl = i.endPoint();
                    for (EndPoint ep : epl) {
                        log.info("ep {}", ep);
                    }
                }
            }

            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }



        /**
         * Service interface of deleteConnectivityService.
         *
         * @param inputVar input of service interface deleteConnectivityService
         * @return rpcOutput output of service interface deleteConnectivityService
         */
        @Override
        public RpcOutput deleteConnectivityService(RpcInput inputVar) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);
        }



        /**
         * Service interface of getConnectionDetails.
         *
         * @param inputVar input of service interface getConnectionDetails
         * @return rpcOutput output of service interface getConnectionDetails
         */
        @Override
        public RpcOutput getConnectionDetails(RpcInput inputVar) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);

        }

        /**
         * Service interface of getConnectivityServiceList.
         *
         * @param inputVar input of service interface getConnectivityServiceList
         * @return rpcOutput output of service interface getConnectivityServiceList
         */
        @Override
        public RpcOutput getConnectivityServiceList(RpcInput inputVar) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);

        }

        /**
         * Service interface of getConnectivityServiceDetails.
         *
         * @param inputVar input of service interface getConnectivityServiceDetails
         * @return rpcOutput output of service interface getConnectivityServiceDetails
         */
        @Override
        public RpcOutput getConnectivityServiceDetails(RpcInput inputVar) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);

        }


        /**
         * Service interface of updateConnectivityService.
         *
         * @param inputVar input of service interface updateConnectivityService
         * @return rpcOutput output of service interface updateConnectivityService
         */
        @Override
        public RpcOutput updateConnectivityService(RpcInput inputVar) {
            return new RpcOutput(RpcOutput.Status.RPC_FAILURE, null);

        }


        private ResourceData createResourceData(DataNode dataNode, ResourceId resId) {
            return DefaultResourceData.builder()
                .addDataNode(dataNode)
                .resourceId(resId)
                .build();
        }

        /**
         * Returns model objects of the store.
         *
         * @param dataNode data node from store
         * @param resId    parent resource id
         * @return model objects
         */
        private List<ModelObject> getModelObjects(DataNode dataNode, ResourceId resId) {
            ResourceData data = createResourceData(dataNode, resId);
            ModelObjectData modelData = modelConverter.createModel(data);
            return modelData.modelObjects();
        }



    }
}
