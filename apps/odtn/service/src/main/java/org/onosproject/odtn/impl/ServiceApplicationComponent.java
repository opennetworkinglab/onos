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

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.Port;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;

import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_REMOVED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.CONNECT_POINT_SUBJECT_FACTORY;


import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.link.LinkListener;
import org.onosproject.net.link.LinkService;

import org.onosproject.odtn.TapiResolver;
import org.onosproject.odtn.TapiConnectivityConfig;
import org.onosproject.odtn.TapiConnectivityService;
import org.onosproject.odtn.TapiTopologyManager;
import org.onosproject.odtn.config.TerminalDeviceConfig;
import org.onosproject.odtn.internal.DcsBasedTapiCommonRpc;
import org.onosproject.odtn.internal.DcsBasedTapiConnectivityRpc;
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import static org.onosproject.net.intent.IntentState.FAILED;
import static org.onosproject.net.intent.IntentState.WITHDRAWN;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.OduSignalType;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * OSGi Component for ODTN Service application.
 */
@Component(immediate = true, service = TapiConnectivityService.class)
public class ServiceApplicationComponent implements TapiConnectivityService  {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DynamicConfigService dynConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigService netcfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected NetworkConfigRegistry netcfgRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RpcRegistry rpcRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TapiTopologyManager tapiTopologyManager;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TapiResolver resolver;

    private static final int WITHDRAW_EVENT_TIMEOUT_SECONDS = 5;

    private static final String APP_ID = "org.onosproject.odtn-service";

    private ApplicationId appId;

    // Listener for events from the DCS
    private final DynamicConfigListener dynamicConfigServiceListener =
            new InternalDynamicConfigListener();

    private DeviceListener deviceListener = new InternalDeviceListener();
    private final LinkListener linkListener = new InternalLinkListener();
    private final NetworkConfigListener netcfgListener = new InternalNetCfgListener();

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
        appId = coreService.registerApplication(APP_ID);
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
     * Process TAPI Event from NBI.
     *
     * @param config TAPI Connectivity config for the event
     */
    public void processTapiEvent(TapiConnectivityConfig config) {
        checkNotNull(config, "Config can't be null");
        Key key = Key.of(config.uuid(), appId);
        // Setup the Intent
        if (config.isSetup()) {
            log.debug("TAPI config: {} to setup intent", config);
            Intent intent = createOpticalIntent(config.leftCp(), config.rightCp(), key, appId);
            intentService.submit(intent);
        } else {
        // Release the intent
            Intent intent = intentService.getIntent(key);
            if (intent == null) {
                log.error("Intent for uuid {} does not exist", config.uuid());
                return;
            }
            log.debug("TAPI config: {} to purge intent {}", config, intent);
            CountDownLatch latch = new CountDownLatch(1);
            IntentListener listener = new DeleteListener(key, latch);
            intentService.addListener(listener);
            try {
                /*
                 * RCAS: Note, withdraw is asynchronous. We cannot call purge
                 * directly, because at this point it remains in the "INSTALLED"
                 * state.
                 */
                intentService.withdraw(intent);

                /*
                 * org.onosproject.onos-core-net - 2.1.0.SNAPSHOT |
                 * Purge for intent 0x0 is rejected because intent state is INSTALLED
                 * intentService.purge(intent);
                 */
                try {
                    latch.await(WITHDRAW_EVENT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                // double check the state
                IntentState state = intentService.getIntentState(key);
                if (state == WITHDRAWN || state == FAILED) {
                    intentService.purge(intent);
                }
            } finally {
                intentService.removeListener(listener);
            }
        }
    }

    /**
     * Returns a new optical intent created from the method parameters.
     *
     * @param ingress ingress description (device/port)
     * @param egress egress description (device/port)
     * @param key intent key
     * @param appId application id. As per Intent class, it cannot be null
     *
     * @return created intent
     */
    protected Intent createOpticalIntent(ConnectPoint ingress, ConnectPoint egress,
                                         Key key, ApplicationId appId) {

        if (ingress == null || egress == null) {
            log.error("Invalid endpoint(s) for optical intent: ingress {}, egress {}",
                ingress, egress);
            return null;
        }
        DeviceService ds = opticalView(deviceService);
        Port srcPort = ds.getPort(ingress.deviceId(), ingress.port());
        Port dstPort = ds.getPort(egress.deviceId(), egress.port());
        if (srcPort == null || dstPort == null) {
            log.error("Invalid port(s) for optical intent: src {}, dst {}",
                srcPort, dstPort);
            return null;
        }

        OduSignalType signalType = ((OchPort) srcPort).signalType();
        return OpticalConnectivityIntent.builder()
            .appId(appId)
            .key(key)
            .src(ingress)
            .dst(egress)
            .signalType(signalType)
            .bidirectional(true) //TODO Revisit this.
            .build();
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

            log.debug("Device event type: {}, subject: {}", event.type(), event.subject());
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
                // TODO: Process device / port updated events
                default:
                    log.warn("Unprocessed Event {}", event.type());
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
                    log.warn("Unknown Event {}", event.type());
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
            log.debug("Event type: {}, subject: {}", event.type(), event.subject());
            DeviceId did = ((ConnectPoint) event.subject()).deviceId();
            TerminalDeviceConfig config = (TerminalDeviceConfig) event.config().get();
            DefaultOdtnTerminalDeviceDriver driver = DefaultOdtnTerminalDeviceDriver.create();
            log.debug("config: {}", config);
            switch (event.type()) {
                case CONFIG_ADDED:
                case CONFIG_UPDATED:
                    driver.apply(did, config.clientCp().port(), config.subject().port(), config.isEnabled());
                    break;

                case CONFIG_REMOVED:
                    driver.apply(did, config.clientCp().port(), config.subject().port(), false);
                    break;
                default:
                    log.error("Unsupported event type.");
            }
        }
    }


    /**
     * Internal listener for tracking the intent deletion events.
     */
    private class DeleteListener implements IntentListener {
        final Key key;
        final CountDownLatch latch;

        /**
         * Default constructor.
         *
         * @param key   key
         * @param latch count down latch
         */
        DeleteListener(Key key, CountDownLatch latch) {
            this.key = key;
            this.latch = latch;
        }

        @Override
        public void event(IntentEvent event) {
            if (Objects.equals(event.subject().key(), key) &&
                    (event.type() == IntentEvent.Type.WITHDRAWN ||
                            event.type() == IntentEvent.Type.FAILED)) {
                latch.countDown();
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
        }
    }

}
