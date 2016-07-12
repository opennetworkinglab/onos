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
import org.onosproject.drivers.fujitsu.behaviour.VoltPonLinkConfig;
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

/**
 * Implementation to get and set parameters available in vOLT
 * through the Netconf protocol.
 */
public class FujitsuVoltPonLinkConfig extends AbstractHandlerBehaviour
        implements VoltPonLinkConfig {

    private final Logger log = getLogger(FujitsuVoltPonLinkConfig.class);
    private final Set<String> ponLinkParams = ImmutableSet.of(
            "admin-state", "onu-discovery-mode", "onu-discovery-interval",
            "dba-cycle-time", "mac-age-time", "lof-threshold",
            "los-threshold", "pm-enable");
    private static final String VOLT_PORTS = "volt-ports";
    private static final String GPON_PONLINK_PORTS = "gpon-ponlink-ports";
    private static final String GPON_PONLINK_PORT = "gpon-ponlink-port";
    private int pon;

    @Override
    public String getPonLinks(String target) {
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
            return reply;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(VOLT_PORTS));
            if (target != null) {
                try {
                    pon = Integer.parseInt(target);
                } catch (NumberFormatException e) {
                    log.error("Non-number input");
                    return reply;
                }
                request.append(buildStartTag(GPON_PONLINK_PORTS));
                request.append(buildStartTag(GPON_PONLINK_PORT));
                request.append(buildStartTag(PONLINK_ID, false));
                request.append(target);
                request.append(buildEndTag(PONLINK_ID));

                request.append(buildEndTag(GPON_PONLINK_PORT));
                request.append(buildEndTag(GPON_PONLINK_PORTS));
            } else {
                request.append(buildEmptyTag(GPON_PONLINK_PORTS));
            }
            request.append(buildEndTag(VOLT_PORTS));
            request.append(VOLT_NE_CLOSE);

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    get(request.toString(), REPORT_ALL);
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

    @Override
    public void setPonLink(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return;
        }

        String[] data = target.split(COLON);
        if (data.length != 3) {
            log.error("Invalid number of arguments");
            return;
        }

        try {
            pon = Integer.parseInt(data[0]);
        } catch (NumberFormatException e) {
            log.error("Non-number input");
            return;
        }

        if (!ponLinkParams.contains(data[1])) {
            log.error("Unsupported parameter: {} ", data[1]);
            return;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(VOLT_PORTS));
            request.append(buildStartTag(GPON_PONLINK_PORTS));
            request.append(buildStartTag(GPON_PONLINK_PORT));
            request.append(buildStartTag(PONLINK_ID, false));
            request.append(data[0]);
            request.append(buildEndTag(PONLINK_ID));

            request.append(buildStartTag(data[1], false));
            request.append(data[2]);
            request.append(buildEndTag(data[1]));

            request.append(buildEndTag(GPON_PONLINK_PORT));
            request.append(buildEndTag(GPON_PONLINK_PORTS));
            request.append(buildEndTag(VOLT_PORTS));
            request.append(VOLT_NE_CLOSE);

            controller.getDevicesMap().get(ncDeviceId).getSession().
                    editConfig(RUNNING, null, request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
    }

}
