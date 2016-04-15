/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.collect.ImmutableSet;
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
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.SubjectFactories;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;
import org.onosproject.routing.config.LocalIpPrefixEntry;
import org.onosproject.routing.config.ReactiveRoutingConfig;
import org.onosproject.routing.config.RouterConfig;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.routing.RouteEntry.createBinaryString;

/**
 * Implementation of RoutingConfigurationService which reads routing
 * configuration from the network configuration service.
 */
@Component(immediate = true)
@Service
public class RoutingConfigurationImpl implements RoutingConfigurationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService configService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private Set<IpAddress> gatewayIpAddresses = new HashSet<>();
    private Set<ConnectPoint> bgpPeerConnectPoints = new HashSet<>();

    private InvertedRadixTree<LocalIpPrefixEntry>
            localPrefixTable4 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());
    private InvertedRadixTree<LocalIpPrefixEntry>
            localPrefixTable6 = new ConcurrentInvertedRadixTree<>(
                    new DefaultByteArrayNodeFactory());

    private MacAddress virtualGatewayMacAddress;
    private final InternalNetworkConfigListener configListener =
            new InternalNetworkConfigListener();

    private ConfigFactory<ApplicationId, BgpConfig> bgpConfigFactory =
            new ConfigFactory<ApplicationId, BgpConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, BgpConfig.class, "bgp") {
        @Override
        public BgpConfig createConfig() {
            return new BgpConfig();
        }
    };

    private ConfigFactory<ApplicationId, RouterConfig> routerConfigFactory =
            new ConfigFactory<ApplicationId, RouterConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY, RouterConfig.class, "router") {
        @Override
        public RouterConfig createConfig() {
            return new RouterConfig();
        }
    };

    private ConfigFactory<ApplicationId, ReactiveRoutingConfig>
            reactiveRoutingConfigFactory =
            new ConfigFactory<ApplicationId, ReactiveRoutingConfig>(
                    SubjectFactories.APP_SUBJECT_FACTORY,
                    ReactiveRoutingConfig.class, "reactiveRouting") {
        @Override
        public ReactiveRoutingConfig createConfig() {
            return new ReactiveRoutingConfig();
        }
    };

    @Activate
    public void activate() {
        configService.addListener(configListener);
        registry.registerConfigFactory(bgpConfigFactory);
        registry.registerConfigFactory(routerConfigFactory);
        registry.registerConfigFactory(reactiveRoutingConfigFactory);
        setUpConfiguration();
        log.info("Routing configuration service started");
    }

    @Deactivate
    public void deactivate() {
        registry.unregisterConfigFactory(bgpConfigFactory);
        registry.unregisterConfigFactory(routerConfigFactory);
        registry.unregisterConfigFactory(reactiveRoutingConfigFactory);
        configService.removeListener(configListener);
        log.info("Routing configuration service stopped");
    }

    /**
     * Set up reactive routing information from configuration.
     */
    private void setUpConfiguration() {
        ReactiveRoutingConfig config = configService.getConfig(
                coreService.registerApplication(RoutingConfigurationService
                        .REACTIVE_ROUTING_APP_ID),
                RoutingConfigurationService.CONFIG_CLASS);
        if (config == null) {
            log.warn("No reactive routing config available!");
            return;
        }
        for (LocalIpPrefixEntry entry : config.localIp4PrefixEntries()) {
            localPrefixTable4.put(createBinaryString(entry.ipPrefix()), entry);
            gatewayIpAddresses.add(entry.getGatewayIpAddress());
        }
        for (LocalIpPrefixEntry entry : config.localIp6PrefixEntries()) {
            localPrefixTable6.put(createBinaryString(entry.ipPrefix()), entry);
            gatewayIpAddresses.add(entry.getGatewayIpAddress());
        }

        virtualGatewayMacAddress = config.virtualGatewayMacAddress();

        // Setup BGP peer connect points
        ApplicationId routerAppId = coreService.getAppId(RoutingService.ROUTER_APP_ID);
        if (routerAppId == null) {
            log.info("Router application ID is null!");
            return;
        }

        BgpConfig bgpConfig = configService.getConfig(routerAppId, BgpConfig.class);

        if (bgpConfig == null) {
            log.info("BGP config is null!");
            return;
        } else {
            bgpPeerConnectPoints =
                    bgpConfig.bgpSpeakers().stream()
                    .flatMap(speaker -> speaker.peers().stream())
                    .map(peer -> interfaceService.getMatchingInterface(peer))
                    .filter(Objects::nonNull)
                    .map(intf -> intf.connectPoint())
                    .collect(Collectors.toSet());
        }
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

    private class InternalNetworkConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            switch (event.type()) {
            case CONFIG_REGISTERED:
                break;
            case CONFIG_UNREGISTERED:
                break;
            case CONFIG_ADDED:
            case CONFIG_UPDATED:
            case CONFIG_REMOVED:
                if (event.configClass() == RoutingConfigurationService.CONFIG_CLASS) {
                    setUpConfiguration();
                }
                break;
            default:
                break;
            }
        }
    }

    @Override
    public Set<ConnectPoint> getBgpPeerConnectPoints() {
        return ImmutableSet.copyOf(bgpPeerConnectPoints);
    }

}
