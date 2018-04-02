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
package org.onosproject.cfm.rest;

import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Layer 2 CFM Maintenance Domain web resource.
 */
@Path("md")
public class MdWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get all Maintenance Domains.
     *
     * @return 200 OK with a list of MDs and their children
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getMds() {
        log.debug("GET called for all MDs");
        Collection<MaintenanceDomain> mdMap =
                                get(CfmMdService.class).getAllMaintenanceDomain();
        ArrayNode arrayNode = mapper().createArrayNode();
        arrayNode.add(codec(MaintenanceDomain.class).encode(mdMap, this));
        return ok(mapper().createObjectNode().set("mds", arrayNode)).build();
    }

    /**
     * Get Maintenance Domain by name.
     *
     * @param mdName The name of a Maintenance Domain
     * @return 200 OK with the details of the MD and its children or 500 on error
     */
    @GET
    @Path("{md_name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getMd(@PathParam("md_name") String mdName) {
        log.debug("GET called for MD {}", mdName);
        try {
            MaintenanceDomain md = get(CfmMdService.class)
                    //FIXME Handle other types of name constructs e.g. DomainName
                    .getMaintenanceDomain(MdIdCharStr.asMdId(mdName))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "MD " + mdName + " not Found"));
            ObjectNode result = mapper().createObjectNode();
            result.set("md", codec(MaintenanceDomain.class).encode(md, this));
            return ok(result.toString()).build();
        } catch (IllegalArgumentException e) {
            log.error("Get MD {} failed", mdName, e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Delete Maintenance Domain by name.
     *
     * @param mdName The name of a Maintenance Domain
     * @return 200 OK, or 304 if not found or 500 on error
     */
    @DELETE
    @Path("{md_name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMd(@PathParam("md_name") String mdName) {
        log.debug("DELETE called for MD {}", mdName);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            boolean deleted = get(CfmMdService.class).deleteMaintenanceDomain(mdId);
            if (!deleted) {
                return Response.notModified(mdName + " did not exist").build();
            } else {
                return ok("{ \"success\":\"deleted " + mdName + "\" }").build();
            }
        } catch (CfmConfigException e) {
            log.error("Delete Maintenance Domain {} failed", mdName, e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Create Maintenance Domain.
     *
     * @onos.rsModel MdCreate
     * @param input A JSON formatted input stream specifying the MA parameters
     * @return 200 OK, 304 if MD already exists or 500 on error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMaintenanceDomain(InputStream input) {
        log.debug("POST called to Create MD");
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper, input);
            MaintenanceDomain md = codec(MaintenanceDomain.class).decode((ObjectNode) cfg, this);

            if (get(CfmMdService.class).createMaintenanceDomain(md)) {
                return Response.notModified(md.mdId().toString() + " already exists").build();
            }
            return Response
                    .created(new URI("md/" + md.mdId()))
                    .entity("{ \"success\":\"" + md.mdId() + " created\" }")
                    .build();

        } catch (Exception | CfmConfigException e) {
            log.error("Create MaintenanceDomain", e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }
    }
}
