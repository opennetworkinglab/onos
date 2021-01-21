/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.kubevirtnetworking.api.DefaultKubevirtPort;
import org.onosproject.kubevirtnetworking.api.KubevirtNetwork;
import org.onosproject.kubevirtnetworking.api.KubevirtPort;
import org.onosproject.kubevirtnode.api.KubevirtApiConfig;
import org.onosproject.kubevirtnode.api.KubevirtApiConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An utility that used in KubeVirt networking app.
 */
public final class KubevirtNetworkingUtil {

    private static final Logger log = LoggerFactory.getLogger(KubevirtNetworkingUtil.class);

    private static final int PORT_NAME_MAX_LENGTH = 15;
    private static final String COLON_SLASH = "://";
    private static final String COLON = ":";

    private static final String NETWORK_STATUS_KEY = "k8s.v1.cni.cncf.io/network-status";
    private static final String NAME = "name";
    private static final String NETWORK_PREFIX = "default/";
    private static final String MAC = "mac";
    private static final String IPS = "ips";

    /**
     * Prevents object installation from external.
     */
    private KubevirtNetworkingUtil() {
    }

    /**
     * Obtains the boolean property value with specified property key name.
     *
     * @param properties    a collection of properties
     * @param name          key name
     * @return mapping value
     */
    public static boolean getPropertyValueAsBoolean(Set<ConfigProperty> properties,
                                                    String name) {
        Optional<ConfigProperty> property =
                properties.stream().filter(p -> p.name().equals(name)).findFirst();

        return property.map(ConfigProperty::asBoolean).orElse(false);
    }

    /**
     * Re-structures the OVS port name.
     * The length of OVS port name should be not large than 15.
     *
     * @param portName  original port name
     * @return re-structured OVS port name
     */
    public static String structurePortName(String portName) {

        // The size of OVS port name should not be larger than 15
        if (portName.length() > PORT_NAME_MAX_LENGTH) {
            return StringUtils.substring(portName, 0, PORT_NAME_MAX_LENGTH);
        }

        return portName;
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
        } catch (IOException e) {
            log.debug("Json string parsing exception caused by {}", e);
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
     * Calculate the broadcast address from given IP address and subnet prefix length.
     *
     * @param ipAddr        IP address
     * @param prefixLength  subnet prefix length
     * @return broadcast address
     */
    public static String getBroadcastAddr(String ipAddr, int prefixLength) {
        String subnet = ipAddr + "/" + prefixLength;
        SubnetUtils utils = new SubnetUtils(subnet);
        return utils.getInfo().getBroadcastAddress();
    }
    /**
     * Generates endpoint URL by referring to scheme, ipAddress and port.
     *
     * @param scheme        scheme
     * @param ipAddress     IP address
     * @param port          port number
     * @return generated endpoint URL
     */
    public static String endpoint(KubevirtApiConfig.Scheme scheme, IpAddress ipAddress, int port) {
        StringBuilder endpoint = new StringBuilder();
        String protocol = org.apache.commons.lang3.StringUtils.lowerCase(scheme.name());

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
    public static String endpoint(KubevirtApiConfig apiConfig) {
        return endpoint(apiConfig.scheme(), apiConfig.ipAddress(), apiConfig.port());
    }

    /**
     * Obtains workable kubernetes client.
     *
     * @param config kubernetes API config
     * @return kubernetes client
     */
    public static KubernetesClient k8sClient(KubevirtApiConfig config) {
        if (config == null) {
            log.warn("Kubernetes API server config is empty.");
            return null;
        }

        String endpoint = endpoint(config);

        ConfigBuilder configBuilder = new ConfigBuilder().withMasterUrl(endpoint);

        if (config.scheme() == KubevirtApiConfig.Scheme.HTTPS) {
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
    public static KubernetesClient k8sClient(KubevirtApiConfigService service) {
        KubevirtApiConfig config = service.apiConfig();
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
     * Obtains the kubevirt port from kubevirt POD.
     *
     * @param networks set of existing kubevirt networks
     * @param pod kubevirt POD
     * @return kubevirt port
     */
    public static KubevirtPort getPort(Set<KubevirtNetwork> networks, Pod pod) {
        try {
            Map<String, String> annots = pod.getMetadata().getAnnotations();
            if (annots == null) {
                return null;
            }

            String networkStatusStr = annots.get(NETWORK_STATUS_KEY);

            if (networkStatusStr == null) {
                return null;
            }

            JSONArray networkStatus = new JSONArray(networkStatusStr);

            for (int i = 0; i < networkStatus.length(); i++) {
                JSONObject object = networkStatus.getJSONObject(i);
                String name = object.getString(NAME);
                KubevirtNetwork network = networks.stream()
                        .filter(n -> (NETWORK_PREFIX + n.name()).equals(name))
                        .findAny().orElse(null);
                if (network != null) {
                    String mac = object.getString(MAC);

                    KubevirtPort.Builder builder = DefaultKubevirtPort.builder()
                            .macAddress(MacAddress.valueOf(mac))
                            .networkId(network.networkId());

                    if (object.has(IPS)) {
                        JSONArray ips = object.getJSONArray(IPS);
                        String ip = (String) ips.get(0);
                        builder.ipAddress(IpAddress.valueOf(ip));
                    }

                    return builder.build();
                }
            }

        } catch (JSONException e) {
            log.error("Failed to parse network status object", e);
        }

        return null;
    }
}
