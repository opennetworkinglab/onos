/*
 * Copyright 2018-present Open Networking Foundation
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
 *
 */

package org.onosproject.dhcprelay.rest;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.dhcprelay.api.DhcpRelayService;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.routeservice.Route;
import org.onosproject.routeservice.RouteStore;
import org.onosproject.routing.fpm.api.FpmRecord;
import org.slf4j.Logger;

import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * DHCP Relay agent REST API.
 */
@Path("fpm-delete")
public class DhcpRelayWebResource extends AbstractWebResource {
    private static final Logger LOG = getLogger(DhcpRelayWebResource.class);

    /**
     * Deletes the fpm route from fpm record.
     * Corresponding route from the route store
     *
     * @param prefix IpPrefix
     * @return 204 NO CONTENT
     * @throws IOException to signify bad request
     */
    @DELETE
    @Path("{prefix}")
    public Response dhcpFpmDelete(@PathParam("prefix") String prefix) {
        DhcpRelayService dhcpRelayService = get(DhcpRelayService.class);
        RouteStore routeStore = get(RouteStore.class);

        try {
            // removes fpm route from fpm record
            Optional<FpmRecord> fpmRecord = dhcpRelayService.removeFpmRecord(IpPrefix.valueOf(prefix));
            if (fpmRecord.isPresent()) {
                IpAddress nextHop = fpmRecord.get().nextHop();
                Route route = new Route(Route.Source.DHCP, IpPrefix.valueOf(prefix), nextHop);
                // removes DHCP route from route store
                routeStore.removeRoute(route);
            } else {
                LOG.warn("fpmRecord is not present");
            }
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(ex);
        }

        return Response.noContent().build();
    }

}
