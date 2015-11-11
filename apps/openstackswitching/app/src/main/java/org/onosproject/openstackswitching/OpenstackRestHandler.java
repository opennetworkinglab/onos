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
package org.onosproject.openstackswitching;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.onosproject.openstackswitching.web.OpenstackNetworkCodec;
import org.onosproject.openstackswitching.web.OpenstackPortCodec;
import org.onosproject.openstackswitching.web.OpenstackSubnetCodec;
import org.slf4j.Logger;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.net.MediaType.JSON_UTF_8;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles REST Calls to Openstack Neutron.
 *
 */
public class OpenstackRestHandler {

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

        WebResource.Builder builder = getClientBuilder(neutronUrl + "networks");
        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header("X-Auth-Token", getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackNetwork> openstackNetworks = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode networkList = (ArrayNode) node.path("networks");
            OpenstackNetworkCodec networkCodec = new OpenstackNetworkCodec();
            networkList.forEach(n -> openstackNetworks.add(networkCodec.decode((ObjectNode) n, null)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.debug("networks response:" + response);
        openstackNetworks.forEach(n -> log.debug("network ID: {}", n.id()));

        return openstackNetworks;
    }

    /**
     * Returns port information stored in Neutron.
     *
     * @return List of OpenstackPort
     */
    public Collection<OpenstackPort> getPorts() {

        WebResource.Builder builder = getClientBuilder(neutronUrl + "ports");
        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header("X-Auth-Token", getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackPort> openstackPorts = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode portList = (ArrayNode) node.path("ports");
            OpenstackPortCodec portCodec = new OpenstackPortCodec();
            portList.forEach(p -> openstackPorts.add(portCodec.decode((ObjectNode) p, null)));
        } catch (IOException e) {
            e.printStackTrace();
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

        WebResource.Builder builder = getClientBuilder(neutronUrl + "subnets");
        String response = builder.accept(MediaType.APPLICATION_JSON_TYPE).
                header("X-Auth-Token", getToken()).get(String.class);

        ObjectMapper mapper = new ObjectMapper();
        List<OpenstackSubnet> subnets = Lists.newArrayList();
        try {
            ObjectNode node = (ObjectNode) mapper.readTree(response);
            ArrayNode subnetList = (ArrayNode) node.path("subnets");
            OpenstackSubnetCodec subnetCodec = new OpenstackSubnetCodec();
            subnetList.forEach(s -> subnets.add(subnetCodec.decode((ObjectNode) s, null)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        log.debug("subnets response:" + response);
        subnets.forEach(s -> log.debug("subnet ID: {}", s.id()));

        return subnets;
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
            WebResource.Builder builder = getClientBuilder(keystoneUrl + "tokens");
            String response = builder.accept(MediaType.APPLICATION_JSON).post(String.class, request);

            ObjectMapper mapper = new ObjectMapper();
            try {
                ObjectNode node = (ObjectNode) mapper.readTree(response);
                tokenId = node.path("access").path("token").path("id").asText();
            } catch (IOException e) {
                e.printStackTrace();
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
