/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.vpls.rest;



import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intf.Interface;
import org.onosproject.net.intf.InterfaceAdminService;
import org.onosproject.rest.AbstractWebResource;
import org.onosproject.vpls.api.Vpls;
import org.onosproject.vpls.api.VplsData;

import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.ArrayList;


import static org.onlab.util.Tools.nullIsNotFound;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.readTreeFromStream;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query and program Vplss.
 */
@Path("vpls")
public class VplsWebResource extends AbstractWebResource {
    @Context
    private UriInfo uriInfo;

    private static final String VPLS_NOT_FOUND = "Vpls is not found for ";
    private static final String DEVICE_NOT_FOUND = "Device is not found for ";
    private static final String PORT_NOT_FOUND = "Port is not found for ";
    private static final String VPLSS = "vplss";
    private static final String VPLS = "vpls";
    private static final String INTERFACES = "interfaces";
    private static final String INTERFACES_KEY_ERROR = "No interfaces";

    private final ObjectNode root = mapper().createObjectNode();
    private final Logger log = getLogger(getClass());
    /**
     * Gets all Vplss. Returns array of all Vplss in the system.
     *
     * @return 200 OK with a collection of Vplss
     * @onos.rsModel Vplss
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getVplss() {
        ArrayNode vplssNode = root.putArray(VPLSS);
        Vpls service = get(Vpls.class);
        Collection<VplsData> vplsDatas = service.getAllVpls();
        if (!vplsDatas.isEmpty()) {
            for (VplsData entry : vplsDatas) {
                vplssNode.add(codec(VplsData.class).encode(entry, this));
            }
        }

        return ok(root).build();
    }

    /**
     * Gets Vpls. Returns a Vpls by vplsName.
     * @param  vplsName  vpls name
     * @return 200 OK with a vpls, return 404 if no entry has been found
     * @onos.rsModel Vpls
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{vplsName}")
    public Response getVpls(@PathParam("vplsName") String vplsName) {
        ArrayNode vplsNode = root.putArray(VPLS);
        Vpls service = get(Vpls.class);
        final VplsData vplsData = nullIsNotFound(service.getVpls(vplsName),
                VPLS_NOT_FOUND + vplsName);
        vplsNode.add(codec(VplsData.class).encode(vplsData, this));

        return ok(root).build();
    }
    /**
     * Creates new vpls. Creates and installs a new Vplps.<br>
     *
     * @param stream Vpls JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel VplsPost
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createVpls(InputStream stream) {
        Vpls service = get(Vpls.class);
        DeviceService deviceService = get(DeviceService.class);
        InterfaceAdminService interfaceService = get(InterfaceAdminService.class);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);

            VplsData vplsData = codec(VplsData.class).decode(jsonTree, this);
            vplsData.interfaces().forEach(interf -> {
                nullIsNotFound(deviceService.getDevice(interf.connectPoint().deviceId()),
                        DEVICE_NOT_FOUND + interf.connectPoint().deviceId());
                nullIsNotFound(deviceService.getPort(interf.connectPoint()),
                        PORT_NOT_FOUND + interf.connectPoint().port());
                    interfaceService.add(interf);
            });
            service.addInterfaces(vplsData, vplsData.interfaces());

            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path(VPLS);
            return Response
                    .created(locationBuilder.build())
                    .build();

            } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

    }

    /**
     * Add new interfaces. Add new interfaces to a Vpls.<br>
     *
     * @param stream interfaces JSON
     * @param vplsName Vpls name
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel InterfacesPost
     */
    @POST
    @Path("interfaces/{vplsName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addInterfaces(@PathParam("vplsName") String vplsName, InputStream stream) {
        Vpls service = get(Vpls.class);
        DeviceService deviceService = get(DeviceService.class);
        InterfaceAdminService interfaceService = get(InterfaceAdminService.class);
        final VplsData vplsData = nullIsNotFound(service.getVpls(vplsName),
                VPLS_NOT_FOUND + vplsName);
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            ArrayNode routesArray = nullIsIllegal((ArrayNode) jsonTree.get(INTERFACES),
                    INTERFACES_KEY_ERROR);
            Collection<Interface> interfaceList = new ArrayList<>();
           routesArray.forEach(interfJson -> {
                Interface inter = codec(Interface.class).decode((ObjectNode) interfJson, this);
                nullIsNotFound(deviceService.getDevice(inter.connectPoint().deviceId()),
                        DEVICE_NOT_FOUND + inter.connectPoint().deviceId());
                nullIsNotFound(deviceService.getPort(inter.connectPoint()),
                        PORT_NOT_FOUND + inter.connectPoint().port());
                interfaceList.add(inter);
                interfaceService.add(inter);
            });
            service.addInterfaces(vplsData, interfaceList);
            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path(INTERFACES)
                    .path(vplsName);
            return Response
                    .created(locationBuilder.build())
                    .build();

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Removes the specified vpls.
     *
     * @param vplsName Vpls name
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{vplsName}")
    public Response deleteVpls(@PathParam("vplsName") String vplsName) {
        Vpls service = get(Vpls.class);
        final VplsData vplsData = nullIsNotFound(service.getVpls(vplsName),
                VPLS_NOT_FOUND + vplsName);
        service.removeVpls(vplsData);
        return Response.noContent().build();
    }

    /**
     * Removes a specified interface.
     *
     * @param vplsName Vpls name
     * @param interfaceName interface name
     * @return 204 NO CONTENT
     *
     */
    @DELETE
    @Path("interface/{vplsName}/{interfaceName}")
    public Response deleteInterface(@PathParam("vplsName") String vplsName,
                                    @PathParam("interfaceName") String interfaceName) {
        Vpls service = get(Vpls.class);
        InterfaceAdminService interfaceService = get(InterfaceAdminService.class);
        final VplsData vplsData = nullIsNotFound(service.getVpls(vplsName),
                VPLS_NOT_FOUND + vplsName);
        vplsData.interfaces().forEach(anInterface -> {
            if (anInterface.name().equals(interfaceName)) {
                interfaceService.remove(anInterface.connectPoint(), anInterface.name());
                service.removeInterface(vplsData, anInterface);
            }
        });

        return Response.noContent().build();
    }

}
