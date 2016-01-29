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
package org.onosproject.openstackswitching.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.onosproject.openstackswitching.OpenstackNetwork;
import org.onosproject.openstackswitching.OpenstackPort;
import org.onosproject.openstackswitching.OpenstackSubnet;
import org.onosproject.openstackswitching.web.OpenstackNetworkCodec;
import org.onosproject.openstackswitching.web.OpenstackPortCodec;
import org.onosproject.openstackswitching.web.OpenstackSecurityGroupCodec;
import org.onosproject.openstackswitching.web.OpenstackSubnetCodec;
import org.slf4j.Logger;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles REST Calls to Openstack Neutron.
 *
 */
public class OpenstackRestHandler {

    private static final String URI_NETWORKS = "networks";
    private static final String URI_PORTS = "ports";
    private static final String URI_SUBNETS = "subnets";
    private static final String URI_SECURITY_GROUPS = "security-groups";
    private static final String URI_TOKENS = "tokens";

    private static final String PATH_NETWORKS = "networks";
    private static final String PATH_PORTS = "ports";
    private static final String PATH_SUBNETS = "subnets";
    private static final String PATH_ACCESS = "access";
    private static final String PATH_TOKEN = "token";
    private static final String PATH_ID = "id";

    private static final String HEADER_AUTH_TOKEN = "X-Auth-Token";

    private final Logger log = getLogger(getClass());
    private String neutronUrl;
    private String keystoneUrl;
    private String tokenId;
    private String userName;
    private String pass;

    /**
     * Creates OpenstackRestHandler instance.
     *
     * @param cfg OpenstackSwitchingConfig reference
     */
    public OpenstackRestHandler(OpenstackSwitchingConfig cfg) {
        this.neutronUrl = checkNotNull(cfg.neutronServer());
        this.keystoneUrl = checkNotNull(cfg.keystoneServer());
        this.userName = checkNotNull(cfg.userName());
        this.pass = checkNotNull(cfg.password());
    }

    /**
     * Returns network information stored in Neutron.
     *
     * @return List of OpenstackNetwork
     */
    public Collection<OpenstackNetwork> getNetworks() {

        WebResource.Builder builder = getClientBuilder(neutronUrl + URI_NETWORKS);
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

        WebResource.Builder builder = getClientBuilder(neutronUrl + URI_PORTS);
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

    /**
     * Returns Subnet information in Neutron.
     *
     * @return List of OpenstackSubnet
     */
    public Collection<OpenstackSubnet> getSubnets() {

        WebResource.Builder builder = getClientBuilder(neutronUrl + URI_SUBNETS);
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
    public OpenstackSecurityGroup getSecurityGroup(String id) {
        WebResource.Builder builder = getClientBuilder(neutronUrl + URI_SECURITY_GROUPS + "/" + id);
        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header(HEADER_AUTH_TOKEN, getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        OpenstackSecurityGroup securityGroup = null;
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            OpenstackSecurityGroupCodec sgCodec = new OpenstackSecurityGroupCodec();
            securityGroup = sgCodec.decode(node, null);
        } catch (IOException e) {
            log.warn("getSecurityGroup()", e);
        }

        return securityGroup;
    }

    private WebResource.Builder getClientBuilder(String uri) {
        Client client = Client.create();
        WebResource resource = client.resource(uri);
        return resource.accept(JSON_UTF_8.toString())
                .type(JSON_UTF_8.toString());
    }

    private String getToken() {
        if (isTokenInvalid()) {
            String request = "{\"auth\": {\"tenantName\": \"admin\", " +
                    "\"passwordCredentials\":  {\"username\": \"" +
                    userName + "\",\"password\": \"" + pass + "\"}}}";
            WebResource.Builder builder = getClientBuilder(keystoneUrl + URI_TOKENS);
            String response = builder.accept(MediaType.APPLICATION_JSON).post(String.class, request);

            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(response);
                tokenId = node.path(PATH_ACCESS).path(PATH_TOKEN).path(PATH_ID).asText();
            } catch (IOException e) {
                log.warn("getToken()", e);
            }
            log.debug("token response:" + response);
        }

        return tokenId;
    }

    private boolean isTokenInvalid() {
        //TODO: validation check for the existing token
        return true;
    }

}
