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
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ConfigProperty;
import org.onosproject.k8snetworking.api.K8sNetwork;
import org.onosproject.k8snetworking.api.K8sNetworkService;
import org.onosproject.k8snode.api.K8sApiConfig;
import org.onosproject.k8snode.api.K8sApiConfigService;
import org.onosproject.k8snode.api.K8sNode;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.k8snetworking.api.Constants.PORT_NAME_PREFIX_CONTAINER;

/**
 * An utility that used in kubernetes networking app.
 */
public final class K8sNetworkingUtil {

    private static final Logger log = LoggerFactory.getLogger(K8sNetworkingUtil.class);

    private static final String COLON_SLASH = "://";
    private static final String COLON = ":";

    private K8sNetworkingUtil() {
    }

    /**
     * Checks that whether the port is associated with container interface.
     *
     * @param portName      port name
     * @return true if the port is associated with container; false otherwise
     */
    public static boolean isContainer(String portName) {
        return portName != null && portName.contains(PORT_NAME_PREFIX_CONTAINER);
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
            log.debug("JsonParseException caused by {}", e);
        } catch (JsonMappingException e) {
            log.debug("JsonMappingException caused by {}", e);
        } catch (JsonProcessingException e) {
            log.debug("JsonProcessingException caused by {}", e);
        } catch (IOException e) {
            log.debug("IOException caused by {}", e);
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
        utils.setInclusiveHostCount(true);
        SubnetUtils.SubnetInfo info = utils.getInfo();
        Set<String> allAddresses =
                new HashSet<>(Arrays.asList(info.getAllAddresses()));

        if (allAddresses.size() > 2) {
            allAddresses.remove(info.getBroadcastAddress());
            allAddresses.remove(info.getNetworkAddress());
        }

        return allAddresses.stream()
                .map(IpAddress::valueOf).collect(Collectors.toSet());
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
}
