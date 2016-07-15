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

import org.onosproject.net.DeviceId;
import org.onosproject.drivers.fujitsu.behaviour.VoltAlertConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.mastership.MastershipService;
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
public class FujitsuVoltAlertConfig extends AbstractHandlerBehaviour
        implements VoltAlertConfig {

    private final Logger log = getLogger(FujitsuVoltAlertConfig.class);
    private static final String VOLT_ALERTS = "volt-alerts";
    private static final String ALERT_FILTER = "alert-filter";
    private static final String NOTIFY_ALERT = "notify-alert";
    private final Set<String> severityLevels = ImmutableSet.of(
            "none", "info", "minor", "major", "critical");
    private static final String DISABLE = "disable";


    @Override
    public String getAlertFilter() {
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
            request.append(buildStartTag(VOLT_ALERTS));
            request.append(buildEmptyTag(ALERT_FILTER));
            request.append(buildEndTag(VOLT_ALERTS));
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
    public void setAlertFilter(String severity) {
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

        if (!severityLevels.contains(severity)) {
            log.error("Invalid severity level: " + severity);
            return;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(VOLT_ALERTS));
            request.append(buildStartTag(ALERT_FILTER, false));
            request.append(severity);
            request.append(buildEndTag(ALERT_FILTER));
            request.append(buildEndTag(VOLT_ALERTS));
            request.append(VOLT_NE_CLOSE);

            controller.getDevicesMap().get(ncDeviceId).getSession().
                    editConfig(RUNNING, null, request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
    }

    @Override
    public void subscribe(String mode) {
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

        if (mode != null) {
            if (!DISABLE.equals(mode)) {
                log.error("Invalid mode: " + mode);
                return;
            }
        }

        try {
            if (mode != null) {
                controller.getDevicesMap().get(ncDeviceId).getSession().
                        endSubscription();
            } else {
                StringBuilder request = new StringBuilder();
                request.append(ANGLE_LEFT).append(NOTIFY_ALERT).append(SPACE);
                request.append(VOLT_NE_NAMESPACE).append(SLASH).append(ANGLE_RIGHT);

                controller.getDevicesMap().get(ncDeviceId).getSession().
                        startSubscription(request.toString());
            }
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
    }

}
