/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.incubator.net.dpi.DpiStatInfo;
import org.onosproject.incubator.net.dpi.DpiStatistics;
import org.onosproject.incubator.net.dpi.DpiStatisticsManagerService;
import org.onosproject.incubator.net.dpi.FlowStatInfo;
import org.onosproject.incubator.net.dpi.ProtocolStatInfo;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query the latest DPI statistics info.
 */

@Path("dpis")
public class DpisWebResource extends AbstractWebResource {

    private final Logger log = getLogger(getClass());

    private static final int MAX_TOPN = 100;

    private final DpiStatisticsManagerService service = get(DpiStatisticsManagerService.class);

    public static final Comparator<ProtocolStatInfo> PROTOCOL_STAT_INFO_COMPARATOR =
        new Comparator<ProtocolStatInfo>() {
            @Override
            public int compare(ProtocolStatInfo psi1, ProtocolStatInfo psi2) {
                long delta = psi1.bytes() - psi2.bytes();
                return delta == 0 ? 0 : (delta > 0 ? -1 : +1);
            }
        };

    public static final Comparator<FlowStatInfo> FLOW_STAT_INFO_COMPARATOR =
            new Comparator<FlowStatInfo>() {
                @Override
                public int compare(FlowStatInfo fsi1, FlowStatInfo fsi2) {
                    long delta = fsi1.bytes() - fsi2.bytes();
                    return delta == 0 ? 0 : (delta > 0 ? -1 : +1);
                }
            };

    /**
     * Gets the latest dpi statistics.
     *
     * @param topn max size
     * @return 200 OK with a dpi statistics
     * @onos.rsModel DpiStatistics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDpisLatest(@QueryParam("topn") int topn) {
        log.debug("getDpisLatest request with topn={}", topn);

        DpiStatistics ds = service.getDpiStatisticsLatest();
        DpiStatistics retDs;

        if (ds == null) {
            retDs = new DpiStatistics("", new DpiStatInfo());
        } else if (topn <= 0) {
            retDs = ds;
        } else {
            if (topn > MAX_TOPN) {
                topn = MAX_TOPN;
            }
            retDs = new DpiStatistics(ds.receivedTime(),
                                      new DpiStatInfo(ds.dpiStatInfo().trafficStatistics()));
            List<ProtocolStatInfo> psiList = ds.dpiStatInfo().detectedProtos();
            if (psiList != null) {
                // sorts protocol list with descending order based on bytes within topn
                List<ProtocolStatInfo> psiListSorted =
                        psiList.stream().sorted(PROTOCOL_STAT_INFO_COMPARATOR).
                        limit(topn).collect(Collectors.toList());
                retDs.dpiStatInfo().setDetectedProtos(psiListSorted);
            }
            List<FlowStatInfo> fsiList = ds.dpiStatInfo().knownFlows();
            if (fsiList != null) {
                // sorts known flow list with descending order based on bytes within topn
                List<FlowStatInfo> fsiListSorted =
                        fsiList.stream().sorted(FLOW_STAT_INFO_COMPARATOR).
                                limit(topn).collect(Collectors.toList());
                retDs.dpiStatInfo().setKnownFlows(fsiListSorted);
            }
            fsiList = ds.dpiStatInfo().unknownFlows();
            if (fsiList != null) {
                // sorts unknown flow list with descending order based on bytes within topn
                List<FlowStatInfo> fsiListSorted =
                        fsiList.stream().sorted(FLOW_STAT_INFO_COMPARATOR).
                                limit(topn).collect(Collectors.toList());
                retDs.dpiStatInfo().setUnknownFlows(fsiListSorted);
            }
        }

        ObjectNode result = codec(DpiStatistics.class).encode(retDs, this);
        return ok(result).build();

    }

    /**
     * Gets the latest traffic statistics only.
     *
     * @return 200 OK with a traffic statistics
     * @onos.rsModel TrafficStatistics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("traffic")
    public Response getTrafficStatistics() {
        log.debug("getTrafficStatistics request");

        DpiStatistics ds = service.getDpiStatisticsLatest();
        if (ds == null) {
            ds = new DpiStatistics("", new DpiStatInfo());
        }

        DpiStatInfo dsi = new DpiStatInfo();
        dsi.setTrafficStatistics(ds.dpiStatInfo().trafficStatistics());
        DpiStatistics dsTraffic = new DpiStatistics(ds.receivedTime(), dsi);

        ObjectNode result = codec(DpiStatistics.class).encode(dsTraffic, this);
        return ok(result).build();
    }

    /**
     * Gets the latest detected protocol statistics only.
     *
     * @param topn max size
     * @return 200 OK with a protocol statistics
     * @onos.rsModel ProtocolStatistics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("protocols")
    public Response getDetectedProtocols(@QueryParam("topn") int topn) {
        log.debug("getDetectedProtocols request with topn={}", topn);

        DpiStatistics ds = service.getDpiStatisticsLatest();
        DpiStatistics dsProtocol;

        if (ds == null) {
            dsProtocol = new DpiStatistics("", new DpiStatInfo());
        } else if (topn <= 0) {
            DpiStatInfo dsi = new DpiStatInfo();
            dsi.setDetectedProtos(ds.dpiStatInfo().detectedProtos());
            dsProtocol = new DpiStatistics(ds.receivedTime(), dsi);
        } else {
            if (topn > MAX_TOPN) {
                topn = MAX_TOPN;
            }
            dsProtocol = new DpiStatistics(ds.receivedTime(), new DpiStatInfo());
            List<ProtocolStatInfo> psiList = ds.dpiStatInfo().detectedProtos();
            if (psiList != null) {
                // sorts protocol list with descending order based on bytes within topn
                List<ProtocolStatInfo> psiListSorted =
                        psiList.stream().sorted(PROTOCOL_STAT_INFO_COMPARATOR).
                                limit(topn).collect(Collectors.toList());
                dsProtocol.dpiStatInfo().setDetectedProtos(psiListSorted);
            }
        }

        ObjectNode result = codec(DpiStatistics.class).encode(dsProtocol, this);
        return ok(result).build();
    }

    /**
     * Gets the latest known flows statistics only.
     *
     * @param topn max size
     * @return 200 OK with a known flow statistics
     * @onos.rsModel KnownFlowStatistics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("knownFlows")
    public Response getKnownFlows(@QueryParam("topn") int topn) {
        log.debug("getKnownFlows request with topn={}", topn);

        DpiStatistics ds = service.getDpiStatisticsLatest();
        DpiStatistics dsKnownFlows;

        if (ds == null) {
            dsKnownFlows = new DpiStatistics("", new DpiStatInfo());
        } else if (topn <= 0) {
            DpiStatInfo dsi = new DpiStatInfo();
            dsi.setKnownFlows(ds.dpiStatInfo().knownFlows());
            dsKnownFlows = new DpiStatistics(ds.receivedTime(), dsi);
        } else {
            if (topn > MAX_TOPN) {
                topn = MAX_TOPN;
            }
            dsKnownFlows = new DpiStatistics(ds.receivedTime(), new DpiStatInfo());
            List<FlowStatInfo> fsiList = ds.dpiStatInfo().knownFlows();
            if (fsiList != null) {
                // sorts known flow list with descending order based on bytes within topn
                List<FlowStatInfo> fsiListSorted =
                        fsiList.stream().sorted(FLOW_STAT_INFO_COMPARATOR).
                                limit(topn).collect(Collectors.toList());
                dsKnownFlows.dpiStatInfo().setKnownFlows(fsiListSorted);
            }
        }

        ObjectNode result = codec(DpiStatistics.class).encode(dsKnownFlows, this);
        return ok(result).build();
    }

    /**
     * Gets the latest unknown flows statistics only.
     *
     * @param topn max size
     * @return 200 OK with an unknown flow statistics
     * @onos.rsModel UnknownFlowStatistics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("unknownFlows")
    public Response getUnknownFlows(@QueryParam("topn") int topn) {
        log.debug("getUnknownFlows request with topn={}", topn);

        DpiStatistics ds = service.getDpiStatisticsLatest();
        DpiStatistics dsUnknownFlows;

        if (ds == null) {
            dsUnknownFlows = new DpiStatistics("", new DpiStatInfo());
        } else if (topn <= 0) {
            DpiStatInfo dsi = new DpiStatInfo();
            dsi.setUnknownFlows(ds.dpiStatInfo().unknownFlows());
            dsUnknownFlows = new DpiStatistics(ds.receivedTime(), dsi);
        } else {
            if (topn > 100) {
                topn = 100;
            }
            dsUnknownFlows = new DpiStatistics(ds.receivedTime(), new DpiStatInfo());
            List<FlowStatInfo> fsiList = ds.dpiStatInfo().unknownFlows();
            if (fsiList != null) {
                // sorts unknown flow list with descending order based on bytes within topn
                List<FlowStatInfo> fsiListSorted =
                        fsiList.stream().sorted(FLOW_STAT_INFO_COMPARATOR).
                                limit(topn).collect(Collectors.toList());
                dsUnknownFlows.dpiStatInfo().setUnknownFlows(fsiListSorted);
            }
        }

        ObjectNode result = codec(DpiStatistics.class).encode(dsUnknownFlows, this);
        return ok(result).build();
    }

    /**
     * Add new dpi statistics entry at the end of list.
     *
     * @param stream dpi statistics JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel DpiStatisticsPost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addDpiStatistics(InputStream stream) {
        ObjectNode result;

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            log.debug("jsonTree={}", jsonTree);

            DpiStatistics ds = codec(DpiStatistics.class).decode(jsonTree, this);
            if (ds == null) {
                log.error("Wrong DpiStatistics json format error");
            }

            // TODO: check the validity of dpi statistics values, specially receivedTime format
            DpiStatistics added = service.addDpiStatistics(ds);

            result = codec(DpiStatistics.class).encode(added, this);
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return ok(result).build();
    }
}
