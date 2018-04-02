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
package org.onosproject.soam.rest;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

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

import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdIdCharStr;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.incubator.net.l2monitoring.soam.SoamConfigException;
import org.onosproject.incubator.net.l2monitoring.soam.SoamId;
import org.onosproject.incubator.net.l2monitoring.soam.SoamService;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.delay.DelayMeasurementEntry;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Layer 2 SOAM Delay Measurement web resource.
 */
@Path("md/{md_name}/ma/{ma_name}/mep/{mep_id}/dm")
public class DmWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get all DMs for a Mep.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The ID of a Mep belonging to the MA
     * @return 200 OK with a list of DMs or 500 on error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAllDmsForMep(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepId) {
        log.debug("GET all DMs called for MEP {}", mdName + "/" + maName + "/" + mepId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            Collection<DelayMeasurementEntry> dmCollection =
                    get(SoamService.class).getAllDms(mdId, maId, mepIdObj);
            ArrayNode an = mapper().createArrayNode();
            an.add(codec(DelayMeasurementEntry.class).encode(dmCollection, this));
            return ok(mapper().createObjectNode().set("dms", an)).build();
        } catch (CfmConfigException | SoamConfigException e) {
            log.error("Get DM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Get DM by MD name, MA name, Mep Id and Dm id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param dmId The Id of the DM
     * @return 200 OK with details of the DM or 500 on error
     */
    @GET
    @Path("{dm_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getDm(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepId, @PathParam("dm_id") int dmId) {
        log.debug("GET called for DM {}", mdName + "/" + maName + "/" + mepId + "/" + dmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId dmIdObj = SoamId.valueOf(dmId);
            DelayMeasurementEntry dm = get(SoamService.class)
                                    .getDm(mdId, maId, mepIdObj, dmIdObj);
            if (dm == null) {
                return Response.serverError().entity("{ \"failure\":\"DM " +
                        mdName + "/" + maName + "/" + mepId + "/" + dmId + " not found\" }").build();
            }
            ObjectNode node = mapper().createObjectNode();
            node.set("dm", codec(DelayMeasurementEntry.class).encode(dm, this));
            return ok(node).build();
        } catch (CfmConfigException | SoamConfigException e) {
            log.error("Get DM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + dmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Abort DM by MD name, MA name, Mep Id and DM Id.
     * In the API the measurement is aborted, and not truly deleted. It still
     * remains so that its results may be read. Depending on the device it will
     * get overwritten on the creation of subsequent measurements.
     * Use clear stats to delete old results
     * measurements.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param dmId The Id of the DM
     * @return 200 OK or 304 if not found, or 500 on error
     */
    @DELETE
    @Path("{dm_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response abortDm(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepId,
            @PathParam("dm_id") int dmId) {
        log.debug("DELETE called for DM {}", mdName + "/" + maName + "/" + mepId + "/" + dmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId dmIdObj = SoamId.valueOf(dmId);

            get(SoamService.class).abortDm(mdId, maId, mepIdObj, dmIdObj);
            return ok("{ \"success\":\"deleted (aborted) " + mdName + "/" + maName +
                    "/" + mepId + "/" + dmId + "\" }").build();
        } catch (CfmConfigException e) {
            log.error("Delete (abort) DM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + dmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Create DM with MD name, MA name, Mep id and DM Json.
     *
     * @onos.rsModel DmCreate
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP belonging to the MEP
     * @param input A JSON formatted input stream specifying the DM parameters
     * @return 201 Created or 304 if already exists or 500 on error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createDm(@PathParam("md_name") String mdName,
            @PathParam("ma_name") String maName,
            @PathParam("mep_id") short mepId, InputStream input) {
        log.debug("POST called to Create Dm");
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);

            Mep mep = get(CfmMepService.class).getMep(mdId, maId, mepIdObj);
            if (mep == null) {
                return Response.serverError().entity("{ \"failure\":\"mep " +
                        mdName + "/" + maName + "/" + mepId + " does not exist\" }").build();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode cfg = readTreeFromStream(mapper, input);
            JsonCodec<DelayMeasurementCreate> dmCodec = codec(DelayMeasurementCreate.class);

            DelayMeasurementCreate dm = dmCodec.decode((ObjectNode) cfg, this);
            get(SoamService.class).createDm(mdId, maId, mepIdObj, dm);
            return Response
                    .created(new URI("md/" + mdName + "/ma/" + maName + "/mep/" +
                            mepId + "/dm"))
                    .entity("{ \"success\":\"dm " + mdName + "/" + maName + "/" +
                            mepId + " created\" }")
                    .build();
        } catch (CfmConfigException | SoamConfigException | IllegalArgumentException |
                IOException | URISyntaxException e) {
            log.error("Create DM on " + mdName + "/" + maName + "/" + mepId +
                    " failed because of exception {}", e.toString());
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }
    }

    /**
     * Clear DM history stats by MD name, MA name, Mep Id and DM Id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param dmId The Id of the DM
     * @return 200 OK or 304 if not found, or 500 on error
     */
    @PUT
    @Path("{dm_id}/clear-history")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearDmHistory(@PathParam("md_name") String mdName,
                            @PathParam("ma_name") String maName,
                            @PathParam("mep_id") short mepId,
                            @PathParam("dm_id") int dmId) {
        log.debug("clear-history called for DM {}", mdName + "/" + maName +
                "/" + mepId + "/" + dmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId dmIdObj = SoamId.valueOf(dmId);

            get(SoamService.class).clearDelayHistoryStats(mdId, maId, mepIdObj, dmIdObj);
            return ok("{ \"success\":\"cleared DM history stats for " +
                    mdName + "/" + maName + "/" + mepId + "/" + dmId + "\" }").build();
        } catch (CfmConfigException e) {
            log.error("Clear history stats for DM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + dmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" +
                    e.toString() + "\" }").build();
        }
    }
}
