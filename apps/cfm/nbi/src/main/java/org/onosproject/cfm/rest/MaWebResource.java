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
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.cfm.web.MaintenanceAssociationCodec;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Layer 2 CFM Maintenance Association web resource.
 */
@Path("md/{md_name}/ma")
public class MaWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get Maintenance Association by MD and MA name.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @return 200 OK with details of MA or 500 on Error
     */
    @GET
    @Path("{ma_name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getMa(@PathParam("md_name") String mdName,
                          @PathParam("ma_name") String maName) {
        log.debug("GET called for MA {}/{}", mdName, maName);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MaintenanceAssociation ma = get(CfmMdService.class)
                .getMaintenanceAssociation(mdId, maId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "MA " + maName + " not Found"));
            ObjectNode node = mapper().createObjectNode();
            node.set("ma", codec(MaintenanceAssociation.class).encode(ma, this));
            return ok(node).build();
        } catch (IllegalArgumentException e) {
            log.error("Get MA {} failed", mdName + "/" + maName, e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Delete the Maintenance Association by MD and MA name.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @return 200 OK if removed, 304 if item was not found or 500 on Error
     */
    @DELETE
    @Path("{ma_name}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMa(@PathParam("md_name") String mdName,
                             @PathParam("ma_name") String maName) {
        log.debug("DELETE called for MA {}/{}", mdName, maName);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            boolean deleted = get(CfmMdService.class)
                                        .deleteMaintenanceAssociation(mdId, maId);
            if (!deleted) {
                return Response.notModified(mdName + "/"
                                            + maName + " did not exist").build();
            } else {
                return ok("{ \"success\":\"deleted " + mdName
                                                + "/" + maName + "\" }").build();
            }
        } catch (CfmConfigException e) {
            log.error("Delete Maintenance Association {} failed",
                    mdName + "/" + maName, e);
            return Response.serverError().entity("{ \"failure\":\"" +
                                                e.toString() + "\" }").build();
        }
    }

    /**
     * Create Maintenance Association by MD and MA name.
     *
     * @onos.rsModel MaCreate
     * @param mdName The name of a Maintenance Domain
     * @param input A JSON formatted input stream specifying the MA parameters
     * @return 200 OK or 500 on error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMaintenanceAssociation(@PathParam("md_name") String mdName,
                                                 InputStream input) {
        log.debug("POST called to Create MA");
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            Optional<MaintenanceDomain> md = get(CfmMdService.class)
                                            .getMaintenanceDomain(mdId);
            if (!md.isPresent()) {
                return Response.serverError().entity("{ \"failure\":\"md "
                                        + mdName + " does not exist\" }").build();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper, input);
            JsonCodec<MaintenanceAssociation> maCodec =
                                            codec(MaintenanceAssociation.class);

            MaintenanceAssociation ma;
            try {
                ma = ((MaintenanceAssociationCodec) maCodec)
                        .decode((ObjectNode) cfg, this, mdId.getNameLength());
            } catch (Exception e) {
                log.error("Create MaintenanceAssociation on MD {} failed", mdName, e);
                return Response.serverError()
                        .entity("{ \"failure\":\"" + e.toString() + "\" }")
                        .build();
            }

            Boolean alreadyExists = get(CfmMdService.class)
                                        .createMaintenanceAssociation(mdId, ma);
            if (alreadyExists) {
                return Response.notModified(mdName + "/" + ma.maId() +
                                                    " already exists").build();
            }
            return Response
                    .created(new URI("md/" + mdName + "/ma/" + ma.maId()))
                    .entity("{ \"success\":\"" + mdName + "/" + ma.maId() + " created\" }")
                    .build();

        } catch (Exception | CfmConfigException e) {
            log.error("Create MaintenanceAssociation on MD {} failed", mdName, e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }
    }
}
