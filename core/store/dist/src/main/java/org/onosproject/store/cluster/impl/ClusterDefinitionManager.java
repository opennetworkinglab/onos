/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.cluster.impl;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterDefinitionService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.consistent.impl.DatabaseDefinition;
import org.onosproject.store.consistent.impl.DatabaseDefinitionStore;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Set;
import java.util.stream.Collectors;

import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Collections.list;
import static org.onosproject.cluster.DefaultControllerNode.DEFAULT_PORT;
import static org.onosproject.store.consistent.impl.DatabaseManager.PARTITION_DEFINITION_FILE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of ClusterDefinitionService.
 */
@Component(immediate = true)
@Service
public class ClusterDefinitionManager implements ClusterDefinitionService {

    public static final String CLUSTER_DEFINITION_FILE = "../config/cluster.json";
    private static final String ONOS_NIC = "ONOS_NIC";
    private static final Logger log = getLogger(ClusterDefinitionManager.class);
    private ControllerNode localNode;
    private Set<ControllerNode> seedNodes;

    @Activate
    public void activate() {
        File clusterDefinitionFile = new File(CLUSTER_DEFINITION_FILE);
        ClusterDefinitionStore clusterDefinitionStore =
                new ClusterDefinitionStore(clusterDefinitionFile.getPath());

        if (!clusterDefinitionFile.exists()) {
            createDefaultClusterDefinition(clusterDefinitionStore);
        }

        try {
            ClusterDefinition clusterDefinition = clusterDefinitionStore.read();
            establishSelfIdentity(clusterDefinition);
            seedNodes = ImmutableSet
                    .copyOf(clusterDefinition.getNodes())
                    .stream()
                    .filter(n -> !localNode.id().equals(new NodeId(n.getId())))
                    .map(n -> new DefaultControllerNode(new NodeId(n.getId()),
                                                        IpAddress.valueOf(n.getIp()),
                                                        n.getTcpPort()))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read cluster definition.", e);
        }

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public ControllerNode localNode() {
        return localNode;
    }

    @Override
    public Set<ControllerNode> seedNodes() {
        return seedNodes;
    }

    @Override
    public void formCluster(Set<ControllerNode> nodes, String ipPrefix) {
        try {
            Set<NodeInfo> infos = Sets.newHashSet();
            nodes.forEach(n -> infos.add(NodeInfo.from(n.id().toString(),
                                                       n.ip().toString(),
                                                       n.tcpPort())));

            ClusterDefinition cdef = ClusterDefinition.from(infos, ipPrefix);
            new ClusterDefinitionStore(CLUSTER_DEFINITION_FILE).write(cdef);

            DatabaseDefinition ddef = DatabaseDefinition.from(infos);
            new DatabaseDefinitionStore(PARTITION_DEFINITION_FILE).write(ddef);
        } catch (IOException e) {
            log.error("Unable to form cluster", e);
        }
    }

    private IpAddress findLocalIp(ClusterDefinition clusterDefinition) throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface iface = interfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = iface.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                IpAddress ip = IpAddress.valueOf(inetAddresses.nextElement());
                if (clusterDefinition.getNodes().stream()
                        .map(NodeInfo::getIp)
                        .map(IpAddress::valueOf)
                        .anyMatch(nodeIp -> ip.equals(nodeIp))) {
                    return ip;
                }
            }
        }
        throw new IllegalStateException("Unable to determine local ip");
    }

    private void establishSelfIdentity(ClusterDefinition clusterDefinition) {
        try {
            IpAddress ip = findLocalIp(clusterDefinition);
            localNode = new DefaultControllerNode(new NodeId(ip.toString()), ip);
        } catch (SocketException e) {
            throw new IllegalStateException("Cannot determine local IP", e);
        }
    }

    private void createDefaultClusterDefinition(ClusterDefinitionStore store) {
        // Assumes IPv4 is returned.
        String ip = getSiteLocalAddress();
        String ipPrefix = ip.replaceFirst("\\.[0-9]*$", ".*");
        NodeInfo node = NodeInfo.from(ip, ip, DEFAULT_PORT);
        try {
            store.write(ClusterDefinition.from(ImmutableSet.of(node), ipPrefix));
        } catch (IOException e) {
            log.warn("Unable to write default cluster definition", e);
        }
    }

    /**
     * Returns the address that matches the IP prefix given in ONOS_NIC
     * environment variable if one was specified, or the first site local
     * address if one can be found or the loopback address otherwise.
     *
     * @return site-local address in string form
     */
    public static String getSiteLocalAddress() {
        try {
            String ipPrefix = System.getenv(ONOS_NIC);
            for (NetworkInterface nif : list(getNetworkInterfaces())) {
                for (InetAddress address : list(nif.getInetAddresses())) {
                    IpAddress ip = IpAddress.valueOf(address);
                    if (ipPrefix == null && address.isSiteLocalAddress() ||
                            ipPrefix != null && matchInterface(ip.toString(), ipPrefix)) {
                        return ip.toString();
                    }
                }
            }
        } catch (SocketException e) {
            log.error("Unable to get network interfaces", e);
        }

        return IpAddress.valueOf(InetAddress.getLoopbackAddress()).toString();
    }

    // Indicates whether the specified interface address matches the given prefix.
    // FIXME: Add a facility to IpPrefix to make this more robust
    private static boolean matchInterface(String ip, String ipPrefix) {
        String s = ipPrefix.replaceAll("\\.\\*", "");
        return ip.startsWith(s);
    }
}
