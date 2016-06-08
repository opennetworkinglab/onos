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

package org.onosproject.drivers.fujitsu;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onlab.packet.IpAddress;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation to get and set parameters available in VOLT NE
 * through the Netconf protocol.
 */
public class FujitsuVoltControllerConfig extends AbstractHandlerBehaviour
        implements ControllerConfig {

    private final Logger log = getLogger(FujitsuVoltControllerConfig.class);

    private static final String DOT = ".";
    private static final String VOLT_NE_NAMESPACE =
            "xmlns=\"http://fujitsu.com/ns/volt/1.1\"";
    private static final String DATA = "data";
    private static final String VOLT_NE = "volt-ne";
    private static final String VOLT_OFCONFIG = "volt-ofconfig";
    private static final String OF_CONTROLLERS = "of-controllers";
    private static final String OF_CONTROLLER = "of-controller";
    private static final String CONTROLLER_INFO = "controller-info";
    private static final String REPORT_ALL = "report-all";
    private static final String IP_ADDRESS = "ip-address";
    private static final String PORT = "port";
    private static final String PROTOCOL = "protocol";

    private static final String VOLT_NE_OPEN = "<" + VOLT_NE + " ";
    private static final String VOLT_NE_CLOSE = "</" + VOLT_NE + ">";
    private static final String VOLT_OFCONFIG_EL = "<" + VOLT_OFCONFIG + "/>\n";

    private static final String VOLT_DATACONFIG = DATA + DOT + VOLT_NE + DOT +
            VOLT_OFCONFIG + DOT + OF_CONTROLLERS + DOT + OF_CONTROLLER;

    @Override
    public List<ControllerInfo> getControllers() {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        List<ControllerInfo> controllers = new ArrayList<>();
        if (mastershipService.isLocalMaster(ncDeviceId)) {
            try {
                StringBuilder request = new StringBuilder();
                request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE).append(">\n");
                request.append(VOLT_OFCONFIG_EL);
                request.append(VOLT_NE_CLOSE);

                String reply;
                reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    get(request.toString(), REPORT_ALL);
                log.debug("Reply XML {}", reply);
                controllers.addAll(parseStreamVoltControllers(XmlConfigParser.
                    loadXml(new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8)))));
            } catch (IOException e) {
                log.error("Cannot communicate to device {} ", ncDeviceId);
            }
        } else {
            log.warn("I'm not master for {} please use master, {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
        }
        return ImmutableList.copyOf(controllers);
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        // TODO update later
        log.warn("Operation not supported");
    }

    /**
     * Parses XML string to get controller information.
     *
     * @param cfg a hierarchical configuration
     * @return a list of controllers
     */
    private List<ControllerInfo> parseStreamVoltControllers(HierarchicalConfiguration cfg) {
        List<ControllerInfo> controllers = new ArrayList<>();
        List<HierarchicalConfiguration> fields =
                cfg.configurationsAt(VOLT_DATACONFIG);

        for (HierarchicalConfiguration sub : fields) {
            List<HierarchicalConfiguration> childFields =
                    sub.configurationsAt(CONTROLLER_INFO);

            for (HierarchicalConfiguration child : childFields) {
                ControllerInfo controller = new ControllerInfo(
                        IpAddress.valueOf(child.getString(IP_ADDRESS)),
                        Integer.parseInt(child.getString(PORT)),
                        child.getString(PROTOCOL));

                log.debug("VOLT: OFCONTROLLER:  PROTOCOL={}, IP={}, PORT={} ",
                          controller.type(), controller.ip(), controller.port());
                controllers.add(controller);
            }
        }
        return controllers;
    }

}
