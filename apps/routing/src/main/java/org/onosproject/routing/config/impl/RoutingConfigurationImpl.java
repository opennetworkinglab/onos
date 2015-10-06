/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.routing.config.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.concurrenttrees.radix.node.concrete.DefaultByteArrayNodeFactory;
import com.googlecode.concurrenttrees.radixinverted.ConcurrentInvertedRadixTree;
import com.googlecode.concurrenttrees.radixinverted.InvertedRadixTree;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.BgpPeer;
import org.onosproject.routing.config.BgpSpeaker;
import org.onosproject.routing.config.Interface;
import org.onosproject.routing.config.LocalIpPrefixEntry;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.onosproject.routing.impl.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.onosproject.routing.RouteEntry.createBinaryString;

/**
 * Implementation of RoutingConfigurationService which reads routing
 * configuration from a file.
 */
@Component(immediate = true)
@Service
public class RoutingConfigurationImpl implements RoutingConfigurationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CONFIG_DIR = "../config";
    private static final String DEFAULT_CONFIG_FILE = "sdnip.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private Map<String, BgpSpeaker> bgpSpeakers = new ConcurrentHashMap<>();
    private Map<IpAddress, BgpPeer> bgpPeers = new ConcurrentHashMap<>();
    private Set<IpAddress> gatewayIpAddresses = new HashSet<>();
    private Set<ConnectPoint> bgpPeerConnectPoints = new HashSet<>();

    private InvertedRadixTree<LocalIpPrefixEntry>
            localPrefixTable4 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<LocalIpPrefixEntry>
            localPrefixTable6 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());

    private MacAddress virtualGatewayMacAddress;

    private ConfigFactory configFactory =
            new ConfigFactory(SubjectFactories.APP_SUBJECT_FACTORY, BgpConfig.class, "bgp") {
        @Override
        public BgpConfig createConfig() {
            return new BgpConfig();
        }
    };

    @Activate
    public void activate() {
        registry.registerConfigFactory(configFactory);
        readConfiguration();
        log.info("Routing configuration service started");
    }

    @Deactivate
    public void deactivate() {
        registry.unregisterConfigFactory(configFactory);
        log.info("Routing configuration service stopped");
    }

    /**
     * Reads SDN-IP related information contained in the configuration file.
     *
     * @param configFilename the name of the configuration file for the SDN-IP
     * application
     */
    private void readConfiguration(String configFilename) {
        File configFile = new File(CONFIG_DIR, configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            log.info("Loading config: {}", configFile.getAbsolutePath());
            Configuration config = mapper.readValue(configFile,
                                                    Configuration.class);
            for (BgpSpeaker speaker : config.getBgpSpeakers()) {
                bgpSpeakers.put(speaker.name(), speaker);
            }
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.ipAddress(), peer);
                bgpPeerConnectPoints.add(peer.connectPoint());
            }

            for (LocalIpPrefixEntry entry : config.getLocalIp4PrefixEntries()) {
                localPrefixTable4.put(createBinaryString(entry.ipPrefix()),
                                      entry);
                gatewayIpAddresses.add(entry.getGatewayIpAddress());
            }
            for (LocalIpPrefixEntry entry : config.getLocalIp6PrefixEntries()) {
                localPrefixTable6.put(createBinaryString(entry.ipPrefix()),
                                      entry);
                gatewayIpAddresses.add(entry.getGatewayIpAddress());
            }

            virtualGatewayMacAddress = config.getVirtualGatewayMacAddress();

        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Error loading configuration", e);
        }
    }

    /**
     * Instructs the configuration reader to read the configuration from the
     * file.
     */
    public void readConfiguration() {
        readConfiguration(configFileName);
    }

    @Override
    public Map<String, BgpSpeaker> getBgpSpeakers() {
        return Collections.unmodifiableMap(bgpSpeakers);
    }

    @Override
    public Map<IpAddress, BgpPeer> getBgpPeers() {
        return Collections.unmodifiableMap(bgpPeers);
    }

    @Override
    public Set<Interface> getInterfaces() {
        return Collections.emptySet();
    }

    @Override
    public Set<ConnectPoint> getBgpPeerConnectPoints() {
        // TODO perhaps cache this result in future
        ApplicationId routerAppId = coreService.getAppId(Router.ROUTER_APP_ID);
        if (routerAppId == null) {
            return Collections.emptySet();
        }

        BgpConfig bgpConfig = configService.getConfig(routerAppId, BgpConfig.class);
        if (bgpConfig == null) {
            return Collections.emptySet();
        } else {
            return bgpConfig.bgpSpeakers().stream()
                    .flatMap(speaker -> speaker.peers().stream())
                    .map(peer -> interfaceService.getMatchingInterface(peer))
                    .filter(intf -> intf != null)
                    .map(intf -> intf.connectPoint())
                    .collect(Collectors.toSet());
        }
    }

    @Override
    public Interface getInterface(ConnectPoint connectPoint) {
        return null;
    }

    @Override
    public Interface getInterface(IpAddress ip) {
        return null;
    }

    @Override
    public Interface getMatchingInterface(IpAddress ipAddress) {
        return null;
    }

    @Override
    public boolean isIpAddressLocal(IpAddress ipAddress) {
        if (ipAddress.isIp4()) {
            return localPrefixTable4.getValuesForKeysPrefixing(
                    createBinaryString(
                    IpPrefix.valueOf(ipAddress, Ip4Address.BIT_LENGTH)))
                    .iterator().hasNext();
        } else {
            return localPrefixTable6.getValuesForKeysPrefixing(
                    createBinaryString(
                    IpPrefix.valueOf(ipAddress, Ip6Address.BIT_LENGTH)))
                    .iterator().hasNext();
        }
    }

    @Override
    public boolean isIpPrefixLocal(IpPrefix ipPrefix) {
        return (localPrefixTable4.getValueForExactKey(
                createBinaryString(ipPrefix)) != null ||
                localPrefixTable6.getValueForExactKey(
                createBinaryString(ipPrefix)) != null);
    }

    @Override
    public boolean isVirtualGatewayIpAddress(IpAddress ipAddress) {
        return gatewayIpAddresses.contains(ipAddress);
    }

    @Override
    public MacAddress getVirtualGatewayMacAddress() {
        return virtualGatewayMacAddress;
    }

}
