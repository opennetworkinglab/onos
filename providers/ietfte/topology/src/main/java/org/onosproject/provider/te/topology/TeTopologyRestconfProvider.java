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

import com.google.common.base.Preconditions;
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
import org.onosproject.protocol.restconf.RestConfNotificationEventListener;
import org.onosproject.protocol.restconf.RestConfSBController;
import org.onosproject.provider.te.utils.DefaultJsonCodec;
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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.IetfTeTopology;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeLinkEvent;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.TeNodeEvent;
import org.onosproject.yms.ych.YangCodecHandler;
import org.onosproject.yms.ych.YangProtocolEncodingFormat;
import org.onosproject.yms.ych.YangResourceIdentifierType;
import org.onosproject.yms.ydt.YmsOperationType;
import org.onosproject.yms.ymsm.YmsService;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_ADDED;
import static org.onosproject.net.config.NetworkConfigEvent.Type.CONFIG_UPDATED;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provider for IETF TE Topology that use RESTCONF as means of communication.
 */
@Component(immediate = true)
public class TeTopologyRestconfProvider extends AbstractProvider
        implements TeTopologyProvider {
    private static final String APP_NAME = "org.onosproject.teprovider.topology";
    private static final String RESTCONF = "restconf";
    private static final String PROVIDER =
            "org.onosproject.teprovider.restconf.domain";
    private static final String IETF_NETWORK_URI = "ietf-network:networks";
    private static final String IETF_NETWORKS_PREFIX =
            "{\"ietf-network:networks\":";
    private static final String TE_NOTIFICATION_PREFIX =
            "{\"ietf-te-topology:ietf-te-topology\":";
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

        if ((yo == null)) {
            log.error("YMS decoder returns {} for restconf {}", yo, deviceId);
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
                        NetworkConverter.yang2TeSubsystemNetwork(nw, ietfNetwork.networks()));
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

        RestConfNotificationEventListener<String> callBackListener =
                new InternalRestconfNotificationEventListener();
        restconfClient.enableNotifications(deviceId, IETF_NOTIFICATION_URI,
                                           "application/json",
                                           callBackListener);
    }

    private String removePrefixTagFromJson(String jsonString, String prefixTag) {
        if (jsonString.startsWith(prefixTag)) {
            return jsonString.substring(prefixTag.length(), jsonString.length() - 1);
        }
        return jsonString;
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

    private class InternalRestconfNotificationEventListener implements
            RestConfNotificationEventListener<String> {
        @Override
        public void handleNotificationEvent(DeviceId deviceId,
                                            String eventJsonString) {
            log.debug("New notification: {} for device: {}",
                      eventJsonString, deviceId.toString());

            String teEventString = removePrefixTagFromJson(eventJsonString,
                                                           TE_NOTIFICATION_PREFIX);
            if (teEventString.startsWith(TE_LINK_EVENT_PREFIX)) {
                String linkString = removePrefixTagFromJson(teEventString,
                                                            TE_LINK_EVENT_PREFIX);
                log.debug("link event={}", linkString);
                handleRestconfLinkNotification(linkString);
            } else if (teEventString.startsWith(TE_NODE_EVENT_PREFIX)) {
                String nodeString = removePrefixTagFromJson(teEventString,
                                                            TE_NODE_EVENT_PREFIX);
                log.debug("node event={}", nodeString);
                handleRestconfNodeNotification(nodeString);
            }
        }

        private void handleRestconfLinkNotification(String linkString) {
            YangCompositeEncodingImpl yce =
                    new YangCompositeEncodingImpl(YangResourceIdentifierType.URI,
                                                  "ietf-te-topology:te-link-event",
                                                  linkString);
            Object yo = codecHandler.decode(yce,
                                            YangProtocolEncodingFormat.JSON,
                                            YmsOperationType.NOTIFICATION);

            if (yo == null) {
                log.error("YMS decoder error");
                return;
            }

            NetworkLinkKey linkKey = LinkConverter.yangLinkEvent2NetworkLinkKey(
                    (TeLinkEvent) yo);

            NetworkLink networkLink = LinkConverter.yangLinkEvent2NetworkLink(
                    (TeLinkEvent) yo,
                    teTopologyService);

            topologyProviderService.linkUpdated(linkKey, networkLink);
        }

        private void handleRestconfNodeNotification(String nodeString) {
            YangCompositeEncodingImpl yce =
                    new YangCompositeEncodingImpl(YangResourceIdentifierType.URI,
                                                  "ietf-te-topology:te-node-event",
                                                  nodeString);
            Object yo = codecHandler.decode(yce,
                                            YangProtocolEncodingFormat.JSON,
                                            YmsOperationType.NOTIFICATION);

            if (yo == null) {
                log.error("YMS decoder error");
                return;
            }

            NetworkNodeKey nodeKey = NodeConverter.yangNodeEvent2NetworkNodeKey(
                    (TeNodeEvent) yo);

            NetworkNode networkNode = NodeConverter.yangNodeEvent2NetworkNode(
                    (TeNodeEvent) yo,
                    teTopologyService);

            topologyProviderService.nodeUpdated(nodeKey, networkNode);
        }
    }
}
