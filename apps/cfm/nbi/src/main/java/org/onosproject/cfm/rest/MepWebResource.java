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
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.cfm.web.MepCodec;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceAssociation;
import org.onosproject.incubator.net.l2monitoring.cfm.MaintenanceDomain;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMdService;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Layer 2 CFM Maintenance Association Endpoint (MEP) web resource.
 */
@Path("md/{md_name}/ma/{ma_name}/mep")
public class MepWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get all MEPs by MD name, MA name.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @return 200 OK with a list of MEPS or 500 on error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAllMepsForMa(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName) {
        log.debug("GET all Meps called for MA {}", mdName + "/" + maName);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            Collection<MepEntry> mepCollection = get(CfmMepService.class).getAllMeps(mdId, maId);
            ArrayNode an = mapper().createArrayNode();
            an.add(codec(MepEntry.class).encode(mepCollection, this));
            return ok(mapper().createObjectNode().set("meps", an)).build();
        } catch (CfmConfigException e) {
            log.error("Get all Meps on {} failed because of exception",
                    mdName + "/" + maName, e);
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }

    }

    /**
     * Get MEP by MD name, MA name and Mep Id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @return 200 OK with details of the MEP or 500 on error
     */
    @GET
    @Path("{mep_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getMep(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepId) {
        log.debug("GET called for MEP {}", mdName + "/" + maName + "/" + mepId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepEntry mepEntry = get(CfmMepService.class)
                    .getMep(mdId, maId, MepId.valueOf(mepId));
            if (mepEntry == null) {
                return Response.serverError().entity("{ \"failure\":\"MEP " +
                        mdName + "/" + maName + "/" + mepId + " not found\" }").build();
            }
            ObjectNode node = mapper().createObjectNode();
            node.set("mep", codec(MepEntry.class).encode(mepEntry, this));
            return ok(node).build();
        } catch (CfmConfigException e) {
            log.error("Get Mep {} failed because of exception",
                    mdName + "/" + maName + "/" + mepId, e);
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Delete MEP by MD name, MA name and Mep Id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepIdShort The Id of the MEP
     * @return 200 OK or 304 if not found, or 500 on error
     */
    @DELETE
    @Path("{mep_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteMep(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepIdShort) {
        log.debug("DELETE called for MEP " + mdName + "/" + maName + "/" + mepIdShort);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            boolean deleted = get(CfmMepService.class)
                    .deleteMep(mdId, maId, MepId.valueOf(mepIdShort), Optional.empty());
            if (!deleted) {
                return Response.notModified(mdName + "/" + maName + "/" +
                        mepIdShort + " did not exist").build();
            } else {
                return ok("{ \"success\":\"deleted " + mdName + "/" + maName +
                        "/" + mepIdShort + "\" }").build();
            }
        } catch (CfmConfigException e) {
            log.error("Delete Mep {} failed because of exception ",
                    mdName + "/" + maName + "/" + mepIdShort, e);
            return Response.serverError().entity("{ \"failure\":\"" +
                    e.toString() + "\" }").build();
        }
    }

    /**
     * Create MEP with MD name, MA name and Mep Json.
     *
     * @onos.rsModel MepCreate
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param input A JSON formatted input stream specifying the Mep parameters
     * @return 201 Created or 304 if already exists or 500 on error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMep(@PathParam("md_name") String mdName,
                              @PathParam("ma_name") String maName, InputStream input) {
        log.debug("POST called to Create Mep");
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MaintenanceAssociation ma =
                    get(CfmMdService.class).getMaintenanceAssociation(mdId, maId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "MA " + mdName + "/" + maName + " not Found"));

            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper(), input);
            JsonCodec<Mep> mepCodec = codec(Mep.class);

            Mep mep = ((MepCodec) mepCodec).decode((ObjectNode) cfg, this, mdName, maName);

            Boolean didNotExist = get(CfmMepService.class).createMep(mdId, maId, mep);
            if (!didNotExist) {
                return Response.notModified(mdName + "/" + ma.maId() + "/" + mep.mepId() +
                        " already exists").build();
            }
            return Response
                    .created(new URI("md/" + mdName + "/ma/" + ma.maId() +
                            "/mep/" + mep.mepId()))
                    .entity("{ \"success\":\"mep " + mdName + "/" + ma.maId() +
                            "/" + mep.mepId() + " created\" }")
                    .build();
        } catch (Exception | CfmConfigException e) {
            log.error("Create Mep on " + mdName + "/" + maName + " failed because of exception {}",
                      e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }
    }

    /**
     * Transmit Loopback on MEP with MD name, MA name and Mep Id.
     *
     * @onos.rsModel MepLbTransmit
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepIdShort The id of a MEP belonging to the MA
     * @param input A JSON formatted input stream specifying the Mep parameters
     * @return 202 Received with success message or 500 on error
     */
    @PUT
    @Path("{mep_id}/transmit-loopback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transmitLoopback(
            @PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepIdShort,
            InputStream input) {
        log.debug("PUT called to Transmit Loopback on Mep");

        MdId mdId = MdIdCharStr.asMdId(mdName);
        MaIdShort maId = MaIdCharStr.asMaId(maName);
        MaintenanceDomain md;
        Optional<MaintenanceDomain> mdOpt = get(CfmMdService.class).getMaintenanceDomain(mdId);
        if (mdOpt.isPresent()) {
            md = mdOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + mdName + " does not exist\" }")
                    .build();
        }
        MaintenanceAssociation ma;
        Optional<MaintenanceAssociation> maOpt = get(CfmMdService.class)
                .getMaintenanceAssociation(mdId, maId);
        if (maOpt.isPresent()) {
            ma = maOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + maName + " does not exist\" }")
                    .build();
        }

        MepId mepId = MepId.valueOf(mepIdShort);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper(), input);
            JsonCodec<MepLbCreate> mepLbCreateCodec = codec(MepLbCreate.class);

            MepLbCreate lbCreate = mepLbCreateCodec.decode((ObjectNode) cfg, this);
            get(CfmMepService.class).transmitLoopback(md.mdId(), ma.maId(), mepId, lbCreate);
        } catch (Exception | CfmConfigException e) {
            log.error("Transmit Loopback on " + mdName + "/" + maName +
                    "/{} failed", String.valueOf(mepIdShort), e);
            return Response.serverError()
                  .entity("{ \"failure\":\"" + e.toString() + "\" }")
                  .build();
        }

        return Response.accepted()
            .entity("{ \"success\":\"Loopback on MEP " + mdName + "/" + ma.maId() + "/"
                    + mepId.id() + " started\" }").build();
    }

    /**
     * Abort Loopback on MEP with MD name, MA name and Mep Id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepIdShort The id of a MEP belonging to the MA
     * @return 202 Received with success message or 500 on error
     */
    @PUT
    @Path("{mep_id}/abort-loopback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response abortLoopback(
            @PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepIdShort) {
        log.debug("PUT called to Abort Loopback on Mep");

        MdId mdId = MdIdCharStr.asMdId(mdName);
        MaIdShort maId = MaIdCharStr.asMaId(maName);
        MaintenanceDomain md;
        Optional<MaintenanceDomain> mdOpt = get(CfmMdService.class).getMaintenanceDomain(mdId);
        if (mdOpt.isPresent()) {
            md = mdOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + mdName + " does not exist\" }")
                    .build();
        }
        MaintenanceAssociation ma;
        Optional<MaintenanceAssociation> maOpt = get(CfmMdService.class)
                .getMaintenanceAssociation(mdId, maId);
        if (maOpt.isPresent()) {
            ma = maOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + maName + " does not exist\" }")
                    .build();
        }

        MepId mepId = MepId.valueOf(mepIdShort);

        try {
            get(CfmMepService.class).abortLoopback(md.mdId(), ma.maId(), mepId);
        } catch (CfmConfigException e) {
            log.error("Abort Loopback on " + mdName + "/" + maName +
                    "/{} failed", String.valueOf(mepIdShort), e);
            return Response.serverError()
                  .entity("{ \"failure\":\"" + e.toString() + "\" }")
                  .build();
        }

        return Response.accepted()
            .entity("{ \"success\":\"Loopback on MEP " + mdName + "/" + ma.maId() + "/"
                    + mepId.id() + " aborted\" }").build();
    }

    /**
     * Transmit Linktrace on MEP with MD name, MA name and Mep Id.
     *
     * @onos.rsModel MepLtTransmit
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepIdShort The id of a MEP belonging to the MA
     * @param input A JSON formatted input stream specifying the Linktrace parameters
     * @return 202 Received with success message or 500 on error
     */
    @PUT
    @Path("{mep_id}/transmit-linktrace")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response transmitLinktrace(
            @PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepIdShort,
            InputStream input) {
        log.debug("PUT called to Transmit Linktrace on Mep");

        MdId mdId = MdIdCharStr.asMdId(mdName);
        MaIdShort maId = MaIdCharStr.asMaId(maName);
        MaintenanceDomain md;
        Optional<MaintenanceDomain> mdOpt = get(CfmMdService.class).getMaintenanceDomain(mdId);
        if (mdOpt.isPresent()) {
            md = mdOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + mdName + " does not exist\" }")
                    .build();
        }
        MaintenanceAssociation ma;
        Optional<MaintenanceAssociation> maOpt = get(CfmMdService.class)
                .getMaintenanceAssociation(mdId, maId);
        if (maOpt.isPresent()) {
            ma = maOpt.get();
        } else {
            return Response.serverError()
                    .entity("{ \"failure\":\"" + maName + " does not exist\" }")
                    .build();
        }

        MepId mepId = MepId.valueOf(mepIdShort);

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper, input);
            JsonCodec<MepLtCreate> mepLtCreateCodec = codec(MepLtCreate.class);

            MepLtCreate ltCreate = mepLtCreateCodec.decode((ObjectNode) cfg, this);
            get(CfmMepService.class).transmitLinktrace(md.mdId(), ma.maId(), mepId, ltCreate);
        } catch (Exception | CfmConfigException e) {
            log.error("Transmit Linktrace on " + mdName + "/" + maName +
                    "/{} failed", String.valueOf(mepIdShort), e);
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }

        return Response.accepted()
                .entity("{ \"success\":\"Linktrace on MEP " + mdName + "/" + ma.maId() + "/"
                        + mepId.id() + " started\" }").build();
    }
}
