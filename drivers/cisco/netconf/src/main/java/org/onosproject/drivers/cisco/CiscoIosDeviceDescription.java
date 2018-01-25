/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.drivers.cisco;

import com.google.common.collect.ImmutableList;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import java.util.List;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

public class CiscoIosDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {


    private final Logger log = getLogger(getClass());
    private String version;
    private String interfaces;

    @Override
    public DeviceDescription discoverDeviceDetails() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        try {
            version = session.get(showVersionRequestBuilder());
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to retrieve version info.", e));
        }

        String[] details = TextBlockParserCisco.parseCiscoIosDeviceDetails(version);

        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);

        return new DefaultDeviceDescription(device.id().uri(), Device.Type.SWITCH,
                                            details[0], details[1],
                                            details[2], details[3],
                                            device.chassisId());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        try {
            interfaces = session.get(showInterfacesRequestBuilder());
        } catch (NetconfException e) {
            log.error("Failed to retrieve Interfaces");
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(TextBlockParserCisco.parseCiscoIosPorts(interfaces));
    }

    /**
     * Builds a request crafted to get the configuration required to create
     * details descriptions for the device.
     *
     * @return The request string.
     */
    private String showVersionRequestBuilder() {
        StringBuilder rpc = new StringBuilder("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter>");
        rpc.append("<config-format-text-block>");
        rpc.append("<text-filter-spec> | include exp_to_match_run_conf </text-filter-spec>");
        rpc.append("</config-format-text-block>");
        rpc.append("<oper-data-format-text-block>");
        rpc.append("<show>version</show>");
        rpc.append("</oper-data-format-text-block>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>]]>]]>");
        return rpc.toString();
    }

    /**
     * Builds a request crafted to get the configuration required to create
     * details descriptions for the device.
     *
     * @return The request string.
     */
    private String showInterfacesRequestBuilder() {
        //Message ID is injected later.
        StringBuilder rpc = new StringBuilder("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter>");
        rpc.append("<config-format-text-block>");
        rpc.append("<text-filter-spec> | include exp_to_match_run_conf </text-filter-spec>");
        rpc.append("</config-format-text-block>");
        rpc.append("<oper-data-format-text-block>");
        rpc.append("<show>interfaces</show>");
        rpc.append("</oper-data-format-text-block>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>]]>]]>");
        return rpc.toString();
    }

}