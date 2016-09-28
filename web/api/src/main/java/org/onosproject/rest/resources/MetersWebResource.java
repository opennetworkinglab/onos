/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;

import static org.onlab.util.Tools.nullIsNotFound;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Query and program meter rules.
 */
@Path("meters")
public class MetersWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private final Logger log = getLogger(getClass());
    private static final String DEVICE_INVALID = "Invalid deviceId in meter creation request";
    private static final String METER_NOT_FOUND = "Meter is not found for ";

    private final MeterService meterService = get(MeterService.class);
    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode metersNode = root.putArray("meters");

    /**
     * Returns all meters of all devices.
     *
     * @return 200 OK with array of all the meters in the system
     * @onos.rsModel Meters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMeters() {
        final Iterable<Meter> meters = meterService.getAllMeters();
        if (meters != null) {
            meters.forEach(meter -> metersNode.add(codec(Meter.class).encode(meter, this)));
        }
        return ok(root).build();
    }

    /**
     * Returns a collection of meters by the device id.
     *
     * @param deviceId device identifier
     * @return 200 OK with array of meters which belongs to specified device
     * @onos.rsModel Meters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}")
    public Response getMetersByDeviceId(@PathParam("deviceId") String deviceId) {
        DeviceId did = DeviceId.deviceId(deviceId);
        final Iterable<Meter> meters = meterService.getMeters(did);
        if (meters != null) {
            meters.forEach(meter -> metersNode.add(codec(Meter.class).encode(meter, this)));
        }
        return ok(root).build();
    }

    /**
     * Returns a meter by the meter id.
     *
     * @param deviceId device identifier
     * @param meterId meter identifier
     * @return 200 OK with a meter, return 404 if no entry has been found
     * @onos.rsModel Meter
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{meterId}")
    public Response getMeterByDeviceIdAndMeterId(@PathParam("deviceId") String deviceId,
                                                 @PathParam("meterId") String meterId) {
        DeviceId did = DeviceId.deviceId(deviceId);
        MeterId mid = MeterId.meterId(Long.valueOf(meterId));

        final Meter meter = nullIsNotFound(meterService.getMeter(did, mid),
                METER_NOT_FOUND + mid.id());

        metersNode.add(codec(Meter.class).encode(meter, this));
        return ok(root).build();
    }

    /**
     * Creates new meter rule. Creates and installs a new meter rule for the
     * specified device.
     *
     * @param deviceId device identifier
     * @param stream   meter rule JSON
     * @return status of the request - CREATED if the JSON is correct,
     * BAD_REQUEST if the JSON is invalid
     * @onos.rsModel MeterPost
     */
    @POST
    @Path("{deviceId}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createMeter(@PathParam("deviceId") String deviceId,
                                InputStream stream) {
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode specifiedDeviceId = jsonTree.get("deviceId");

            if ((specifiedDeviceId != null &&
                    !specifiedDeviceId.asText().equals(deviceId)) ||
                    get(DeviceService.class).getDevice(DeviceId.deviceId(deviceId))
                            == null) {
                throw new IllegalArgumentException(DEVICE_INVALID);
            }

            jsonTree.put("deviceId", deviceId);
            final MeterRequest meterRequest = codec(MeterRequest.class)
                    .decode(jsonTree, this);

            final Meter meter = meterService.submit(meterRequest);

            UriBuilder locationBuilder = uriInfo.getBaseUriBuilder()
                    .path("meters")
                    .path(deviceId)
                    .path(Long.toString(meter.id().id()));
            return Response
                    .created(locationBuilder.build())
                    .build();
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Removes the specified meter.
     *
     * @param deviceId device identifier
     * @param meterId  meter identifier
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{deviceId}/{meterId}")
    public Response deleteMeterByDeviceIdAndMeterId(@PathParam("deviceId") String deviceId,
                                                @PathParam("meterId") String meterId) {
        DeviceId did = DeviceId.deviceId(deviceId);
        MeterId mid = MeterId.meterId(Long.valueOf(meterId));
        final Meter tmpMeter = meterService.getMeter(did, mid);
        if (tmpMeter != null) {
            final MeterRequest meterRequest = meterToMeterRequest(tmpMeter, "REMOVE");
            meterService.withdraw(meterRequest, tmpMeter.id());
        }
        return Response.noContent().build();
    }

    /**
     * Converts a meter instance to meterRequest instance with a certain operation.
     *
     * @param meter     meter instance
     * @param operation operation
     * @return converted meterRequest instance
     */
    private MeterRequest meterToMeterRequest(Meter meter, String operation) {
        MeterRequest.Builder builder;
        MeterRequest meterRequest;

        if (meter == null) {
            return null;
        }

        if (meter.isBurst()) {
            builder = DefaultMeterRequest.builder()
                    .fromApp(meter.appId())
                    .forDevice(meter.deviceId())
                    .withUnit(meter.unit())
                    .withBands(meter.bands())
                    .burst();
        } else {
            builder = DefaultMeterRequest.builder()
                    .fromApp(meter.appId())
                    .forDevice(meter.deviceId())
                    .withUnit(meter.unit())
                    .withBands(meter.bands());
        }

        switch (operation) {
            case "ADD":
                meterRequest = builder.add();
                break;
            case "REMOVE":
                meterRequest = builder.remove();
                break;
            default:
                log.warn("Invalid operation {}.", operation);
                return null;
        }

        return meterRequest;
    }
}
