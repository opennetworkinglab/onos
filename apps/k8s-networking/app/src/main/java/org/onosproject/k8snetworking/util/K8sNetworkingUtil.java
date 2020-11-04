/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.k8snetworking.util;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.k8snetworking.api.DefaultK8sPort;
import org.onosproject.k8snetworking.api.K8sNamespaceService;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkAdminService;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snetworking.api.K8sPodService;
import org.onosproject.k8snetworking.api.K8sPort;
import org.onosproject.k8snetworking.api.K8sServiceService;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.k8snode.api.K8sHost;
import org.onosproject.k8snode.api.K8sHostService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.k8snode.api.K8sNodeService;
import org.onosproject.k8snode.api.K8sRouterBridge;
import org.onosproject.k8snode.api.K8sTunnelBridge;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.k8snetworking.api.Constants.DEFAULT_NAMESPACE_HASH;
import static org.onosproject.k8snetworking.api.Constants.NORMAL_PORT_NAME_PREFIX_CONTAINER;
import static org.onosproject.k8snetworking.api.Constants.NORMAL_PORT_PREFIX_LENGTH;
import static org.onosproject.k8snetworking.api.Constants.PT_PORT_NAME_PREFIX_CONTAINER;
import static org.onosproject.k8snetworking.api.Constants.PT_PORT_PREFIX_LENGTH;
import static org.onosproject.k8snetworking.api.K8sPort.State.INACTIVE;
import static org.onosproject.k8snode.api.K8sApiConfig.Mode.PASSTHROUGH;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;

/**
 * An utility that used in kubernetes networking app.
 */
public final class K8sNetworkingUtil {

    private static final Logger log = LoggerFactory.getLogger(K8sNetworkingUtil.class);

    private static final String COLON_SLASH = "://";
    private static final String COLON = ":";

    private static final String STR_ZERO = "0";
    private static final String STR_ONE = "1";
    private static final String STR_PADDING = "0000000000000000";
    private static final int MASK_BEGIN_IDX = 0;
    private static final int MASK_MAX_IDX = 16;
    private static final int MASK_RADIX = 2;
    private static final int PORT_RADIX = 16;

    private static final String PORT_ID = "portId";
    private static final String DEVICE_ID = "deviceId";
    private static final String PORT_NUMBER = "portNumber";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String NETWORK_ID = "networkId";

    private K8sNetworkingUtil() {
    }

    /**
     * Checks that whether the port is associated with container interface.
     *
     * @param portName      port name
     * @return true if the port is associated with container; false otherwise
     */
    public static boolean isContainer(String portName) {
        return portName != null && (portName.contains(NORMAL_PORT_NAME_PREFIX_CONTAINER) ||
                portName.contains(PT_PORT_NAME_PREFIX_CONTAINER));
    }

    /**
     * Checks that whether the compared ports exist in the source name.
     *
     * @param sourceName    source port name
     * @param comparedName  port name to be compared
     * @return true if the compared port name exists, false otherwise
     */
    public static boolean existingContainerPortByName(String sourceName, String comparedName) {
        if (comparedName == null) {
            return false;
        }

        if (comparedName.contains(NORMAL_PORT_NAME_PREFIX_CONTAINER)) {
            return sourceName.contains(comparedName.substring(NORMAL_PORT_PREFIX_LENGTH));
        }

        if (comparedName.contains(PT_PORT_NAME_PREFIX_CONTAINER)) {
            return sourceName.contains(comparedName.substring(PT_PORT_PREFIX_LENGTH));
        }

        return false;
    }

    /**
     * Checks that whether the compared ports exist in the source MAC address.
     *
     * @param sourceMac     source port MAC address
     * @param comparedMac   MAC address of port to be compared
     * @return true if the compared port MAC address exists, false otherwise
     */
    public static boolean existingContainerPortByMac(String sourceMac, String comparedMac) {
        if (comparedMac == null || sourceMac == null) {
            return false;
        }

        String shortSourceMac = sourceMac.substring(3).toUpperCase();
        String shortComparedMac = comparedMac.substring(3).toUpperCase();

        return shortSourceMac.equals(shortComparedMac);
    }

    /**
     * Returns the tunnel port number with specified net ID and kubernetes node.
     *
     * @param netId         network ID
     * @param netService    network service
     * @param node          kubernetes node
     * @return tunnel port number
     */
    public static PortNumber tunnelPortNumByNetId(String netId,
                                                  K8sNetworkService netService,
                                                  K8sNode node) {
        K8sNetwork.Type netType = netService.network(netId).type();

        if (netType == null) {
            return null;
        }

        return tunnelPortNumByNetType(netType, node);
    }

    /**
     * Returns the tunnel port number with specified net type and kubernetes node.
     *
     * @param netType       network type
     * @param node          kubernetes node
     * @return tunnel port number
     */
    public static PortNumber tunnelPortNumByNetType(K8sNetwork.Type netType,
                                                    K8sNode node) {
        if (node.mode() == PASSTHROUGH) {
            K8sHostService hostService =
                    DefaultServiceDirectory.getService(K8sHostService.class);
            Port port = null;
            for (K8sHost host : hostService.hosts()) {
                if (host.nodeNames().contains(node.hostname())) {
                    for (K8sTunnelBridge bridge : host.tunBridges()) {
                        if (bridge.tunnelId() == node.segmentId()) {
                            String portName = netType.name().toLowerCase() +
                                    "-" + node.segmentId();
                            port = port(bridge.deviceId(), portName);
                        }
                    }
                }
            }

            if (port == null) {
                return null;
            } else {
                return port.number();
            }

        } else {
            switch (netType) {
                case VXLAN:
                    return node.vxlanPortNum();
                case GRE:
                    return node.grePortNum();
                case GENEVE:
                    return node.genevePortNum();
                default:
                    return null;
            }
        }
    }

    /**
     * Obtains the port from the device with the given port name.
     *
     * @param deviceId      device identifier
     * @param portName      port name
     * @return port object
     */
    public static Port port(DeviceId deviceId, String portName) {
        DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
        return deviceService.getPorts(deviceId).stream()
                .filter(p -> p.isEnabled() &&
                        Objects.equals(p.annotations().value(PORT_NAME), portName))
                .findAny().orElse(null);
    }

    /**
     * Obtains the property value with specified property key name.
     *
     * @param properties    a collection of properties
     * @param name          key name
     * @return mapping value
     */
    public static String getPropertyValue(Set<ConfigProperty> properties,
                                          String name) {
        Optional<ConfigProperty> property =
                properties.stream().filter(p -> p.name().equals(name)).findFirst();
        return property.map(ConfigProperty::value).orElse(null);
    }

    /**
     * Prints out the JSON string in pretty format.
     *
     * @param mapper        Object mapper
     * @param jsonString    JSON string
     * @return pretty formatted JSON string
     */
    public static String prettyJson(ObjectMapper mapper, String jsonString) {
        try {
            Object jsonObject = mapper.readValue(jsonString, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
        } catch (JsonParseException e) {
            log.debug("JsonParseException", e);
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException", e);
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException", e);
        }
        return null;
    }

    /**
     * Obtains valid IP addresses of the given subnet.
     *
     * @param cidr CIDR
     * @return set of IP addresses
     */
    public static Set<IpAddress> getSubnetIps(String cidr) {
        SubnetUtils utils = new SubnetUtils(cidr);
        utils.setInclusiveHostCount(false);
        SubnetUtils.SubnetInfo info = utils.getInfo();
        Set<String> allAddresses =
                new HashSet<>(Arrays.asList(info.getAllAddresses()));

        if (allAddresses.size() > 2) {
            allAddresses.remove(info.getLowAddress());
            allAddresses.remove(info.getHighAddress());
        }

        return allAddresses.stream()
                .map(IpAddress::valueOf).collect(Collectors.toSet());
    }

    /**
     * Obtains gateway IP address of the given subnet.
     *
     * @param cidr CIDR
     * @return gateway IP address
     */
    public static IpAddress getGatewayIp(String cidr) {
        SubnetUtils utils = new SubnetUtils(cidr);
        utils.setInclusiveHostCount(false);
        SubnetUtils.SubnetInfo info = utils.getInfo();
        return IpAddress.valueOf(info.getLowAddress());
    }

    /**
     * Obtains flow group key from the given id.
     *
     * @param groupId flow group identifier
     * @return flow group key
     */
    public static GroupKey getGroupKey(int groupId) {
        return new DefaultGroupKey((Integer.toString(groupId)).getBytes());
    }

    /**
     * Generates endpoint URL by referring to scheme, ipAddress and port.
     *
     * @param scheme        scheme
     * @param ipAddress     IP address
     * @param port          port number
     * @return generated endpoint URL
     */
    public static String endpoint(K8sApiConfig.Scheme scheme, IpAddress ipAddress, int port) {
        StringBuilder endpoint = new StringBuilder();
        String protocol = StringUtils.lowerCase(scheme.name());

        endpoint.append(protocol);
        endpoint.append(COLON_SLASH);
        endpoint.append(ipAddress.toString());
        endpoint.append(COLON);
        endpoint.append(port);

        return endpoint.toString();
    }

    /**
     * Generates endpoint URL by referring to scheme, ipAddress and port.
     *
     * @param apiConfig     kubernetes API config
     * @return generated endpoint URL
     */
    public static String endpoint(K8sApiConfig apiConfig) {
        return endpoint(apiConfig.scheme(), apiConfig.ipAddress(), apiConfig.port());
    }

    /**
     * Obtains workable kubernetes client.
     *
     * @param config kubernetes API config
     * @return kubernetes client
     */
    public static KubernetesClient k8sClient(K8sApiConfig config) {
        if (config == null) {
            log.warn("Kubernetes API server config is empty.");
            return null;
        }

        String endpoint = endpoint(config);

        ConfigBuilder configBuilder = new ConfigBuilder().withMasterUrl(endpoint);

        if (config.scheme() == K8sApiConfig.Scheme.HTTPS) {
            configBuilder.withTrustCerts(true)
                    .withOauthToken(config.token())
                    .withCaCertData(config.caCertData())
                    .withClientCertData(config.clientCertData())
                    .withClientKeyData(config.clientKeyData());
        }

        return new DefaultKubernetesClient(configBuilder.build());
    }

    /**
     * Obtains workable kubernetes client.
     *
     * @param service kubernetes API service
     * @return kubernetes client
     */
    public static KubernetesClient k8sClient(K8sApiConfigService service) {
        K8sApiConfig config =
                service.apiConfigs().stream().findAny().orElse(null);
        if (config == null) {
            log.error("Failed to find valid kubernetes API configuration.");
            return null;
        }

        KubernetesClient client = k8sClient(config);

        if (client == null) {
            log.error("Failed to connect to kubernetes API server.");
            return null;
        }

        return client;
    }

    /**
     * Obtains the kubernetes node IP and kubernetes network gateway IP map.
     *
     * @param nodeService       kubernetes node service
     * @param networkService    kubernetes network service
     * @return kubernetes node IP and kubernetes network gateway IP map
     */
    public static Map<String, String> nodeIpGatewayIpMap(K8sNodeService nodeService,
                                                         K8sNetworkService networkService) {
        Map<String, String> ipMap = Maps.newConcurrentMap();

        nodeService.completeNodes().forEach(n -> {
            K8sNetwork network = networkService.network(n.hostname());
            if (network != null) {
                ipMap.put(n.nodeIp().toString(), network.gatewayIp().toString());
            }
        });

        return ipMap;
    }

    /**
     * Returns a shifted IP address.
     *
     * @param ipAddress     IP address to be shifted
     * @param shiftPrefix   A IP prefix used in shifted IP address
     * @return shifted Ip address
     */
    public static String shiftIpDomain(String ipAddress, String shiftPrefix) {
        String origIpPrefix = ipAddress.split("\\.")[0] + "." + ipAddress.split("\\.")[1];
        return StringUtils.replace(ipAddress, origIpPrefix, shiftPrefix);
    }

    /**
     * Returns an unshifted IP address.
     *
     * @param ipAddress     IP address to be unshifted
     * @param ipPrefix      IP prefix which to be used for unshifting
     * @param cidr          a POD network CIDR
     * @return unshifted IP address
     */
    public static String unshiftIpDomain(String ipAddress,
                                         String ipPrefix,
                                         String cidr) {

        String origIpPrefix = cidr.split("\\.")[0] + "." + cidr.split("\\.")[1];
        return StringUtils.replace(ipAddress, ipPrefix, origIpPrefix);
    }

    /**
     * Returns the B class IP prefix of the given CIDR.
     *
     * @param cidr  CIDR
     * @return IP prefix
     */
    public static String getBclassIpPrefixFromCidr(String cidr) {
        if (cidr == null) {
            return null;
        }
        return cidr.split("\\.")[0] + "." + cidr.split("\\.")[1];
    }

    /**
     * Returns the A class IP prefix of the given CIDR.
     *
     * @param cidr  CIDR
     * @return IP prefix
     */
    public static String getAclassIpPrefixFromCidr(String cidr) {
        if (cidr == null) {
            return null;
        }
        return cidr.split("\\.")[0];
    }

    /**
     * Returns the map of port range.
     *
     * @param portMin minimum port number
     * @param portMax maximum port number
     * @return map of port range
     */
    public static Map<TpPort, TpPort> buildPortRangeMatches(int portMin, int portMax) {

        boolean processing = true;
        int start = portMin;
        Map<TpPort, TpPort> portMaskMap = Maps.newHashMap();
        while (processing) {
            String minStr = Integer.toBinaryString(start);
            String binStrMinPadded = STR_PADDING.substring(minStr.length()) + minStr;

            int mask = testMasks(binStrMinPadded, start, portMax);
            int maskStart = binLower(binStrMinPadded, mask);
            int maskEnd = binHigher(binStrMinPadded, mask);

            log.debug("start : {} port/mask = {} / {} ", start, getMask(mask), maskStart);
            portMaskMap.put(TpPort.tpPort(maskStart), TpPort.tpPort(
                    Integer.parseInt(Objects.requireNonNull(getMask(mask)), PORT_RADIX)));

            start = maskEnd + 1;
            if (start > portMax) {
                processing = false;
            }
        }

        return portMaskMap;
    }

    /**
     * Returns the namespace hash value by given POD IP.
     *
     * @param k8sPodService         kubernetes POD service
     * @param k8sNamespaceService   kubernetes namespace service
     * @param podIp                 POD IP address
     * @return namespace hash value
     */
    public static Integer namespaceHashByPodIp(K8sPodService k8sPodService,
                                               K8sNamespaceService k8sNamespaceService,
                                               String podIp) {
        String ns = k8sPodService.pods().stream()
                .filter(pod -> pod.getStatus().getPodIP() != null)
                .filter(pod -> pod.getStatus().getPodIP().equals(podIp))
                .map(pod -> pod.getMetadata().getNamespace())
                .findAny().orElse(null);

        if (ns != null) {
            return k8sNamespaceService.namespaces().stream()
                    .filter(n -> n.getMetadata().getName().equals(ns))
                    .map(Namespace::hashCode).findAny().orElse(null);
        } else {
            return null;
        }
    }

    /**
     * Returns the namespace hash value by given service IP.
     *
     * @param k8sServiceService     kubernetes service service
     * @param k8sNamespaceService   kubernetes namespace service
     * @param serviceIp             service IP address
     * @return namespace hash value
     */
    public static int namespaceHashByServiceIp(K8sServiceService k8sServiceService,
                                                   K8sNamespaceService k8sNamespaceService,
                                                   String serviceIp) {
        String ns = k8sServiceService.services().stream()
                .filter(service -> service.getSpec().getClusterIP() != null)
                .filter(service -> service.getSpec().getClusterIP().equalsIgnoreCase(serviceIp))
                .map(service -> service.getMetadata().getNamespace())
                .findAny().orElse(null);

        if (ns != null) {
            return namespaceHashByNamespace(k8sNamespaceService, ns);
        } else {
            return DEFAULT_NAMESPACE_HASH;
        }
    }

    /**
     * Returns the namespace hash value by given namespace name.
     *
     * @param k8sNamespaceService   kubernetes namespace service
     * @param ns                    namespace name
     * @return namespace hash value
     */
    public static int namespaceHashByNamespace(K8sNamespaceService k8sNamespaceService,
                                               String ns) {

        return k8sNamespaceService.namespaces().stream()
                .filter(n -> n.getMetadata().getName() != null)
                .filter(n -> n.getMetadata().getName().equalsIgnoreCase(ns))
                .map(Namespace::hashCode).findAny().orElse(DEFAULT_NAMESPACE_HASH);
    }

    /**
     * Returns POD instance by POD IP address.
     *
     * @param podService    kubernetes POD service
     * @param podIp         POD IP address
     * @return POD instance
     */
    public static Pod podByIp(K8sPodService podService, String podIp) {
        return podService.pods().stream()
                .filter(pod -> pod.getStatus().getPodIP() != null)
                .filter(pod -> pod.getStatus().getPodIP().equals(podIp))
                .findAny().orElse(null);
    }

    /**
     * Returns the container port number by given container port name.
     *
     * @param pod           kubernetes POD
     * @param portName      port name
     * @return container port number,
     *         return 0 if there is no port number mapped with the given port name
     */
    public static int portNumberByName(Pod pod, String portName) {

        if (pod == null || pod.getSpec() == null) {
            return 0;
        }

        for (Container container : pod.getSpec().getContainers()) {
            for (ContainerPort cp : container.getPorts()) {
                if (cp.getName() != null && cp.getName().equals(portName)) {
                    return cp.getContainerPort();
                }
            }
        }

        return 0;
    }

    /**
     * Synchronizes port from kubernetes POD.
     *
     * @param pod               kubernetes POD
     * @param adminService      admin service
     */
    public static void syncPortFromPod(Pod pod, K8sNetworkAdminService adminService) {
        Map<String, String> annotations = pod.getMetadata().getAnnotations();
        if (annotations != null && !annotations.isEmpty() &&
                annotations.get(PORT_ID) != null) {
            String portId = annotations.get(PORT_ID);

            K8sPort oldPort = adminService.port(portId);

            String networkId = annotations.get(NETWORK_ID);
            DeviceId deviceId = DeviceId.deviceId(annotations.get(DEVICE_ID));
            PortNumber portNumber = PortNumber.portNumber(annotations.get(PORT_NUMBER));
            IpAddress ipAddress = IpAddress.valueOf(annotations.get(IP_ADDRESS));
            MacAddress macAddress = MacAddress.valueOf(annotations.get(MAC_ADDRESS));

            K8sPort newPort = DefaultK8sPort.builder()
                    .portId(portId)
                    .networkId(networkId)
                    .deviceId(deviceId)
                    .ipAddress(ipAddress)
                    .macAddress(macAddress)
                    .portNumber(portNumber)
                    .state(INACTIVE)
                    .build();

            if (oldPort == null) {
                adminService.createPort(newPort);
            } else {
                adminService.updatePort(newPort);
            }
        }
    }

    /**
     * Generates string format based on the given string length list.
     *
     * @param stringLengths a list of string lengths
     * @return string format (e.g., %-28s%-15s%-24s%-20s%-15s)
     */
    public static String genFormatString(List<Integer> stringLengths) {
        StringBuilder fsb = new StringBuilder();
        stringLengths.forEach(length -> {
            fsb.append("%-");
            fsb.append(length);
            fsb.append("s");
        });
        return fsb.toString();
    }

    /**
     * Returns all device identifiers belong to kubernetes nodes and hosts.
     *
     * @param nodeService   node service
     * @param hostService   host service
     * @return all device identifiers belong to kubernetes nodes and hosts
     */
    public static Set<DeviceId> allK8sDevices(K8sNodeService nodeService,
                                              K8sHostService hostService) {
        Set<DeviceId> allDevIds = new HashSet<>();

        Set<DeviceId> intgDevIds = nodeService.completeNodes().stream()
                .map(K8sNode::intgBridge).collect(Collectors.toSet());
        Set<DeviceId> extDevIds = nodeService.completeNodes().stream()
                .map(K8sNode::extBridge).collect(Collectors.toSet());
        Set<DeviceId> tunDevIds = nodeService.completeNodes().stream()
                .map(K8sNode::tunBridge).collect(Collectors.toSet());
        Set<DeviceId> localDevIds = nodeService.completeNodes().stream()
                .map(K8sNode::localBridge).collect(Collectors.toSet());

        Set<DeviceId> hostTunDevIds = new HashSet<>();
        Set<DeviceId> hostRouterDevIds = new HashSet<>();

        for (K8sHost host : hostService.completeHosts()) {
            Set<K8sTunnelBridge> hostTunBrs = host.tunBridges();
            Set<K8sRouterBridge> hostRouterBrs = host.routerBridges();
            hostTunDevIds.addAll(hostTunBrs.stream().map(K8sTunnelBridge::deviceId)
                    .collect(Collectors.toSet()));
            hostRouterDevIds.addAll(hostRouterBrs.stream().map(K8sRouterBridge::deviceId)
                    .collect(Collectors.toSet()));
        }

        allDevIds.addAll(intgDevIds);
        allDevIds.addAll(extDevIds);
        allDevIds.addAll(tunDevIds);
        allDevIds.addAll(localDevIds);
        allDevIds.addAll(hostTunDevIds);
        allDevIds.addAll(hostRouterDevIds);

        return allDevIds;
    }

    private static int binLower(String binStr, int bits) {
        StringBuilder outBin = new StringBuilder(
                binStr.substring(MASK_BEGIN_IDX, MASK_MAX_IDX - bits));
        for (int i = 0; i < bits; i++) {
            outBin.append(STR_ZERO);
        }

        return Integer.parseInt(outBin.toString(), MASK_RADIX);
    }

    private static int binHigher(String binStr, int bits) {
        StringBuilder outBin = new StringBuilder(
                binStr.substring(MASK_BEGIN_IDX, MASK_MAX_IDX - bits));
        for (int i = 0; i < bits; i++) {
            outBin.append(STR_ONE);
        }

        return Integer.parseInt(outBin.toString(), MASK_RADIX);
    }

    private static int testMasks(String binStr, int start, int end) {
        int mask = MASK_BEGIN_IDX;
        for (; mask <= MASK_MAX_IDX; mask++) {
            int maskStart = binLower(binStr, mask);
            int maskEnd = binHigher(binStr, mask);
            if (maskStart < start || maskEnd > end) {
                return mask - 1;
            }
        }

        return mask;
    }

    private static String getMask(int bits) {
        switch (bits) {
            case 0:  return "ffff";
            case 1:  return "fffe";
            case 2:  return "fffc";
            case 3:  return "fff8";
            case 4:  return "fff0";
            case 5:  return "ffe0";
            case 6:  return "ffc0";
            case 7:  return "ff80";
            case 8:  return "ff00";
            case 9:  return "fe00";
            case 10: return "fc00";
            case 11: return "f800";
            case 12: return "f000";
            case 13: return "e000";
            case 14: return "c000";
            case 15: return "8000";
            case 16: return "0000";
            default: return null;
        }
    }
}
