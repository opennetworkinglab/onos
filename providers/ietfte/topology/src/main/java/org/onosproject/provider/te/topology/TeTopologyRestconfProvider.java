/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.provider.te.topology;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.onosproject.provider.te.utils.TeTopologyRestconfEventType.TE_TOPOLOGY_LINK_NOTIFICATION;
import static org.onosproject.provider.te.utils.TeTopologyRestconfEventType.TE_TOPOLOGY_NODE_NOTIFICATION;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.device.DeviceProviderRegistry;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.protocol.rest.RestSBDevice;
import org.onosproject.protocol.restconf.RestConfSBController;
import org.onosproject.provider.te.utils.DefaultJsonCodec;
import org.onosproject.provider.te.utils.RestconfNotificationEventProcessor;
import org.onosproject.provider.te.utils.TeTopologyRestconfEventListener;
import org.onosproject.provider.te.utils.YangCompositeEncodingImpl;
import org.onosproject.tetopology.management.api.TeTopologyProvider;
import org.onosproject.tetopology.management.api.TeTopologyProviderRegistry;
import org.onosproject.tetopology.management.api.TeTopologyProviderService;
import org.onosproject.tetopology.management.api.TeTopologyService;
import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.node.NetworkNode;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.teyang.utils.topology.LinkConverter;
import org.onosproject.teyang.utils.topology.NetworkConverter;
import org.onosproject.teyang.utils.topology.NodeConverter;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.IetfNetwork;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.networks.Network;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208.IetfNetworkTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.IetfTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.IetfTeTopologyEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.TeNodeEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.tetopologyeventtype.TeTopologyEventTypeEnum;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ych.YangResourceIdentifierType;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;

/**
 * Provider for IETF TE Topology that use RESTCONF as means of communication.
 */
@Component(immediate = true)
public class TeTopologyRestconfProvider extends AbstractProvider
        implements TeTopologyProvider {
    private static final String APP_NAME = "org.onosproject.teprovider";
    private static final String RESTCONF = "restconf";
    private static final String PROVIDER =
            "org.onosproject.teprovider.restconf.domain";
    private static final String IETF_NETWORK_URI = "ietf-network:networks";
    private static final String IETF_NETWORKS_PREFIX =
            "{\"ietf-network:networks\":";
    private static final String TE_LINK_EVENT_PREFIX =
            "{\"ietf-te-topology:te-link-event\":";
    private static final String TE_NODE_EVENT_PREFIX =
            "{\"ietf-te-topology:te-node-event\":";
    private static final String IETF_NOTIFICATION_URI = "netconf";
    private static final String JSON = "json";
    private static final String E_DEVICE_NULL = "Restconf device is null";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceProviderRegistry deviceProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyProviderRegistry topologyProviderRegistry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestConfSBController restconfClient;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected YmsService ymsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TeTopologyService teTopologyService;

    private YangCodecHandler codecHandler;

    private TeTopologyProviderService topologyProviderService;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(5, groupedThreads("onos/restconfsbprovider",
                                                           "device-installer-%d", log));

    private final ConfigFactory<ApplicationId, RestconfServerConfig> factory =
            new ConfigFactory<ApplicationId, RestconfServerConfig>(APP_SUBJECT_FACTORY,
                                                                   RestconfServerConfig.class,
                                                                   "restconfDevices",
                                                                   true) {
                @Override
                public RestconfServerConfig createConfig() {
                    return new RestconfServerConfig();
                }
            };

    private final NetworkConfigListener cfgLister = new InternalNetworkConfigListener();
    private ApplicationId appId;

    private Set<DeviceId> addedDevices = new HashSet<>();

    @Activate
    public void activate() {
        // Get the codec handler.
        codecHandler = ymsService.getYangCodecHandler();
        // Register all three IETF Topology YANG model schema with YMS.
        codecHandler.addDeviceSchema(IetfNetwork.class);
        codecHandler.addDeviceSchema(IetfNetworkTopology.class);
        codecHandler.addDeviceSchema(IetfTeTopology.class);
        // Register JSON CODEC functions
        codecHandler.registerOverriddenCodec(new DefaultJsonCodec(ymsService),
                                             YangProtocolEncodingFormat.JSON);

        appId = coreService.registerApplication(APP_NAME);
        topologyProviderService = topologyProviderRegistry.register(this);
        cfgService.registerConfigFactory(factory);
        cfgService.addListener(cfgLister);
        executor.execute(TeTopologyRestconfProvider.this::connectDevices);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.removeListener(cfgLister);
        restconfClient.getDevices().keySet().forEach(this::deviceRemoved);
        topologyProviderRegistry.unregister(this);
        cfgService.unregisterConfigFactory(factory);
        log.info("Stopped");
    }

    /**
     * Creates an instance of TeTopologyRestconf provider.
     */
    public TeTopologyRestconfProvider() {
        super(new ProviderId(RESTCONF, PROVIDER));
    }

    private void deviceAdded(RestSBDevice nodeId) {
        Preconditions.checkNotNull(nodeId, E_DEVICE_NULL);
        nodeId.setActive(true);
        addedDevices.add(nodeId.deviceId());
    }

    private void deviceRemoved(DeviceId deviceId) {
        Preconditions.checkNotNull(deviceId, E_DEVICE_NULL);
        restconfClient.removeDevice(deviceId);
    }

    private void connectDevices() {

        RestconfServerConfig cfg = cfgService.getConfig(appId,
                                                        RestconfServerConfig.class);
        try {
            if (cfg != null && cfg.getDevicesAddresses() != null) {
                //Precomputing the devices to be removed
                Set<RestSBDevice> toBeRemoved = new HashSet<>(restconfClient.
                        getDevices().values());
                toBeRemoved.removeAll(cfg.getDevicesAddresses());
                //Adding new devices
                for (RestSBDevice device : cfg.getDevicesAddresses()) {
                    device.setActive(false);
                    restconfClient.addDevice(device);
                    deviceAdded(device);
                }

                //Removing devices not wanted anymore
                toBeRemoved.forEach(device -> deviceRemoved(device.deviceId()));
            }
        } catch (ConfigException e) {
            log.error("Configuration error {}", e);
        }

        // Discover the topology from RESTCONF server
        addedDevices.forEach(this::retrieveTopology);
        addedDevices.clear();
    }

    private void retrieveTopology(DeviceId deviceId) {
        // Retrieve IETF Network at top level.
        InputStream jsonStream = restconfClient.get(deviceId,
                                                    IETF_NETWORK_URI,
                                                    JSON);
        if (jsonStream == null) {
            log.warn("Unable to retrieve network Topology from restconf " +
                             "server {}", deviceId);
            return;
        }

        // Need to convert Input stream to String.
        StringWriter writer = new StringWriter();
        try {
            IOUtils.copy(jsonStream, writer, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("There is an exception {} for copy jsonStream to " +
                             "stringWriter for restconf {}",
                     e.getMessage(), deviceId);
            return;
        }
        String jsonString = writer.toString();
        String networkLevelJsonString = removePrefixTagFromJson(jsonString,
                                                                IETF_NETWORKS_PREFIX);

        YangCompositeEncodingImpl yce =
                new YangCompositeEncodingImpl(YangResourceIdentifierType.URI,
                                              IETF_NETWORK_URI,
                                              networkLevelJsonString);

        Object yo = codecHandler.decode(yce,
                                        YangProtocolEncodingFormat.JSON,
                                        YmsOperationType.QUERY_REPLY);

        if (yo == null) {
            log.error("YMS decoder returns null for restconf {}", deviceId);
            return;
        }

        // YMS returns an ArrayList in a single Object (i.e. yo in this case)
        // this means yo is actually an ArrayList of size 1
        IetfNetwork ietfNetwork = ((List<IetfNetwork>) yo).get(0);

        if (ietfNetwork.networks() != null &&
                ietfNetwork.networks().network() != null) {
            //Convert the YO to TE Core data and update TE Core.
            for (Network nw : ietfNetwork.networks().network()) {
                topologyProviderService.networkUpdated(
                        NetworkConverter.yang2TeSubsystemNetwork(nw, ietfNetwork.networks(), deviceId));
            }
        }

        //TODO: Uncomment when YMS fixes the issue in NetworkState translation (network-ref)
//        org.onosproject.tetopology.management.api.Networks networks =
//                NetworkConverter.yang2TeSubsystemNetworks(ietfNetwork.networks(),
//                                                          ietfNetwork.networksState());
//        if (networks == null || networks.networks() == null) {
//            log.error("Yang2Te returns null for restconf {}", deviceId);
//            return;
//        }
//        for (org.onosproject.tetopology.management.api.Network network : networks.networks()) {
//            topologyProviderService.networkUpdated(network);
//        }

        subscribeRestconfNotification(deviceId);
    }

    private void subscribeRestconfNotification(DeviceId deviceId) {

        TeTopologyRestconfEventListener listener =
                new TeTopologyRestconfEventListener();

        listener.addCallbackFunction(TE_TOPOLOGY_LINK_NOTIFICATION,
                                     new InternalLinkEventProcessor());
        listener.addCallbackFunction(TE_TOPOLOGY_NODE_NOTIFICATION,
                                     new InternalNodeEventProcessor());

        if (!restconfClient.isNotificationEnabled(deviceId)) {
            restconfClient.enableNotifications(deviceId,
                                               IETF_NOTIFICATION_URI,
                                               "application/json",
                                               listener);
        } else {
            restconfClient.addNotificationListener(deviceId, listener);
        }
    }

    private String removePrefixTagFromJson(String jsonString, String prefixTag) {
        if (jsonString.startsWith(prefixTag)) {
            return jsonString.substring(prefixTag.length(), jsonString.length() - 1);
        }
        return jsonString;
    }

    private class InternalLinkEventProcessor implements
            RestconfNotificationEventProcessor<String> {

        @Override
        public void processEventPayload(String payload) {
            String linkString = removePrefixTagFromJson(payload,
                                                        TE_LINK_EVENT_PREFIX);
            log.debug("link event={}", linkString);
            handleRestconfLinkNotification(linkString);
        }

        private void handleRestconfLinkNotification(String linkString) {

            IetfTeTopologyEvent event = convertJson2IetfTeTopologyEvent(
                    "ietf-te-topology:te-link-event",
                    linkString);
            if (event == null) {
                log.error("ERROR: json to YO conversion failure");
                return;
            }

            if (event.type() != IetfTeTopologyEvent.Type.TE_LINK_EVENT) {
                log.error("ERROR: wrong YO event type: {}", event.type());
                return;
            }

            TeLinkEvent teLinkEvent = event.subject().teLinkEvent();

            log.debug("TeLinkEvent: {}", teLinkEvent);

            NetworkLinkKey linkKey = LinkConverter.yangLinkEvent2NetworkLinkKey(
                    teLinkEvent);

            TeTopologyEventTypeEnum teLinkEventType = teLinkEvent.eventType()
                    .enumeration();

            if (teLinkEventType == TeTopologyEventTypeEnum.REMOVE) {
                topologyProviderService.linkRemoved(linkKey);
                return;
            }

            NetworkLink networkLink = LinkConverter.yangLinkEvent2NetworkLink(teLinkEvent,
                                                                              teTopologyService);

            if (networkLink == null) {
                log.error("ERROR: yangLinkEvent2NetworkLink returns null");
                return;
            }

            log.debug("networkLink: {}", networkLink);

            topologyProviderService.linkUpdated(linkKey, networkLink);
        }
    }

    private class InternalNodeEventProcessor implements
            RestconfNotificationEventProcessor<String> {

        @Override
        public void processEventPayload(String payload) {
            String nodeString = removePrefixTagFromJson(payload, TE_NODE_EVENT_PREFIX);
            log.debug("node event={}", nodeString);
            handleRestconfNodeNotification(nodeString);
        }

        private void handleRestconfNodeNotification(String nodeString) {

            IetfTeTopologyEvent event = convertJson2IetfTeTopologyEvent(
                    "ietf-te-topology:te-node-event",
                    nodeString);

            if (event == null) {
                log.error("ERROR: json to YO conversion failure");
                return;
            }

            if (event.type() != IetfTeTopologyEvent.Type.TE_NODE_EVENT) {
                log.error("ERROR: wrong YO event type: {}", event.type());
                return;
            }

            TeNodeEvent teNodeEvent = event.subject().teNodeEvent();

            log.debug("TeNodeEvent: {}", teNodeEvent);

            NetworkNodeKey nodeKey = NodeConverter.yangNodeEvent2NetworkNodeKey(
                    teNodeEvent);

            TeTopologyEventTypeEnum teNodeEventType = teNodeEvent.eventType()
                    .enumeration();

            if (teNodeEventType == TeTopologyEventTypeEnum.REMOVE) {
                topologyProviderService.nodeRemoved(nodeKey);
                return;
            }

            NetworkNode networkNode = NodeConverter.yangNodeEvent2NetworkNode(
                    teNodeEvent,
                    teTopologyService);

            if (networkNode == null) {
                log.error("ERROR: yangNodeEvent2NetworkNode returns null");
                return;
            }

            topologyProviderService.nodeUpdated(nodeKey, networkNode);
        }
    }

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            executor.execute(TeTopologyRestconfProvider.this::connectDevices);
        }

        @Override
        public boolean isRelevant(NetworkConfigEvent event) {
            return event.configClass().equals(RestconfServerConfig.class) &&
                    (event.type() == CONFIG_ADDED ||
                            event.type() == CONFIG_UPDATED);
        }
    }

    private IetfTeTopologyEvent convertJson2IetfTeTopologyEvent(String uriString,
                                                                String jsonBody) {

        YangCompositeEncodingImpl yce =
                new YangCompositeEncodingImpl(YangResourceIdentifierType.URI,
                                              uriString,
                                              jsonBody);
        Object yo = codecHandler.decode(yce,
                                        YangProtocolEncodingFormat.JSON,
                                        YmsOperationType.NOTIFICATION);

        if (yo == null) {
            log.error("YMS decoder error");
            return null;
        }

        if (!(yo instanceof IetfTeTopologyEvent)) {
            log.error("ERROR: YO is not IetfTeTopologyEvent");
            return null;
        }

        return (IetfTeTopologyEvent) yo;
    }
}
