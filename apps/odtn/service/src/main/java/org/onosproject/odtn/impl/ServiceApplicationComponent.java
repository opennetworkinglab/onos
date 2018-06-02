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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;
import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.TapiTopologyManager;
import org.onosproject.odtn.config.TerminalDeviceConfig;
import org.onosproject.odtn.internal.DcsBasedTapiCommonRpc;
import org.onosproject.odtn.internal.DcsBasedTapiConnectivityRpc;
import org.onosproject.odtn.internal.DcsBasedTapiDataProducer;
import org.onosproject.odtn.internal.TapiDataProducer;
import org.onosproject.odtn.internal.DefaultOdtnTerminalDeviceDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// DCS / onos-yang-tools
import org.onosproject.config.DynamicConfigEvent;
import org.onosproject.config.DynamicConfigListener;
import org.onosproject.config.DynamicConfigService;
import org.onosproject.yang.model.RpcRegistry;

import static org.onosproject.config.DynamicConfigEvent.Type.NODE_ADDED;
import static org.onosproject.config.DynamicConfigEvent.Type.NODE_DELETED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REMOVED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;

/**
 * OSGi Component for ODTN Service application.
 */
@Component(immediate = true)
public class ServiceApplicationComponent {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DynamicConfigService dynConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService netcfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RpcRegistry rpcRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TapiTopologyManager tapiTopologyManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TapiResolver resolver;

    // Listener for events from the DCS
    private final DynamicConfigListener dynamicConfigServiceListener =
            new InternalDynamicConfigListener();

    private DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final NetworkConfigListener netcfgListener = new InternalNetCfgListener();
    private TapiDataProducer dataProvider = new DcsBasedTapiDataProducer();

    // Rpc Service for TAPI Connectivity
    private final DcsBasedTapiConnectivityRpc rpcTapiConnectivity = new DcsBasedTapiConnectivityRpc();
    private final DcsBasedTapiCommonRpc rpcTapiCommon = new DcsBasedTapiCommonRpc();

    // FIXME create factory and register for all behaviours
    private final ConfigFactory<ConnectPoint, TerminalDeviceConfig> factory =
            new ConfigFactory<ConnectPoint, TerminalDeviceConfig>(CONNECT_POINT_SUBJECT_FACTORY,
                    TerminalDeviceConfig.class, TerminalDeviceConfig.CONFIG_KEY) {
                @Override
                public TerminalDeviceConfig createConfig() {
                    return new TerminalDeviceConfig();
                }
            };


    @Activate
    protected void activate() {
        log.info("Started");
        dynConfigService.addListener(dynamicConfigServiceListener);
        deviceService.addListener(deviceListener);
        linkService.addListener(linkListener);
        netcfgService.addListener(netcfgListener);
        netcfgRegistry.registerConfigFactory(factory);
        rpcRegistry.registerRpcService(rpcTapiConnectivity);
        rpcRegistry.registerRpcService(rpcTapiCommon);

        rpcTapiConnectivity.init();
        rpcTapiCommon.init();
    }


    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
        rpcRegistry.unregisterRpcService(rpcTapiCommon);
        rpcRegistry.unregisterRpcService(rpcTapiConnectivity);
        netcfgRegistry.unregisterConfigFactory(factory);
        netcfgService.removeListener(netcfgListener);
        linkService.removeListener(linkListener);
        deviceService.removeListener(deviceListener);
        dynConfigService.removeListener(dynamicConfigServiceListener);
    }


    /**
     * Representation of internal listener, listening for device event.
     */
    private class InternalDeviceListener implements DeviceListener {

        /**
         * Process an Event from the Device Service.
         *
         * @param event device event
         */
        @Override
        public void event(DeviceEvent event) {

            log.info("Device event type: {}", event.type());
            log.info("Device event subject: {}", event.subject());
            switch (event.type()) {
                case DEVICE_ADDED:
                    tapiTopologyManager.addDevice(event.subject());
                    break;
                case DEVICE_REMOVED:
                    tapiTopologyManager.removeDevice(event.subject());
                    break;
                case PORT_ADDED:
                    tapiTopologyManager.addPort(event.port());
                    break;
                case PORT_REMOVED:
                    tapiTopologyManager.removePort(event.port());
                    break;
                default:
                    log.warn("Unknown Event", event.type());
                    break;
            }

        }
    }

    /**
     * Representation of internal listener, listening for link event.
     */
    private class InternalLinkListener implements LinkListener {

        /**
         * Process an Event from the Device Service.
         *
         * @param event link event
         */
        @Override
        public void event(LinkEvent event) {
            Link link = event.subject();

            switch (event.type()) {
                case LINK_ADDED:
                    tapiTopologyManager.addLink(link);
                    break;
                case LINK_REMOVED:
                    tapiTopologyManager.removeLink(link);
                    break;
                default:
                    log.warn("Unknown Event", event.type());
                    break;
            }
        }
    }

    /**
     * Representation of internal listener, listening for netcfg event.
     */
    private class InternalNetCfgListener implements NetworkConfigListener {

        /**
         * Check if the netcfg event should be further processed.
         *
         * @param event config event
         * @return true if event is supported; false otherwise
         */
        @Override
        public boolean isRelevant(NetworkConfigEvent event) {

            if (event.type() == CONFIG_ADDED || event.type() == CONFIG_UPDATED) {
                if (event.config().orElse(null) instanceof TerminalDeviceConfig) {
                    return true;
                }
            }
            if (event.type() == CONFIG_REMOVED) {
                if (event.prevConfig().orElse(null) instanceof TerminalDeviceConfig) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Process an Event from the NetCfg Service.
         *
         * @param event event
         */
        @Override
        public void event(NetworkConfigEvent event) {

            log.info("type: {}", event.type());
            log.info("subject: {}", event.subject());
            DeviceId did = ((ConnectPoint) event.subject()).deviceId();

            DefaultOdtnTerminalDeviceDriver driver = DefaultOdtnTerminalDeviceDriver.create();
            TerminalDeviceConfig config;

            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    config = (TerminalDeviceConfig) event.config().get();
                    log.info("config: {}", config);
                    driver.apply(did, config.clientCp().port(), config.subject().port(), config.isEnabled());
                    break;
                case CONFIG_REMOVED:
                    config = (TerminalDeviceConfig) event.prevConfig().get();
                    log.info("config: {}", config);
                    driver.apply(did, config.clientCp().port(), config.subject().port(), false);
                    break;
                default:
                    log.error("Unsupported event type.");
            }
        }
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
            resolver.makeDirty();
//            ResourceId rsId = event.subject();
//            DataNode node;
//            try {
//                Filter filter = Filter.builder().addCriteria(rsId).build();
//                node = dynConfigService.readNode(rsId, filter);
//            } catch (FailedException e) {
//                node = null;
//            }
//            switch (event.type()) {
//                case NODE_ADDED:
//                    onDcsNodeAdded(rsId, node);
//                    break;
//
//                case NODE_DELETED:
//                    onDcsNodeDeleted(node);
//                    break;
//
//                default:
//                    log.warn("Unknown Event", event.type());
//                    break;
//            }
        }

//        /**
//         * Process the event that a node has been added to the DCS.
//         *
//         * @param rsId ResourceId of the added node
//         * @param node added node. Access the key and value
//         */
//        private void onDcsNodeAdded(ResourceId rsId, DataNode node) {
//
//            switch (node.type()) {
//                case SINGLE_INSTANCE_NODE:
//                    break;
//                case MULTI_INSTANCE_NODE:
//                    break;
//                case SINGLE_INSTANCE_LEAF_VALUE_NODE:
//                    break;
//                case MULTI_INSTANCE_LEAF_VALUE_NODE:
//                    break;
//                default:
//                    break;
//            }
//
//            NodeKey dataNodeKey = node.key();
//            SchemaId schemaId = dataNodeKey.schemaId();

        // Consolidate events
//            if (!schemaId.namespace().contains("tapi")) {
//                return;
//            }
//            log.info("namespace {}", schemaId.namespace());
//        }

//        /**
//         * Process the event that a node has been deleted from the DCS.
//         *
//         * @param dataNode data node
//         */
//        private void onDcsNodeDeleted(DataNode dataNode) {
//            // TODO: Implement release logic
//        }

    }

}
