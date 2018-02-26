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

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmConfigException;
import org.onosproject.incubator.net.l2monitoring.cfm.service.CfmMepService;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;

/**
 * Layer 2 CFM Maintenance Association Endpoint (MEP) by Device web resource.
 */
@Path("device")
public class DeviceMepWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Get all MEPs by Device Id. The device should support Meps
     *
     * @param deviceId The id of a device.
     * @return 200 OK with a list of MEPS or 500 on error
     */
    @GET
    @Path("{device_id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getAllMepsForDevice(@PathParam("device_id") String deviceId) {
        DeviceId deviceIdObj = DeviceId.deviceId(deviceId);
        log.debug("GET all Meps called for Device {}", deviceIdObj);
        try {
            Collection<Mep> mepCollection = get(CfmMepService.class)
                    .getAllMepsByDevice(deviceIdObj);
            ArrayNode an = mapper().createArrayNode();
            an.add(codec(Mep.class).encode(mepCollection, this));
            return ok(mapper().createObjectNode().set("meps", an)).build();
        } catch (CfmConfigException e) {
            log.error("Get all Meps on device {} failed because of exception",
                    deviceIdObj, e);
            return Response.serverError().entity("{ \"failure\":\"" + e.toString() + "\" }").build();
        }
    }
}
