/*
 * Copyright 2019-present Open Networking Foundation
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
 * This Work is contributed by Sterlite Technologies
 */
package org.onosproject.rest.resources;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BitErrorRateState;
import org.onosproject.net.device.DeviceService;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/*
 * Rest APIs to Fetch the BER(bit error rate) pre/post FEC(Forward Error Correction).
 */
@Path("bit-error-rate")
public class BitErrorRateWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DEVICE_NOT_FOUND = "Device is not found";
    private static final String BER_UNSUPPORTED = "Bit Error Rate is not supported";
    private static final String PRE_FEC_BER = "pre-fec-ber";
    private static final String POST_FEC_BER = "post-fec-ber";

    /*
     * This method returns the implemenation of BitErrorRateConfig interface.
     *
     * @param id the device identifier
     * @return instance of BitErrorRateConfig driver implementation
     */
    private BitErrorRateState getBitErrorRateState(String id) {
        Device device = get(DeviceService.class).getDevice(DeviceId.deviceId(id));
        if (device == null) {
            throw new IllegalArgumentException(DEVICE_NOT_FOUND);
        }
        if (device.is(BitErrorRateState.class)) {
            return device.as(BitErrorRateState.class);
        }
        return null;
    }

    /*
     * Get Request to fetch the BER value before FEC.
     *
     * @param connectPointStr connectPoint (device/portNumber)
     * @return Json Response with pre FEC,BER value
     */
    @GET
    @Path("{connectPoint}/pre_fec_ber")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPreFecBerValue(@PathParam("connectPoint") String connectPointStr) {
        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(connectPointStr);
        BitErrorRateState bitErrorRateState = getBitErrorRateState(connectPoint.deviceId().toString());

        if (bitErrorRateState == null) {
            throw new IllegalArgumentException(BER_UNSUPPORTED);
        }
        ObjectNode result = encode(connectPoint.deviceId(), connectPoint.port(),
                                   bitErrorRateState, PRE_FEC_BER);
        return ok(result).build();
    }

    /*
     * Get Request to fetch the BER value after FEC.
     *
     * @param connectPointStr connectPoint (device/portNumber)
     * @return Json Response with post FEC,BER value
     */
    @GET
    @Path("{connectPoint}/post_fec_ber")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPostFecBerValue(@PathParam("connectPoint") String connectPointStr) {
        ConnectPoint connectPoint = ConnectPoint.deviceConnectPoint(connectPointStr);
        BitErrorRateState bitErrorRateState = getBitErrorRateState(connectPoint.deviceId().toString());

        if (bitErrorRateState == null) {
            throw new IllegalArgumentException(BER_UNSUPPORTED);
        }
        ObjectNode result = encode(connectPoint.deviceId(), connectPoint.port(),
                                   bitErrorRateState, POST_FEC_BER);
        return ok(result).build();
    }

    /*
     * This method generates the Json Response for BER.
     *
     * @param id device identifier
     * @param portId port identifier
     * @param bitErrorRateConfig BitErrorRateConfig object
     * @param type String value to differentiate Pre/Post Fec, Ber value
     * @return ObjectNode instance
     */
    private ObjectNode encode(DeviceId deviceId, PortNumber port,
                              BitErrorRateState bitErrorRateConfig,
                              String type) {
        double ber = 0.0;

        if (PRE_FEC_BER.equals(type)) {
            ber = bitErrorRateConfig.getPreFecBer(deviceId, port).get();
        } else if (POST_FEC_BER.equals(type)) {
            ber = bitErrorRateConfig.getPostFecBer(deviceId, port).get();
        } else {
            log.error("Invalid type");
            throw new IllegalArgumentException("Invalid type");
        }
        ObjectNode responseNode = mapper().createObjectNode();

        responseNode.put("deviceId", deviceId.toString());
        responseNode.put("portId", port.toString());
        responseNode.put("ber", ber);

        return responseNode;
    }
}
