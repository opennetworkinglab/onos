/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroupRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;
import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;

/**
 * Handles Security Group Rule Rest API call from Neutron ML2 plugin.
 */
@Path("security-group-rules")
public class OpenstackSecurityGroupRuleWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received security group rules %s request";
    private static final String SECURITY_GROUP_RULES = "security-group-rules";

    private final OpenstackSecurityGroupAdminService adminService =
            DefaultServiceDirectory.getService(OpenstackSecurityGroupAdminService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a security group from the JSON input stream.
     *
     * @param input security group JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated port already exists
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSecurityGroupRules(InputStream input) {
        log.trace(String.format(MESSAGE, "CREATE"));

        final NeutronSecurityGroupRule sgRule = readSecurityGroupRule(input);
        adminService.createSecurityGroupRule(sgRule);
        UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                .path(SECURITY_GROUP_RULES)
                .path(sgRule.getId());

        return created(locationBuilder.build()).build();
    }

    /**
     * Removes the security group rule.
     *
     * @param id security group rule ID
     * @return 204 NO_CONTENT
     */
    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeSecurityGroupRule(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "REMOVE " + id));

        adminService.removeSecurityGroupRule(id);
        return noContent().build();
    }

    private NeutronSecurityGroupRule readSecurityGroupRule(InputStream input) {
        try {
            JsonNode jsonTree = mapper().enable(INDENT_OUTPUT).readTree(input);
            log.trace(mapper().writeValueAsString(jsonTree));
            return ObjectMapperSingleton.getContext(NeutronSecurityGroupRule.class)
                    .readerFor(NeutronSecurityGroupRule.class)
                    .readValue(jsonTree);
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
