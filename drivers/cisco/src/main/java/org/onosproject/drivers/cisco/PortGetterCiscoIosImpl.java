/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.net.behaviour.PortDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Retrieves the ports from Cisco IOS devices via netconf.
 */
public class PortGetterCiscoIosImpl extends AbstractHandlerBehaviour
        implements PortDiscovery {

    private final Logger log = getLogger(getClass());
    private String interfaces;

    @Override
    public List<PortDescription> getPorts() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        try {
            interfaces = session.get(showInterfacesRequestBuilder());
        } catch (IOException e) {
            log.error("Failed to retrieve Interfaces");
            return ImmutableList.of();
        }
        return ImmutableList.copyOf(TextBlockParserCisco.parseCiscoIosPorts(interfaces));
    }

    /**
     * Builds a request crafted to get the configuration required to create port
     * descriptions for the device.
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