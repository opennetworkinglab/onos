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

import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.drivers.fujitsu.behaviour.VoltNniLinkConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtility.*;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.netconf.TargetConfig.RUNNING;

/**
 * Implementation to get and set parameters available in vOLT
 * through the Netconf protocol.
 */
public class FujitsuVoltNniLinkConfig extends AbstractHandlerBehaviour
        implements VoltNniLinkConfig {

    private final Logger log = getLogger(FujitsuVoltNniLinkConfig.class);
    private static final Set<String> NNILINKPARAMS =
            ImmutableSet.of("loopback-enable");
    private static final Set<String> ENABLES = ImmutableSet.of("true", "false");
    private static final String VOLT_PORTS = "volt-ports";
    private static final String ETH_NNILINK_PORTS = "eth-nnilink-ports";
    private static final String ETH_NNILINK_PORT = "eth-nnilink-port";
    private static final String NNILINK_ID = "nnilink-id";

    @Override
    public String getNniLinks(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        String reply = null;

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return null;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE)
                .append(ANGLE_RIGHT + NEW_LINE)
                .append(buildStartTag(VOLT_PORTS));
            if (target != null) {
                int nni;
                try {
                    nni = Integer.parseInt(target);
                    if (nni <= ZERO) {
                        log.error("Invalid integer for nnilink-id:{}", target);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    log.error("Non-number input for nnilink-id:{}", target);
                    return null;
                }
                request.append(buildStartTag(ETH_NNILINK_PORTS))
                    .append(buildStartTag(ETH_NNILINK_PORT))
                    .append(buildStartTag(NNILINK_ID, false))
                    .append(target)
                    .append(buildEndTag(NNILINK_ID))
                    .append(buildEndTag(ETH_NNILINK_PORT))
                    .append(buildEndTag(ETH_NNILINK_PORTS));
            } else {
                request.append(buildEmptyTag(ETH_NNILINK_PORTS));
            }
            request.append(buildEndTag(VOLT_PORTS))
                .append(VOLT_NE_CLOSE);

            reply = controller.getDevicesMap()
                        .get(ncDeviceId)
                        .getSession()
                        .get(request.toString(), REPORT_ALL);
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
        }
        return reply;
    }

    @Override
    public boolean setNniLink(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return false;
        }

        String[] data = target.split(COLON);
        if (data.length != THREE) {
            log.error("Invalid number of arguments {}", target);
            return false;
        }

        try {
            int nni = Integer.parseInt(data[FIRST_PART]);
            if (nni <= ZERO) {
                log.error("Invalid integer for nnilink-id:{}", target);
                return false;
            }
        } catch (NumberFormatException e) {
            log.error("Non-number input for nnilink-id:{}", target);
            return false;
        }

        if (!checkSetParam(data[SECOND_PART], data[THIRD_PART])) {
            log.error("Failed to check input {}", target);
            return false;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE)
                .append(ANGLE_RIGHT + NEW_LINE)
                .append(buildStartTag(VOLT_PORTS))
                .append(buildStartTag(ETH_NNILINK_PORTS))
                .append(buildStartTag(ETH_NNILINK_PORT))
                .append(buildStartTag(NNILINK_ID, false))
                .append(data[FIRST_PART])
                .append(buildEndTag(NNILINK_ID))

                .append(buildStartTag(data[SECOND_PART], false))
                .append(data[THIRD_PART])
                .append(buildEndTag(data[SECOND_PART]))

                .append(buildEndTag(ETH_NNILINK_PORT))
                .append(buildEndTag(ETH_NNILINK_PORTS))
                .append(buildEndTag(VOLT_PORTS))
                .append(VOLT_NE_CLOSE);

            controller.getDevicesMap()
                .get(ncDeviceId)
                .getSession()
                .editConfig(RUNNING, null, request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
            return false;
        }
        return true;
    }

    /**
     * Verifies input string for valid options.
     *
     * @param name input data in string
     * @param value input data in string
     * @return true/false if the param is valid/invalid
     */
    private boolean checkSetParam(String name, String value) {
        if (!NNILINKPARAMS.contains(name)) {
            log.error("Unsupported parameter: {}", name);
            return false;
        }

        switch (name) {
            default:
                if (!ENABLES.contains(value)) {
                    log.error("Invalid value for Name {} : Value {}.", name, value);
                    return false;
                }
                break;
        }
        return true;
    }

}
