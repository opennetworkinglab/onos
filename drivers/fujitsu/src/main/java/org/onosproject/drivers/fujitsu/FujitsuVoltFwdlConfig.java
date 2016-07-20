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

import org.onosproject.drivers.fujitsu.behaviour.VoltFwdlConfig;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation to upgrade firmware in ONUs manually
 * through the Netconf protocol.
 */
public class FujitsuVoltFwdlConfig extends AbstractHandlerBehaviour
        implements VoltFwdlConfig {

    private final Logger log = getLogger(FujitsuVoltFwdlConfig.class);
    private static final String ONDEMAND_FIRMWARE_UPGRADE = "ondemand-firmware-upgrade";
    private static final String PARTICIPANT_LIST = "participant-list";
    private static final String MEMBER = "member";
    private static final String IMAGE_NAME = "image-name";
    private static final String REBOOT_MODE = "reboot-mode";
    private int pon;
    private int onu;


    @Override
    public String upgradeFirmwareOndemand(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        String reply = null;
        int count;

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return reply;
        }

        String[] data = target.split(":");
        if ((data.length < 2) || (data.length > 3)) {
            log.error("Invalid number of arguments");
            return reply;
        }

        String[] onuList = data[1].split(",");
        if (onuList.length == 0) {
            log.error("No ONU listed");
            return reply;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT).append(ONDEMAND_FIRMWARE_UPGRADE).append(SPACE);
            request.append(VOLT_NE_NAMESPACE).append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(PARTICIPANT_LIST));

            for (count = 0; count < onuList.length; count++) {
                String[] onuId = onuList[count].split("-");
                if (onuId.length != 2) {
                    log.error("Invalid ONU identifier");
                    return reply;
                }

                try {
                    pon = Integer.parseInt(onuId[0]);
                    onu = Integer.parseInt(onuId[1]);
                } catch (NumberFormatException e) {
                    log.error("Non-number input");
                    return reply;
                }

                request.append(buildStartTag(MEMBER));
                request.append(buildStartTag(PONLINK_ID));
                request.append(onuId[0]);
                request.append(buildEndTag(PONLINK_ID));
                request.append(buildStartTag(ONU_ID));
                request.append(onuId[1]);
                request.append(buildEndTag(ONU_ID));
                request.append(buildEndTag(MEMBER));
            }
            request.append(buildEndTag(PARTICIPANT_LIST));
            request.append(buildStartTag(IMAGE_NAME));
            request.append(data[0]);
            request.append(buildEndTag(IMAGE_NAME));
            if (data.length == 3) {
                request.append(buildStartTag(REBOOT_MODE));
                request.append(data[2]);
                request.append(buildEndTag(REBOOT_MODE));
            }
            request.append(buildEndTag(ONDEMAND_FIRMWARE_UPGRADE));

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

}
