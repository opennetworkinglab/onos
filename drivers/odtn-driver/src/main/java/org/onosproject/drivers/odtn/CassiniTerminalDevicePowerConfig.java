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
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.collect.Range;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 *
 */
public class CassiniTerminalDevicePowerConfig
        extends AbstractHandlerBehaviour implements PowerConfig<OchSignal> {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final long NO_POWER = -50;

    private static final Logger log = getLogger(CassiniTerminalDevicePowerConfig.class);

    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     *
     * @return The netconf session or null
     */
    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfController controller = handler().get(NetconfController.class);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
    }

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    /**
     * Parse filtering string from port and component.
     * @param portNumber Port Number
     * @param component port component (optical-channel)
     * @param power power value set.
     * @return filtering string in xml format
     */
    private String parsePort(PortNumber portNumber, OchSignal component, Long power) {
        if (component == null) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            DeviceId deviceId = handler().data().deviceId();
            String name = deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");
            StringBuilder sb = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
            sb.append("<component>").append("<name>").append(name).append("</name>");
            if (power != null) {
                // This is an edit-config operation.
                sb.append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">")
                        .append("<config>")
                        .append("<target-output-power>")
                        .append(power)
                        .append("</target-output-power>")
                        .append("</config>")
                        .append("</optical-channel>");
            }
            sb.append("</component>").append("</components>");
            return sb.toString();
        } else {
            log.error("Cannot process the component {}.", component.getClass());
            return null;
        }
    }

    /**
     * Execute RPC request.
     * @param session Netconf session
     * @param message Netconf message in XML format
     * @return XMLConfiguration object
     */
    private XMLConfiguration executeRpc(NetconfSession session, String message) {
        try {
            CompletableFuture<String> fut = session.rpc(message);
            String rpcReply = fut.get();
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(new XPathExpressionEngine());
            return xconf;
        } catch (NetconfException ne) {
            log.error("Exception on Netconf protocol: {}.", ne);
        } catch (InterruptedException ie) {
            log.error("Interrupted Exception: {}.", ie);
        } catch (ExecutionException ee) {
            log.error("Concurrent Exception while executing Netconf operation: {}.", ee);
        }
        return null;
    }

    /**
     * Get the target-output-power value on specific optical-channel.
     * @param port the port
     * @param component the port component. It should be 'oc-name' in the Annotations of Port.
     *                  'oc-name' could be mapped to <component><name> in openconfig yang.
     * @return target power value
     */
    @Override
    public Optional<Long> getTargetPower(PortNumber port, OchSignal component) {
        NetconfSession session = getNetconfSession(did());
        if (session == null) {
            log.error("discoverPortDetails called with null session for {}", did());
            return Optional.of(NO_POWER);
        }

        String filter = parsePort(port, component, null);
        StringBuilder rpcReq = new StringBuilder();
        rpcReq.append(RPC_TAG_NETCONF_BASE)
                .append("<get>")
                .append("<filter type='subtree'>")
                .append(filter)
                .append("</filter>")
                .append("</get>")
                .append(RPC_CLOSE_TAG);
        XMLConfiguration xconf = executeRpc(session, rpcReq.toString());
        HierarchicalConfiguration config =
                xconf.configurationAt("data/components/component/optical-channel/config");
        long power = Float.valueOf(config.getString("target-output-power")).longValue();
        return Optional.of(power);
    }

    @Override
    public void setTargetPower(PortNumber port, OchSignal component, long power) {
        NetconfSession session = getNetconfSession(did());
        if (session == null) {
            log.error("setTargetPower called with null session for {}", did());
            return;
        }
        String editConfig = parsePort(port, component, power);
        StringBuilder rpcReq = new StringBuilder();
        rpcReq.append(RPC_TAG_NETCONF_BASE)
                .append("<edit-config>")
                .append("<target><running/></target>")
                .append("<config>")
                .append(editConfig)
                .append("</config>")
                .append("</edit-config>")
                .append(RPC_CLOSE_TAG);
        XMLConfiguration xconf = executeRpc(session, rpcReq.toString());
        // The successful reply should be "<rpc-reply ...><ok /></rpc-reply>"
        if (!xconf.getRoot().getChild(0).getName().equals("ok")) {
            log.error("The <edit-config> operation to set target-output-power of Port({}:{}) is failed.",
                    port.toString(), component.toString());
        }
    }

    @Override
    public Optional<Long> currentPower(PortNumber port, OchSignal component) {
        // FIXME
        log.warn("Not Implemented Yet!");
        return Optional.empty();
    }

    @Override
    public Optional<Range<Long>> getTargetPowerRange(PortNumber port, OchSignal component) {
        // FIXME
        log.warn("Not Implemented Yet!");
        return Optional.empty();
    }

    @Override
    public Optional<Range<Long>> getInputPowerRange(PortNumber port, OchSignal component) {
        // FIXME
        log.warn("Not Implemented Yet!");
        return Optional.empty();
    }

    @Override
    public List<PortNumber> getPorts(OchSignal component) {
        // FIXME
        log.warn("Not Implemented Yet!");
        return new ArrayList<PortNumber>();
    }
}
