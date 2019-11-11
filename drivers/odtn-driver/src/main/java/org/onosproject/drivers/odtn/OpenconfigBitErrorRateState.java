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
package org.onosproject.drivers.odtn;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.drivers.odtn.util.NetconfSessionUtility;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BitErrorRateState;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/*
 * Driver Implementation of the BitErrorRateConfig for OpenConfig terminal devices.
 */
public abstract class OpenconfigBitErrorRateState extends AbstractHandlerBehaviour implements BitErrorRateState {

    private static final Logger log = LoggerFactory.getLogger(OpenconfigBitErrorRateState.class);

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";
    private static final String PRE_FEC_BER_TAG = "pre-fec-ber";
    private static final String POST_FEC_BER_TAG = "post-fec-ber";

    /*
     * This method returns the instance of NetconfController from DriverHandler.
     */
    private NetconfController getController() {
        return handler().get(NetconfController.class);
    }

    /*
     * This method is used for getting Openconfig Component name.
     * from DeviceService port annotations by device specific key for component name
     *
     * @param deviceId the device identifier
     * @param port the port identifier
     * @return String value representing Openconfig component name
     */
    protected abstract String ocName(DeviceId deviceId, PortNumber port);

    /**
     * Get the BER value pre FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    @Override
    public Optional<Double> getPreFecBer(DeviceId deviceId, PortNumber port) {
        NetconfSession session = NetconfSessionUtility
                .getNetconfSession(deviceId, getController());
        checkNotNull(session);

        String preFecBerFilter = generateBerFilter(deviceId, port, PRE_FEC_BER_TAG);
        String rpcRequest = getConfigOperation(preFecBerFilter);
        log.debug("RPC call for fetching Pre FEC BER : {}", rpcRequest);

        XMLConfiguration xconf = NetconfSessionUtility.executeRpc(session, rpcRequest);

        if (xconf == null) {
            log.error("Error in executing Pre FEC BER RPC");
            return Optional.empty();
        }

        try {
            HierarchicalConfiguration config =
                    xconf.configurationAt("data/components/component/transceiver/state/" + PRE_FEC_BER_TAG);
            if (config == null || config.getString("instant") == null) {
                return Optional.empty();
            }
            double ber = Float.valueOf(config.getString("instant")).doubleValue();
            return Optional.of(ber);

        } catch (IllegalArgumentException e) {
            log.error("Error in fetching configuration : {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Get the BER value post FEC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @return the decimal value of BER
     */
    @Override
    public Optional<Double> getPostFecBer(DeviceId deviceId, PortNumber port) {
        NetconfSession session = NetconfSessionUtility
                .getNetconfSession(deviceId, getController());
        checkNotNull(session);

        String postFecBerFilter = generateBerFilter(deviceId, port, POST_FEC_BER_TAG);
        String rpcRequest = getConfigOperation(postFecBerFilter);
        log.debug("RPC call for fetching Post FEC BER : {}", rpcRequest);

        XMLConfiguration xconf = NetconfSessionUtility.executeRpc(session, rpcRequest);

        if (xconf == null) {
            log.error("Error in executing Post FEC BER RPC");
            return Optional.empty();
        }

        try {
            HierarchicalConfiguration config =
                    xconf.configurationAt("data/components/component/transceiver/state/" + POST_FEC_BER_TAG);
            if (config == null || config.getString("instant") == null) {
                return Optional.empty();
            }
            double ber = Float.valueOf(config.getString("instant")).doubleValue();
            return Optional.of(ber);

        } catch (IllegalArgumentException e) {
            log.error("Error in fetching configuration : {}", e.getMessage());
            return Optional.empty();
        }
    }

    /*
     * This method is used for generating RPC for Netconf getconfig operation.
     *
     * @param filter the String representing the <filter> tag in Netconf RPC
     * @return the String value representing Netconf getConfig Operation
     */
    private String getConfigOperation(String filter) {
        StringBuilder rpcRequest = new StringBuilder();

        rpcRequest.append(RPC_TAG_NETCONF_BASE)
                .append("<get>")
                .append("<filter>")
                .append(filter)
                .append("</filter>")
                .append("</get>")
                .append(RPC_CLOSE_TAG);

        return rpcRequest.toString();
    }

    /**
     * Generate the BER filter to be used in Netconf get-operation RPC.
     *
     * @param deviceId the device identifier
     * @param port     the port identifier
     * @param tag      the String value to identify the preFecBer / PostFecBer
     * @return the String value representing the BER filter
     */
    private String generateBerFilter(DeviceId deviceId, PortNumber port, String tag) {
        StringBuilder filter = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
        filter.append("<component>");
        filter.append("<name>").append(ocName(deviceId, port)).append("</name>");
        filter.append("<transceiver xmlns=\"http://openconfig.net/yang/platform/transceiver\">");
        filter.append("<state>");
        if (PRE_FEC_BER_TAG.equals(tag)) {
            filter.append("<pre-fec-ber>").append("<instant/>").append("</pre-fec-ber>");
        } else if (POST_FEC_BER_TAG.equals(tag)) {
            filter.append("<post-fec-ber>").append("<instant/>").append("</post-fec-ber>");
        }
        filter.append("</state>");
        filter.append("</transceiver>");
        filter.append("</component>");
        filter.append("</components>");

        return filter.toString();
    }
}
