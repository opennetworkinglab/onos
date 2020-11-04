/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.web;

import com.fasterxml.jackson.databind.JsonNode;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.openstacknetworking.api.OpenstackK8sIntegrationService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * REST interface for integrating openstack and kubernetes.
 */
@Path("integration")
public class OpenstackK8sIntegrationWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String K8S_NODE_IP = "k8sNodeIp";
    private static final String OS_K8S_INT_PORT_NAME = "osK8sIntPortName";
    private static final String OS_K8S_EXT_PORT_NAME = "osK8sExtPortName";
    private static final String POD_CIDR = "podCidr";
    private static final String SERVICE_CIDR = "serviceCidr";
    private static final String POD_GW_IP = "podGwIp";
    private static final String K8S_INT_OS_PORT_MAC = "k8sIntOsPortMac";

    private final OpenstackK8sIntegrationService intService =
            get(OpenstackK8sIntegrationService.class);

    /**
     * Installs CNI pass-through related flow rules for each kubernetes nodes.
     *
     * @param input JSON string
     * @return 200 ok, 400 BAD_REQUEST if the json is malformed
     * @throws IOException exception
     */
    @PUT
    @Path("node/pt-install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installCniPtNodeRules(InputStream input) throws IOException {
        log.trace("Install K8S CNI pass-through node rules");

        JsonNode json = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
        IpAddress k8sNodeIp = IpAddress.valueOf(json.get(K8S_NODE_IP).asText());
        IpPrefix podCidr = IpPrefix.valueOf(json.get(POD_CIDR).asText());
        IpPrefix serviceCidr = IpPrefix.valueOf(json.get(SERVICE_CIDR).asText());
        IpAddress podGwIp = IpAddress.valueOf(json.get(POD_GW_IP).asText());
        String osK8sIntPortName = json.get(OS_K8S_INT_PORT_NAME).asText();
        MacAddress k8sIntOsPortMac = MacAddress.valueOf(json.get(K8S_INT_OS_PORT_MAC).asText());

        intService.installCniPtNodeRules(k8sNodeIp, podCidr, serviceCidr,
                podGwIp, osK8sIntPortName, k8sIntOsPortMac);

        return Response.ok().build();
    }

    /**
     * Uninstalls CNI pass-through related flow rules for each kubernetes nodes.
     *
     * @param input JSON string
     * @return 200 ok, 400 BAD_REQUEST if the json is malformed
     * @throws IOException exception
     */
    @PUT
    @Path("node/pt-uninstall")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uninstallCniPtNodeRules(InputStream input) throws IOException {
        log.trace("Uninstall K8S CNI pass-through node rules");

        JsonNode json = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
        IpAddress k8sNodeIp = IpAddress.valueOf(json.get(K8S_NODE_IP).asText());
        IpPrefix podCidr = IpPrefix.valueOf(json.get(POD_CIDR).asText());
        IpPrefix serviceCidr = IpPrefix.valueOf(json.get(SERVICE_CIDR).asText());
        IpAddress podGwIp = IpAddress.valueOf(json.get(POD_GW_IP).asText());
        String osK8sIntPortName = json.get(OS_K8S_INT_PORT_NAME).asText();
        MacAddress k8sIntOsPortMac = MacAddress.valueOf(json.get(K8S_INT_OS_PORT_MAC).asText());

        intService.uninstallCniPtNodeRules(k8sNodeIp, podCidr, serviceCidr,
                podGwIp, osK8sIntPortName, k8sIntOsPortMac);

        return Response.ok().build();
    }

    /**
     * Installs CNI pass-through related node port flow rules.
     *
     * @param input JSON string
     * @return 200 ok, 400 BAD_REQUEST if the json is malformed
     * @throws IOException exception
     */
    @PUT
    @Path("nodeport/pt-install")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response installCniPtNodePortRules(InputStream input) throws IOException {
        log.trace("Install K8S CNI pass-through node port rules");

        JsonNode json = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
        IpAddress k8sNodeIp = IpAddress.valueOf(json.get(K8S_NODE_IP).asText());
        String osK8sExtPortName = json.get(OS_K8S_EXT_PORT_NAME).asText();

        intService.installCniPtNodePortRules(k8sNodeIp, osK8sExtPortName);

        return Response.ok().build();
    }

    /**
     * Uninstalls CNI pass-through related node port flow rules.
     *
     * @param input JSON string
     * @return 200 ok, 400 BAD_REQUEST if the json is malformed
     * @throws IOException exception
     */
    @PUT
    @Path("nodeport/pt-uninstall")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uninstallCniPtNodePortRules(InputStream input) throws IOException {
        log.trace("Uninstall K8S CNI pass-through node port rules");

        JsonNode json = readTreeFromStream(mapper().enable(INDENT_OUTPUT), input);
        IpAddress k8sNodeIp = IpAddress.valueOf(json.get(K8S_NODE_IP).asText());
        String osK8sExtPortName = json.get(OS_K8S_EXT_PORT_NAME).asText();

        intService.uninstallCniPtNodePortRules(k8sNodeIp, osK8sExtPortName);

        return Response.ok().build();
    }
}
