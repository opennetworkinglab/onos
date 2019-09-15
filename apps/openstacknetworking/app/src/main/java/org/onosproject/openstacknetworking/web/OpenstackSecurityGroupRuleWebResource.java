/*
 * Copyright 2017-present Open Networking Foundation
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

import org.apache.commons.io.IOUtils;
import org.onosproject.openstacknetworking.api.OpenstackHaService;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupAdminService;
import org.onosproject.rest.AbstractWebResource;
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
import java.io.IOException;
import java.io.InputStream;

import static javax.ws.rs.core.Response.created;
import static javax.ws.rs.core.Response.noContent;
import static org.onosproject.openstacknetworking.api.Constants.DEFAULT_ACTIVE_IP_ADDRESS;
import static org.onosproject.openstacknetworking.api.Constants.REST_UTF8;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.jsonToModelEntity;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.syncDelete;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.syncPost;

/**
 * Handles Security Group Rule Rest API call from Neutron ML2 plugin.
 */
@Path("security-group-rules")
public class OpenstackSecurityGroupRuleWebResource extends AbstractWebResource {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String MESSAGE = "Received security group rules %s request";
    private static final String SECURITY_GROUP_RULES = "security-group-rules";

    private final OpenstackSecurityGroupAdminService adminService =
                                    get(OpenstackSecurityGroupAdminService.class);
    private final OpenstackHaService haService = get(OpenstackHaService.class);

    @Context
    private UriInfo uriInfo;

    /**
     * Creates a security group from the JSON input stream.
     *
     * @param input security group JSON input stream
     * @return 201 CREATED if the JSON is correct, 400 BAD_REQUEST if the JSON
     * is invalid or duplicated security group rule ID already exists
     * @throws IOException exception
     * @onos.rsModel NeutronSecurityGroupRule
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSecurityGroupRules(InputStream input) throws IOException {
        log.trace(String.format(MESSAGE, "CREATE"));

        String inputStr = IOUtils.toString(input, REST_UTF8);

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncPost(haService, SECURITY_GROUP_RULES, inputStr);
        }

        final NeutronSecurityGroupRule sgRule = (NeutronSecurityGroupRule)
                        jsonToModelEntity(inputStr, NeutronSecurityGroupRule.class);

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
    public Response deleteSecurityGroupRule(@PathParam("id") String id) {
        log.trace(String.format(MESSAGE, "REMOVE " + id));

        if (!haService.isActive()
                && !DEFAULT_ACTIVE_IP_ADDRESS.equals(haService.getActiveIp())) {
            return syncDelete(haService, SECURITY_GROUP_RULES, id);
        }

        adminService.removeSecurityGroupRule(id);
        return noContent().build();
    }
}
