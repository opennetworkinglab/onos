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
package org.onosproject.cluster.impl;

import static java.net.NetworkInterface.getNetworkInterfaces;
import static org.slf4j.LoggerFactory.getLogger;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterMetadata;
import org.onosproject.cluster.ClusterMetadataProvider;
import org.onosproject.cluster.ClusterMetadataProviderRegistry;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

/**
 * Provider of default {@link ClusterMetadata cluster metadata}.
 */
@Component(immediate = true)
public class DefaultClusterMetadataProvider implements ClusterMetadataProvider {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterMetadataProviderRegistry providerRegistry;

    private static final String ONOS_IP = "ONOS_IP";
    private static final String ONOS_INTERFACE = "ONOS_INTERFACE";
    private static final String ONOS_ALLOW_IPV6 = "ONOS_ALLOW_IPV6";
    private static final String DEFAULT_ONOS_INTERFACE = "eth0";
    private static final int DEFAULT_ONOS_PORT = 9876;
    private static final ProviderId PROVIDER_ID = new ProviderId("default", "none");
    private final AtomicReference<Versioned<ClusterMetadata>> cachedMetadata = new AtomicReference<>();

    @Activate
    public void activate() {
        String localIp = getSiteLocalAddress();
        ControllerNode localNode =
                new DefaultControllerNode(new NodeId(localIp), IpAddress.valueOf(localIp), DEFAULT_ONOS_PORT);
        // partition 1
        Partition partition = new DefaultPartition(PartitionId.from(1), ImmutableSet.of(localNode.id()));
        ClusterMetadata metadata = new ClusterMetadata(PROVIDER_ID,
                                        "default",
                                        ImmutableSet.of(localNode),
                                        ImmutableSet.of(partition));
        long version = System.currentTimeMillis();
        cachedMetadata.set(new Versioned<>(metadata, version));
        providerRegistry.register(this);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        providerRegistry.unregister(this);
        log.info("Stopped");
    }

    @Override
    public ProviderId id() {
        return PROVIDER_ID;
    }

    @Override
    public Versioned<ClusterMetadata> getClusterMetadata() {
        return cachedMetadata.get();
    }

    @Override
    public void setClusterMetadata(ClusterMetadata metadata) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addActivePartitionMember(PartitionId partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeActivePartitionMember(PartitionId partitionId, NodeId nodeId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<NodeId> getActivePartitionMembers(PartitionId partitionId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    private static String getSiteLocalAddress() {

        /*
         * If the IP ONOS should use is set via the environment variable we will assume it is valid and should be used.
         * Setting the IP address takes presidence over setting the interface via the environment.
         */
        String useOnosIp = System.getenv(ONOS_IP);
        if (useOnosIp != null) {
            return useOnosIp;
        }

        // Read environment variables for IP interface information or set to default
        String useOnosInterface = System.getenv(ONOS_INTERFACE);
        if (useOnosInterface == null) {
            useOnosInterface = DEFAULT_ONOS_INTERFACE;
        }

        // Capture if they want to limit IP address selection to only IPv4 (default).
        boolean allowIPv6 = (System.getenv(ONOS_ALLOW_IPV6) != null);

        Function<NetworkInterface, IpAddress> ipLookup = nif -> {
            IpAddress fallback = null;

            // nif can be null if the interface name specified doesn't exist on the node's host
            if (nif != null) {
                for (InetAddress address : Collections.list(nif.getInetAddresses())) {
                    if (address.isSiteLocalAddress() && (allowIPv6 || address instanceof Inet4Address)) {
                        return IpAddress.valueOf(address);
                    }
                    if (fallback == null && !address.isLoopbackAddress() && !address.isMulticastAddress()
                        && (allowIPv6 || address instanceof Inet4Address)) {
                        fallback = IpAddress.valueOf(address);
                    }
                }
            }
            return fallback;
        };
        try {
            IpAddress ip = ipLookup.apply(NetworkInterface.getByName(useOnosInterface));
            if (ip != null) {
                return ip.toString();
            }
            for (NetworkInterface nif : Collections.list(getNetworkInterfaces())) {
                if (!nif.getName().equals(useOnosInterface)) {
                    ip = ipLookup.apply(nif);
                    if (ip != null) {
                        return ip.toString();
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to get network interfaces", e);
        }

        return IpAddress.valueOf(InetAddress.getLoopbackAddress()).toString();
    }
}