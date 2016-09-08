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
package org.onosproject.openstackinterface.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.glassfish.jersey.client.ClientProperties;
import org.onlab.packet.Ip4Address;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Port;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigEvent;
import org.onosproject.net.config.NetworkConfigListener;
import org.onosproject.net.config.NetworkConfigRegistry;
import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstackinterface.OpenstackInterfaceService;
import org.onosproject.openstackinterface.OpenstackNetwork;
import org.onosproject.openstackinterface.OpenstackInterfaceConfig;
import org.onosproject.openstackinterface.OpenstackPort;
import org.onosproject.openstackinterface.OpenstackRouter;
import org.onosproject.openstackinterface.OpenstackSecurityGroup;
import org.onosproject.openstackinterface.OpenstackSubnet;
import org.onosproject.openstackinterface.web.OpenstackFloatingIpCodec;
import org.onosproject.openstackinterface.web.OpenstackNetworkCodec;
import org.onosproject.openstackinterface.web.OpenstackPortCodec;
import org.onosproject.openstackinterface.web.OpenstackRouterCodec;
import org.onosproject.openstackinterface.web.OpenstackSecurityGroupCodec;
import org.onosproject.openstackinterface.web.OpenstackSubnetCodec;
import org.slf4j.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.config.basics.SubjectFactories.APP_SUBJECT_FACTORY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles REST Calls to Openstack Neutron.
 *
 */
@Service
@Component(immediate = true)
public class OpenstackInterfaceManager implements OpenstackInterfaceService {

    private static final String URI_NETWORKS = "networks";
    private static final String URI_PORTS = "ports";
    private static final String URI_SUBNETS = "subnets";
    private static final String URI_SECURITY_GROUPS = "security-groups";
    private static final String URI_FLOATINGIPS = "floatingips";
    private static final String URI_TOKENS = "tokens";
    private static final String FLOATINGIP = "floatingip";
    private static final String PORT_ID = "port_id";
    private static final String FIXED_IP_ADDRESS = "fixed_ip_address";

    private static final String PATH_ROUTERS = "routers";
    private static final String PATH_NETWORKS = "networks";
    private static final String PATH_PORTS = "ports";
    private static final String PATH_SUBNETS = "subnets";
    private static final String PATH_FLOATINGIPS = "floatingips";
    private static final String PATH_ACCESS = "access";
    private static final String PATH_TOKEN = "token";
    private static final String PATH_ID = "id";
    private static final String PATH_EXPIRES = "expires";

    private static final String HEADER_AUTH_TOKEN = "X-Auth-Token";
    private static final String TOKEN_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final int DEFAULT_TIMEOUT_MS = 2000;

    private final Logger log = getLogger(getClass());
    private final Client client = ClientBuilder.newClient();

    private String neutronUrl;
    private String keystoneUrl;
    private String tokenId;
    private String tokenExpires;
    private String userName;
    private String pass;

    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigRegistry cfgService;

    private InternalConfigListener internalConfigListener = new InternalConfigListener();
    private ExecutorService networkEventExcutorService =
            Executors.newSingleThreadExecutor(groupedThreads("onos/openstackinterface", "config-event", log));

    private final Set<ConfigFactory> factories = ImmutableSet.of(
            new ConfigFactory<ApplicationId, OpenstackInterfaceConfig>(APP_SUBJECT_FACTORY,
                    OpenstackInterfaceConfig.class,
                    "openstackinterface") {
                @Override
                public OpenstackInterfaceConfig createConfig() {
                    return new OpenstackInterfaceConfig();
                }
            }
    );

    @Activate
    protected void activate() {
        appId = coreService
                .registerApplication("org.onosproject.openstackinterface");

        factories.forEach(cfgService::registerConfigFactory);
        cfgService.addListener(internalConfigListener);

        client.property(ClientProperties.CONNECT_TIMEOUT, DEFAULT_TIMEOUT_MS);
        client.property(ClientProperties.READ_TIMEOUT, DEFAULT_TIMEOUT_MS);

        configureNetwork();
        log.info("started");
    }

    @Deactivate
    protected void deactivate() {
        cfgService.removeListener(internalConfigListener);
        factories.forEach(cfgService::unregisterConfigFactory);
        log.info("stopped");
    }

    /**
     * Returns network information stored in Neutron.
     *
     * @return List of OpenstackNetwork
     */
    public Collection<OpenstackNetwork> getNetworks() {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_NETWORKS);
        if (builder == null) {
            log.warn("Failed to get networks");
            return Collections.EMPTY_LIST;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);
        log.debug("networks response:" + response);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackNetwork> openstackNetworks = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode networkList = (ArrayNode) node.path(PATH_NETWORKS);
            OpenstackNetworkCodec networkCodec = new OpenstackNetworkCodec();
            networkList.forEach(n -> openstackNetworks.add(networkCodec.decode((ObjectNode) n, null)));
        } catch (IOException e) {
            log.warn("getNetworks()", e);
        }

        openstackNetworks.removeAll(Collections.singleton(null));
        openstackNetworks.forEach(n -> log.debug("network ID: {}", n.id()));

        return openstackNetworks;
    }

    /**
     * Returns port information stored in Neutron.
     *
     * @return List of OpenstackPort
     */
    public Collection<OpenstackPort> getPorts() {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_PORTS);
        if (builder == null) {
            log.warn("Failed to get ports");
            return Collections.EMPTY_LIST;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackPort> openstackPorts = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode portList = (ArrayNode) node.path(PATH_PORTS);
            OpenstackPortCodec portCodec = new OpenstackPortCodec();
            portList.forEach(p -> openstackPorts.add(portCodec.decode((ObjectNode) p, null)));
        } catch (IOException e) {
            log.warn("getPorts()", e);
        }

        log.debug("port response:" + response);
        openstackPorts.forEach(n -> log.debug("port ID: {}", n.id()));

        return openstackPorts;
    }

    public Collection<OpenstackRouter> getRouters() {
        Invocation.Builder builder = getClientBuilder(neutronUrl, PATH_ROUTERS);
        if (builder == null) {
            log.warn("Failed to get routers");
            return Collections.EMPTY_LIST;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackRouter> openstackRouters = Lists.newArrayList();

        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode routerList = (ArrayNode) node.path(PATH_ROUTERS);
            OpenstackRouterCodec openstackRouterCodec = new OpenstackRouterCodec();
            routerList.forEach(r -> openstackRouters
                    .add(openstackRouterCodec.decode((ObjectNode) r, null)));
        } catch (IOException e) {
            log.warn("getRouters()", e);
        }

        log.debug("router response:" + response);
        openstackRouters.forEach(r -> log.debug("router ID: {}", r.id()));

        return openstackRouters;
    }

    /**
     * Returns Subnet information in Neutron.
     *
     * @return List of OpenstackSubnet
     */
    public Collection<OpenstackSubnet> getSubnets() {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_SUBNETS);
        if (builder == null) {
            log.warn("Failed to get subnets");
            return Collections.EMPTY_LIST;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackSubnet> subnets = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode subnetList = (ArrayNode) node.path(PATH_SUBNETS);
            OpenstackSubnetCodec subnetCodec = new OpenstackSubnetCodec();
            subnetList.forEach(s -> subnets.add(subnetCodec.decode((ObjectNode) s, null)));
        } catch (IOException e) {
            log.warn("getSubnets()", e);
        }

        log.debug("subnets response:" + response);
        subnets.forEach(s -> log.debug("subnet ID: {}", s.id()));

        return subnets;
    }

    /**
     * Extracts OpenstackSecurityGroup information for the ID.
     *
     * @param id Security Group ID
     * @return OpenstackSecurityGroup object or null if fails
     */
    @Override
    public OpenstackSecurityGroup securityGroup(String id) {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_SECURITY_GROUPS + "/" + id);
        if (builder == null) {
            log.warn("Failed to get security group {}", id);
            return null;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        OpenstackSecurityGroup securityGroup = null;
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            OpenstackSecurityGroupCodec sgCodec = new OpenstackSecurityGroupCodec();
            securityGroup = sgCodec.decode(node, null);
        } catch (IOException e) {
            log.warn("securityGroup()", e);
        }

        return securityGroup;
    }

    private Invocation.Builder getClientBuilder(String baseUrl, String path) {
        if (Strings.isNullOrEmpty(baseUrl)) {
            log.warn("Keystone or Neutron URL is not set");
            return null;
        }

        WebTarget wt = client.target(baseUrl + path);
        return wt.request(JSON_UTF_8.toString());
    }

    private String getToken() {
        if (!isTokenValid()) {
            String request = "{\"auth\": {\"tenantName\": \"admin\", " +
                    "\"passwordCredentials\":  {\"username\": \"" +
                    userName + "\",\"password\": \"" + pass + "\"}}}";
            Invocation.Builder builder = getClientBuilder(keystoneUrl, URI_TOKENS);
            if (builder == null) {
                log.warn("Failed to get token");
                return null;
            }

            String response = builder.accept(MediaType.APPLICATION_JSON).post(Entity.json(request), String.class);
            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(response);
                tokenId = node.path(PATH_ACCESS).path(PATH_TOKEN).path(PATH_ID).asText();
                tokenExpires = node.path(PATH_ACCESS).path(PATH_TOKEN).path(PATH_EXPIRES).asText();
            } catch (IOException e) {
                log.warn("getToken()", e);
            }
            log.debug("token response:" + response);
        }

        return tokenId;
    }

    private boolean isTokenValid() {

        if (tokenExpires == null || tokenId == null || tokenExpires.isEmpty()) {
            return false;
        }

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TOKEN_DATE_FORMAT);
            Date exireDate = dateFormat.parse(tokenExpires);

            Calendar today = Calendar.getInstance();
            if (exireDate.after(today.getTime())) {
                return true;
            }
        } catch (ParseException e) {
            log.error("Token parse exception error : {}", e.getMessage());
            return false;
        }

        log.debug("token is Invalid");
        return false;
    }

    @Override
    public Collection<OpenstackPort> ports(String networkId) {
        return getPorts().stream()
                .filter(port -> port.networkId().equals(networkId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<OpenstackPort> ports() {
        return getPorts();
    }

    @Override
    public OpenstackPort port(Port port) {
        String uuid = port.annotations().value(PORT_NAME).substring(3);
        return getPorts().stream()
                .filter(p -> p.id().startsWith(uuid))
                .findAny().orElse(null);
    }

    @Override
    public OpenstackPort port(String portId) {
        return getPorts().stream()
                .filter(p -> p.id().equals(portId))
                .findAny().orElse(null);
    }

    @Override
    public OpenstackNetwork network(String networkId) {
        Collection<OpenstackSubnet> subnets = getSubnets().stream()
                .filter(s -> s.networkId().equals(networkId))
                .collect(Collectors.toList());

        OpenstackNetwork openstackNetwork = getNetworks().stream()
                .filter(n -> n.id().equals(networkId))
                .findAny().orElse(null);

        if (openstackNetwork == null) {
            return null;
        }

        return OpenstackNetwork.builder()
                .id(openstackNetwork.id())
                .name(openstackNetwork.name())
                .networkType(openstackNetwork.networkType())
                .segmentId(openstackNetwork.segmentId())
                .tenantId(openstackNetwork.tenantId())
                .subnets(subnets)
                .build();
    }

    @Override
    public Collection<OpenstackNetwork> networks() {
        return getNetworks();
    }

    @Override
    public OpenstackSubnet subnet(String subnetId) {
        return getSubnets().stream()
                .filter(subnet -> subnet.id().equals(subnetId))
                .findAny().orElse(null);
    }

    @Override
    public Collection<OpenstackSubnet> subnets() {
        return getSubnets();
    }

    @Override
    public Collection<OpenstackRouter> routers() {
        return getRouters();
    }

    @Override
    public OpenstackRouter router(String routerId) {
        return getRouters().stream()
                .filter(router -> router.id().equals(routerId))
                .findAny().orElse(null);
    }

    @Override
    public Collection<OpenstackFloatingIP> floatingIps() {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_FLOATINGIPS);
        if (builder == null) {
            log.warn("Failed to get floating IPs");
            return Collections.EMPTY_LIST;
        }

        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        log.debug("floatingIps response:" + response);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackFloatingIP> openstackFloatingIPs = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode floatingIpList = (ArrayNode) node.path(PATH_FLOATINGIPS);
            OpenstackFloatingIpCodec fipCodec = new OpenstackFloatingIpCodec();
            floatingIpList.forEach(f -> openstackFloatingIPs.add(fipCodec.decode((ObjectNode) f, null)));
        } catch (IOException e) {
            log.warn("floatingIps()", e);
        }

        openstackFloatingIPs.removeAll(Collections.singleton(null));

        return openstackFloatingIPs;
    }

    @Override
    public boolean updateFloatingIp(String id, String portId, Optional<Ip4Address> fixedIpAddress) {
        Invocation.Builder builder = getClientBuilder(neutronUrl, URI_FLOATINGIPS + "/" + id);

        if (builder == null || (portId != null && !fixedIpAddress.isPresent())) {
            log.warn("Failed to update floating IP");
            return false;
        }

        ObjectNode objectNode = createFloatingIpObject(portId, fixedIpAddress);

        InputStream inputStream = new ByteArrayInputStream(objectNode.toString().getBytes());

        try {
            Response response = builder.header(HEADER_AUTH_TOKEN, getToken())
                    .put(Entity.entity(IOUtils.toString(inputStream, StandardCharsets.UTF_8),
                            MediaType.APPLICATION_JSON));
            log.debug("updateFloatingIp called: {}, status: {}", response.readEntity(String.class),
                    String.valueOf(response.getStatus()));

            return checkReply(response);
        } catch (IOException e) {
            log.error("Cannot do PUT {} request");
            return false;
        }
    }

    private ObjectNode createFloatingIpObject(String portId, Optional<Ip4Address> fixedIpAddress) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();

        objectNode.putObject(FLOATINGIP)
                .put(PORT_ID, portId);

        if (portId != null) {
            objectNode.put(FIXED_IP_ADDRESS, fixedIpAddress.get().toString());
        }

        return objectNode;
    }

    private boolean checkReply(Response response) {
        if (response != null) {
            return checkStatusCode(response.getStatus());
        }

        log.warn("Null floating IP response from openstack");
        return false;
    }

    private boolean checkStatusCode(int statusCode) {
        if (statusCode == Response.Status.OK.getStatusCode()) {
            return true;
        }

        return false;
    }
    private void configureNetwork() {
        OpenstackInterfaceConfig cfg =
                cfgService.getConfig(appId, OpenstackInterfaceConfig.class);
        if (cfg == null) {
            log.error("There is no openstack server information in config.");
            return;
        }

        neutronUrl = checkNotNull(cfg.neutronServer());
        keystoneUrl = checkNotNull(cfg.keystoneServer());
        userName = checkNotNull(cfg.userName());
        pass = checkNotNull(cfg.password());
    }

    private class InternalConfigListener implements NetworkConfigListener {

        @Override
        public void event(NetworkConfigEvent event) {
            if (((event.type() == NetworkConfigEvent.Type.CONFIG_ADDED ||
                    event.type() == NetworkConfigEvent.Type.CONFIG_UPDATED)) &&
                    event.configClass().equals(OpenstackInterfaceConfig.class)) {

                log.info("Network configuration changed");
                networkEventExcutorService.execute(() -> configureNetwork());
            }
        }
    }
}
