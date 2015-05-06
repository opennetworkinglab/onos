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
package org.onosproject.rest.resources;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.topology.PathService;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.rest.AbstractWebResource;

/**
 * REST resource for interacting with path calculations.
 */
@Path("paths")
public class PathsWebResource extends AbstractWebResource {

    // Host id format is 00:00:00:00:00:01/-1
    private static final int VLAN_SEPARATOR_OFFSET = 17;
    private static final int FIRST_MAC_ADDRESS_SEPARATOR_OFFSET = 2;
    private static final int SECOND_MAC_ADDRESS_SEPARATOR_OFFSET = 5;
    private static final int THIRD_MAC_ADDRESS_SEPARATOR_OFFSET = 8;

    /**
     * Determines if the id appears to be the id of a host.
     *
     * @param id id string
     * @return HostId if the id is valid, null otherwise
     */
    private HostId isHostId(String id) {

        if (id.length() < VLAN_SEPARATOR_OFFSET + 1 ||
            id.charAt(FIRST_MAC_ADDRESS_SEPARATOR_OFFSET) != ':' ||
            id.charAt(SECOND_MAC_ADDRESS_SEPARATOR_OFFSET) != ':' ||
            id.charAt(THIRD_MAC_ADDRESS_SEPARATOR_OFFSET) != ':' ||
            id.charAt(VLAN_SEPARATOR_OFFSET) != '/') {
            return null;
        }

        return HostId.hostId(id);
    }

    /**
     * Gets the paths between two elements.
     *
     * @param src source
     * @param dst destination
     * @return path data
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{src}/{dst}")
    public Response getPath(@PathParam("src") String src,
                            @PathParam("dst") String dst) {
        PathService pathService = get(PathService.class);

        ElementId srcElement = isHostId(src);
        ElementId dstElement = isHostId(dst);

        if (srcElement == null) {
            // Doesn't look like a host, assume it is a device
            srcElement = DeviceId.deviceId(src);
        }

        if (dstElement == null) {
            // Doesn't look like a host, assume it is a device
            dstElement = DeviceId.deviceId(dst);
        }

        Set<org.onosproject.net.Path> paths =
                pathService.getPaths(srcElement, dstElement);
        ObjectNode root =
                    encodeArray(org.onosproject.net.Path.class, "paths", paths);
        return ok(root).build();
    }

}
