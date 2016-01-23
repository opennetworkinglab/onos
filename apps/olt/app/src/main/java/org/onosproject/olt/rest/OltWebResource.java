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
package org.onosproject.olt.rest;

import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.olt.AccessDeviceService;
import org.onosproject.rest.AbstractWebResource;

/**
 * OLT REST APIs.
 */

@Path("oltapp")
public class OltWebResource extends AbstractWebResource {

    /**
     * Provision a subscriber.
     *
     * @param device device id
     * @param port port number
     * @param vlan vlan id
     * @return 200 OK
     */
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{device}/{port}/{vlan}")
    public Response provisionSubscriber(
            @PathParam("device")String device,
            @PathParam("port")long port,
            @PathParam("vlan")short vlan) {
        AccessDeviceService service = get(AccessDeviceService.class);
        DeviceId deviceId = DeviceId.deviceId(device);
        PortNumber portNumber = PortNumber.portNumber(port);
        VlanId vlanId = VlanId.vlanId(vlan);
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        service.provisionSubscriber(connectPoint, vlanId);
        return ok("").build();
    }

    /**
     * Remove the provisioning for a subscriber.
     *
     * @param device device id
     * @param port port number
     * @return 200 OK
     */
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{device}/{port}")
    public Response removeSubscriber(
            @PathParam("device")String device,
            @PathParam("port")long port) {
        AccessDeviceService service = get(AccessDeviceService.class);
        DeviceId deviceId = DeviceId.deviceId(device);
        PortNumber portNumber = PortNumber.portNumber(port);
        ConnectPoint connectPoint = new ConnectPoint(deviceId, portNumber);
        service.removeSubscriber(connectPoint);
        return ok("").build();
    }
}
