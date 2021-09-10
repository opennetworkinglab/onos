/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.meter.DefaultMeterRequest;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterCellId;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.meter.MeterRequest;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterService;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.rest.AbstractWebResource;

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
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Query and program meter rules.
 */
@Path("meters")
public class MetersWebResource extends AbstractWebResource {

    @Context
    private UriInfo uriInfo;

    private static final String DEVICE_INVALID = "Invalid deviceId in meter creation request";
    private static final String METER_NOT_FOUND = "Meter is not found for ";

    private final ObjectNode root = mapper().createObjectNode();
    private final ArrayNode metersNode = root.putArray("meters");

    private static final String REST_APP_ID = "org.onosproject.rest";
    private ApplicationId applicationId;

    /**
     * Returns all meters of all devices.
     *
     * @return 200 OK with array of all the meters in the system
     * @onos.rsModel Meters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMeters() {
        MeterService meterService = get(MeterService.class);
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
        MeterService meterService = get(MeterService.class);
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
        MeterCellId mid = MeterId.meterId(Long.valueOf(meterId));
        MeterService meterService = get(MeterService.class);
        final Meter meter = nullIsNotFound(meterService.getMeter(did, mid),
                METER_NOT_FOUND + mid);

        metersNode.add(codec(Meter.class).encode(meter, this));
        return ok(root).build();
    }

    /**
     * Returns a meter by the meter cell id.
     *
     * @param deviceId device identifier
     * @param scope scope identifier
     * @param index index
     * @return 200 OK with a meter, return 404 if no entry has been found
     * @onos.rsModel Meter
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{deviceId}/{scope}/{index}")
    public Response getMeterByDeviceIdAndMeterCellId(@PathParam("deviceId") String deviceId,
                                                     @PathParam("scope") String scope,
                                                     @PathParam("index") String index) {
        DeviceId did = DeviceId.deviceId(deviceId);
        MeterScope meterScope = MeterScope.of(scope);
        long meterIndex = Long.parseLong(index);
        MeterCellId meterCellId;
        if (meterScope.equals(MeterScope.globalScope())) {
            meterCellId = MeterId.meterId(meterIndex);
        } else {
            meterCellId = PiMeterCellId.ofIndirect(PiMeterId.of(meterScope.id()), meterIndex);
        }

        MeterService meterService = get(MeterService.class);
        final Meter meter = nullIsNotFound(meterService.getMeter(did, meterCellId),
                METER_NOT_FOUND + meterCellId);

        metersNode.add(codec(Meter.class).encode(meter, this));
        return ok(root).build();
    }

    /**
     * Returns a collection of meters by the device id and meter scope.
     *
     * @param deviceId device identifier
     * @param scope scope identifier
     * @return 200 OK with array of meters which belongs to specified device
     * @onos.rsModel Meters
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("scope/{deviceId}/{scope}")
    public Response getMetersByDeviceIdAndScope(@PathParam("deviceId") String deviceId,
                                                @PathParam("scope") String scope) {
        DeviceId did = DeviceId.deviceId(deviceId);
        MeterScope meterScope = MeterScope.of(scope);
        MeterService meterService = get(MeterService.class);
        final Iterable<Meter> meters = meterService.getMeters(did, meterScope);
        if (meters != null) {
            meters.forEach(meter -> metersNode.add(codec(Meter.class).encode(meter, this)));
        }
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
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
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

            MeterService meterService = get(MeterService.class);
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
     * Removes the meter by device id and meter id.
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
        MeterCellId mid = MeterId.meterId(Long.valueOf(meterId));
        MeterRequest meterRequest = deleteRequest(did);

        MeterService meterService = get(MeterService.class);
        meterService.withdraw(meterRequest, mid);

        return Response.noContent().build();
    }

    /**
     * Removes the meter by the device id and meter cell id.
     *
     * @param deviceId device identifier
     * @param scope scope identifier
     * @param index index
     * @return 204 NO CONTENT
     */
    @DELETE
    @Path("{deviceId}/{scope}/{index}")
    public Response deleteMeterByDeviceIdAndMeterCellId(@PathParam("deviceId") String deviceId,
                                                        @PathParam("scope") String scope,
                                                        @PathParam("index") String index) {
        DeviceId did = DeviceId.deviceId(deviceId);
        MeterScope meterScope = MeterScope.of(scope);
        long meterIndex = Long.parseLong(index);
        MeterCellId meterCellId;
        if (meterScope.equals(MeterScope.globalScope())) {
            meterCellId = MeterId.meterId(meterIndex);
        } else {
            meterCellId = PiMeterCellId.ofIndirect(PiMeterId.of(meterScope.id()), meterIndex);
        }
        MeterRequest meterRequest = deleteRequest(did);

        MeterService meterService = get(MeterService.class);
        meterService.withdraw(meterRequest, meterCellId);

        return Response.noContent().build();
    }

    private MeterRequest deleteRequest(DeviceId did) {
        CoreService coreService = getService(CoreService.class);
        if (applicationId == null) {
            applicationId = coreService.registerApplication(REST_APP_ID);
        }

        return DefaultMeterRequest.builder()
                .forDevice(did)
                .fromApp(applicationId)
                .remove();
    }
}
