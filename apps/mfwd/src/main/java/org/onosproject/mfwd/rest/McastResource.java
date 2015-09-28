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
package org.onosproject.mfwd.rest;

import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.mfwd.impl.McastConnectPoint;
import org.onosproject.mfwd.impl.McastRouteTable;
import org.onosproject.mfwd.impl.McastRouteBase;
import org.onosproject.mfwd.impl.MRibCodec;
import org.onosproject.rest.AbstractWebResource;

import org.slf4j.Logger;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Rest API for Multicast Forwarding.
 */
@Path("mcast")
public class McastResource extends AbstractWebResource  {

    private final Logger log = getLogger(getClass());
    private static final String SOURCE_ADDRESS = "sourceAddress";
    private static final String GROUP_ADDRESS = "groupAddress";
    private static final String INGRESS_POINT = "ingressPoint";
    private static final String EGRESS_POINT = "egressPoint";
    private static final String MCAST_GROUP = "mcastGroup";

    /**
     * Retrieve the multicast route table.
     *
     * @return the multicast route table.
     * @throws IOException if an error occurs
     */
    @Path("show")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response showAll() throws IOException {
        McastRouteTable mrt = McastRouteTable.getInstance();
        ObjectNode pushContent = new MRibCodec().encode(mrt , this);
        return ok(pushContent.toString()).build();
    }

    /**
     * Static join a multicast flow.
     *
     * @param sAddr source address to join
     * @param gAddr group address to join
     * @param ports ingress and egress ConnectPoints to join
     * @return the Result of the join
     * @throws IOException if something failed with the join command
     */
    @Path("/join")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    public Response join(@QueryParam("src") String sAddr,
                    @QueryParam("grp") String gAddr,
                    @DefaultValue("") @QueryParam("ports") String ports)
                    throws IOException {

        ObjectMapper mapper = new ObjectMapper();
        log.debug("Source IP Address: " + sAddr);
        log.debug("Destination IP Address: " + gAddr);
        log.debug("Ingress and Egress ports: " + ports);

        String output = "Insertion Faild";
        if (sAddr != null && gAddr != null && ports != null) {

            String[] portArr = ports.split(",");
            log.debug("Port Array Length: " + portArr.length);
            McastRouteTable mrt = McastRouteTable.getInstance();
            McastRouteBase mr = mrt.addRoute(sAddr, gAddr);

            // Port format "of:0000000000000023/4"
            log.debug("checking inside outer if: " + portArr.length);

            if (mr != null && portArr != null && portArr.length > 0) {

                String inCP = portArr[0];
                log.debug("Ingress port provided: " + inCP);
                mr.addIngressPoint(inCP);

                for (int i = 1; i < portArr.length; i++) {
                    String egCP = portArr[i];
                    log.debug("Egress port provided: " + egCP);
                    mr.addEgressPoint(egCP, McastConnectPoint.JoinSource.STATIC);
                }
                mrt.printMcastRouteTable();
                output = "Successfully Inserted";
            }
        } else {
            output = "Please Insert the rest uri correctly";
        }
        return Response.ok(output).build();
    }

    /**
     * Delete multicast state.
     *
     * @param src address to be deleted
     * @param grp address to be deleted
     * @return status of delete if successful
     */
    @Path("/delete")
    @DELETE
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    public Response removeMcastFlow(@QueryParam("src") String src,
                    @QueryParam("grp") String grp) {

        String resp = "Failed to delete";
        log.info("Source IP Address to delete: " + src);
        log.info("Destination IP Address to delete: " + grp);
        McastRouteTable mrt = McastRouteTable.getInstance();
        if (src != null && grp != null) {
            mrt.removeRoute(src, grp);
            resp = "Deleted flow for src " + src + " and grp " + grp;
        }

        return Response.ok(resp).build();
    }
}
