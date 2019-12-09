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
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.Range;
import org.onosproject.drivers.odtn.openconfig.TerminalDevicePowerConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 * Currently works only with PSI-2T.
 * If you want to make it work with ROADM, you need to implement this interface again.
 *
 */
public class NokiaTerminalDevicePowerConfig<T>
        extends TerminalDevicePowerConfig<T> {

    private static final String OPTICAL_CHANNEL = "OCH";

    private static final Logger log = getLogger(NokiaTerminalDevicePowerConfig.class);

    //The username and password are different from the username and password in the netconf-cfg file
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "admin";

    /**
     * Login to the device by providing the correct user and password in order to configure the device
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device identifier
     * @param userName username to access the device
     * @param passwd password to access the device
     * @return The netconf session or null
     */
    @Override
    public NetconfSession getNetconfSession(DeviceId deviceId, String userName, String passwd) {
        userName = USER_NAME;
        passwd = PASSWORD;
        NetconfController nc = handler().get(NetconfController.class);
        NetconfDevice ndev = nc.getDevicesMap().get(deviceId);
        if (ndev == null) {
            log.debug("NetConf device " + deviceId + " is not found, returning null session");
            return null;
        }
        NetconfSession ns = ndev.getSession();
        if (ns == null) {
            log.error("discoverPorts called with null session for \n {}", deviceId);
            return null;
        }

        try {
            String reply = ns.requestSync(buildLoginRpc(userName, passwd));
            if (reply.contains("<ok/>")) {
                return ns;
            } else {
                log.debug("Reply contains this: \n {}", reply);
                return null;
            }
        } catch (NetconfException e) {
            log.error("Can NOT login to the device", e);
        }
        return ns;
    }

    /**
     * Construct a rpc login message.
     *
     * @param userName username to access the device
     * @param passwd password to access the device
     * @return RPC message
     */
    private String buildLoginRpc(String userName, String passwd) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<login xmlns=\"http://nokia.com/yang/nokia-security\">");
        rpc.append("<username>");
        rpc.append(userName);
        rpc.append("</username>");
        rpc.append("<password>");
        rpc.append(passwd);
        rpc.append("</password>");
        rpc.append("</login>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    //crude way of removing rpc-reply envelope (copy from netconf session)
    private String getDataOfRpcReply(String rpcReply) {
        String data = null;
        int begin = rpcReply.indexOf("<data>");
        int end = rpcReply.lastIndexOf("</data>");
        if (begin != -1 && end != -1) {
            data = (String) rpcReply.subSequence(begin, end + "</data>".length());
        } else {
            data = rpcReply;
        }
        return data;
    }

    /**
     *
     * @param param the config parameter.
     * @return array of string
     */
    @Override
    public Map<String, String> buildRpcString(String param) {
        Map<String, String> rpcMap = new HashMap<String, String>();
        switch (param) {
            case TARGET_POWER:
                rpcMap.put("TARGET_OUTPUT_PATH", "components/component/oc-opt-term:optical-channel/oc-opt-term:config");
                rpcMap.put("TARGET_OUTPUT_LEAF", "oc-opt-term:target-output-power");
            case CURRENT_POWER:
                rpcMap.put("CURRENT_POWER_PATH", "components.component." +
                                                            "oc-opt-term:optical-channel." +
                                                            "oc-opt-term:state." +
                                                            "oc-opt-term:output-power");
                rpcMap.put("CURRENT_POWER_ROUTE", "<oc-opt-term:output-power>" +
                                                            "<oc-opt-term:instant/>" +
                                                            "</oc-opt-term:output-power>");
                rpcMap.put("CURRENT_POWER_LEAF", "oc-opt-term:instant");
            default:
                rpcMap.put("CURRENT_INPUT_POWER_PATH", "components.component." +
                                                                "oc-opt-term:optical-channel." +
                                                                "oc-opt-term:state." +
                                                                "oc-opt-term:input-power");
                rpcMap.put("CURRENT_INPUT_POWER_ROUTE", "<oc-opt-term:input-power>" +
                                                                "<oc-opt-term:instant/>" +
                                                                "</oc-opt-term:input-power>");
                rpcMap.put("CURRENT_INPUT_POWER_LEAF", "oc-opt-term:instant");
        }
            return rpcMap;
    }

    /**
     * Construct a rpc target power message.
     *
     * @param filter to build rpc
     * @return RPC payload
     */
    @Override
    public StringBuilder getTargetPowerRequestRpc(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<get>")
                .append("<filter type='subtree'>")
                .append(filter)
                .append("</filter>")
                .append("</get>");
        return rpc;
    }

    /**
     * Construct a rpc target power message.
     *
     * @return RPC payload
     */
    @Override
    public DatastoreId getDataStoreId() {
        return DatastoreId.RUNNING;
    }

    /**
     * Construct a rpc target power message.
     *
     * @param name
     * @param underState to build rpc for setting configuration
     * @return RPC payload
     */
    @Override
    public StringBuilder getOpticalChannelStateRequestRpc(String name, String underState) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<name>").append(name).append("</name>")
                .append("<oc-opt-term:optical-channel " +
                                "xmlns:oc-opt-term=\"http://openconfig.net/yang/terminal-device\">")
                .append("<oc-opt-term:state>")
                .append(underState)
                .append("</oc-opt-term:state>")
                .append("</oc-opt-term:optical-channel>")
                .append("</component></components></filter></get>");
        return rpc;
    }

    /**
     * Getting target value of output power.
     * @param port port
     * @param component the component
     * @return target output power range
     */
    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
        double targetMin = -20;
        double targetMax = 5;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, Object component) {
        double targetMin = -20;
        double targetMax = 5;
        return Optional.of(Range.open(targetMin, targetMax));
    }

    /**
     * Construct a rpc target power message.
     *
     * @param name for optical channel name
     * @param power to build rpc for setting configuration
     * @return RPC payload
     */
    @Override
    public StringBuilder parsePortRequestRpc(Double power, String name) {
        if (name != null) {
            StringBuilder rpc = new StringBuilder();
            rpc.append("<component>").append("<name>").append(name).append("</name>");
            if (power != null) {
                // This is an edit-config operation.
                rpc.append("<oc-opt-term:optical-channel " +
                                  "xmlns:oc-opt-term=\"http://openconfig.net/yang/terminal-device\">")
                        .append("<oc-opt-term:config>")
                        .append("<oc-opt-term:target-output-power>")
                        .append(power)
                        .append("</oc-opt-term:target-output-power>")
                        .append("</oc-opt-term:config>")
                        .append("</oc-opt-term:optical-channel>");
            }
            return rpc;
        }
        return null;
    }

}
