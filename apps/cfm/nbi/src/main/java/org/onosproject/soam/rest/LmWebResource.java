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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementCreate;
import org.onosproject.incubator.net.l2monitoring.soam.loss.LossMeasurementEntry;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;

import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Layer 2 SOAM Loss Measurement web resource.
 */
@Path("md/{md_name}/ma/{ma_name}/mep/{mep_id}/lm")
public class LmWebResource extends AbstractWebResource {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get all LMs for a Mep.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The ID of a Mep belonging to the MA
     * @return 200 OK with a list of LMs or 500 on error
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAllLmsForMep(@PathParam("md_name") String mdName,
                                    @PathParam("ma_name") String maName,
                                    @PathParam("mep_id") short mepId) {

        log.debug("GET all LMs called for MEP {}", mdName + "/" + maName + "/" + mepId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            Collection<LossMeasurementEntry> lmCollection =
                    get(SoamService.class).getAllLms(mdId, maId, mepIdObj);
            ArrayNode an = mapper().createArrayNode();
            an.add(codec(LossMeasurementEntry.class).encode(lmCollection, this));
            return ok(mapper().createObjectNode().set("lms", an)).build();
        } catch (CfmConfigException | SoamConfigException e) {
            log.error("Get LM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Get LM by MD name, MA name, Mep Id and Dm id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param lmId The Id of the LM
     * @return 200 OK with details of the LM or 500 on error
     */
    @GET
    @Path("{lm_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getLm(@PathParam("md_name") String mdName,
                          @PathParam("ma_name") String maName,
                          @PathParam("mep_id") short mepId, @PathParam("lm_id") int lmId) {
        log.debug("GET called for LM {}", mdName + "/" + maName + "/" + mepId + "/" + lmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId lmIdObj = SoamId.valueOf(lmId);
            LossMeasurementEntry lm = get(SoamService.class)
                                    .getLm(mdId, maId, mepIdObj, lmIdObj);
            if (lm == null) {
                return Response.serverError().entity("{ \"failure\":\"LM " +
                        mdName + "/" + maName + "/" + mepId + "/" + lmId + " not found\" }").build();
            }
            ObjectNode node = mapper().createObjectNode();
            node.set("lm", codec(LossMeasurementEntry.class).encode(lm, this));
            return ok(node).build();
        } catch (CfmConfigException | SoamConfigException e) {
            log.error("Get LM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + lmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Abort LM by MD name, MA name, Mep Id and LM Id.
     * In the API the measurement is aborted, and not truly deleted. It still
     * remains so that its results may be read. Depending on the device it will
     * get overwritten on the creation of subsequent measurements.
     * Use clear stats to delete old results measurements.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param lmId The Id of the LM
     * @return 200 OK or 304 if not found, or 500 on error
     */
    @DELETE
    @Path("{lm_id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response abortLm(@PathParam("md_name") String mdName,
                            @PathParam("ma_name") String maName,
                            @PathParam("mep_id") short mepId,
                            @PathParam("lm_id") int lmId) {
        log.debug("DELETE called for LM {}", mdName + "/" + maName + "/" + mepId + "/" + lmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId lmIdObj = SoamId.valueOf(lmId);

            get(SoamService.class).abortLm(mdId, maId, mepIdObj, lmIdObj);
            return ok("{ \"success\":\"deleted (aborted) " + mdName + "/" + maName +
                    "/" + mepId + "/" + lmId + "\" }").build();
        } catch (CfmConfigException e) {
            log.error("Delete (abort) LM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + lmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }

    /**
     * Create LM with MD name, MA name, Mep id and LM Json.
     *
     * @onos.rsModel LmCreate
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP belonging to the MEP
     * @param input A JSON formatted input stream specifying the LM parameters
     * @return 201 Created or 304 if already exists or 500 on error
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createLm(@PathParam("md_name") String mdName,
                             @PathParam("ma_name") String maName,
                             @PathParam("mep_id") short mepId, InputStream input) {
        log.debug("POST called to Create Lm");
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
            JsonCodec<LossMeasurementCreate> lmCodec = codec(LossMeasurementCreate.class);

            LossMeasurementCreate lm = lmCodec.decode((ObjectNode) cfg, this);
            get(SoamService.class).createLm(mdId, maId, mepIdObj, lm);
            return Response
                    .created(new URI("md/" + mdName + "/ma/" + maName + "/mep/" +
                            mepId + "/lm"))
                    .entity("{ \"success\":\"lm " + mdName + "/" + maName + "/" +
                            mepId + " created\" }")
                    .build();
        } catch (CfmConfigException | SoamConfigException | IllegalArgumentException |
                IOException | URISyntaxException e) {
            log.error("Create LM on " + mdName + "/" + maName +  "/" + mepId +
                    " failed because of exception {}", e.toString());
            return Response.serverError()
                    .entity("{ \"failure\":\"" + e.toString() + "\" }")
                    .build();
        }
    }

    /**
     * Clear LM history stats by MD name, MA name, Mep Id and LM Id.
     *
     * @param mdName The name of a Maintenance Domain
     * @param maName The name of a Maintenance Association belonging to the MD
     * @param mepId The Id of the MEP
     * @param lmId The Id of the LM
     * @return 200 OK or 304 if not found, or 500 on error
     */
    @PUT
    @Path("{lm_id}/clear-history")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearLmHistory(@PathParam("md_name") String mdName,
                                   @PathParam("ma_name") String maName,
                                   @PathParam("mep_id") short mepId,
                                   @PathParam("lm_id") int lmId) {
        log.debug("clear-history called for LM {}", mdName + "/" + maName +
                "/" + mepId + "/" + lmId);
        try {
            MdId mdId = MdIdCharStr.asMdId(mdName);
            MaIdShort maId = MaIdCharStr.asMaId(maName);
            MepId mepIdObj = MepId.valueOf(mepId);
            SoamId lmIdObj = SoamId.valueOf(lmId);

            get(SoamService.class).clearDelayHistoryStats(mdId, maId, mepIdObj, lmIdObj);
            return ok("{ \"success\":\"cleared LM history stats for " +
                    mdName + "/" + maName + "/" + mepId + "/" + lmId + "\" }").build();
        } catch (CfmConfigException e) {
            log.error("Clear history stats for LM {} failed because of exception {}",
                    mdName + "/" + maName + "/" + mepId + "/" + lmId, e.toString());
            return Response.serverError().entity("{ \"failure\":\"" +
                    e.toString() + "\" }").build();
        }
    }

}
