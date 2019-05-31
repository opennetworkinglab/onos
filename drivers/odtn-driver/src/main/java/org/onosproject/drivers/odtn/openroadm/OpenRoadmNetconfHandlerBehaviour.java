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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;



/**
 * Driver Implementation of the DeviceDescrption discovery for OpenROADM.
 */
public class OpenRoadmNetconfHandlerBehaviour extends AbstractHandlerBehaviour {

    protected static final String RPC_TAG_NETCONF_BASE =
      "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    protected static final String RPC_CLOSE_TAG = "</rpc>";

    protected static final Logger log = getLogger(OpenRoadmNetconfHandlerBehaviour.class);


    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     *
     * @return The netconf session or null
     */
    protected NetconfSession getNetconfSession(DeviceId deviceId) {
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
    protected DeviceId did() {
        return handler().data().deviceId();
    }


    /**
     * Get the device instance for which the methods apply.
     *
     * @return The device instance
     */
    protected Device getDevice() {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(did());
        return device;
    }


    /**
     * Get the device instance for which the methods apply.
     *
     * @return The device instance
     */
    protected NetconfDevice getNetconfDevice() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfDevice ncDevice = controller.getDevicesMap().get(did());
        return ncDevice;
    }


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    protected String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XPath Expression with the filter to apply in the get
     * @return a String containing the RPC XML Document
     *
     * Note: server must support xpath capability.
     */
    protected String xpathFilteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='xpath' select=\"");
        rpc.append(filter);
        rpc.append("\"/>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }
}
