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

package org.onosproject.drivers.fujitsu;

import org.onosproject.drivers.fujitsu.behaviour.VoltFwdlConfig;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.slf4j.Logger;

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
    private static final String AUTO = "auto";
    private static final String COMMA = ",";

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
            return null;
        }

        String[] data = target.split(COLON);
        if ((data.length < TWO) || (data.length > THREE)) {
            log.error("Invalid number of arguments");
            return null;
        }

        String[] onuList = data[SECOND_PART].split(COMMA);
        if (onuList.length == ZERO) {
            log.error("No ONU listed");
            return null;
        }

        if ((data.length > TWO) && (!AUTO.equals(data[THIRD_PART]))) {
            log.error("Invalid reboot-mode {}", data[THIRD_PART]);
            return null;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT + ONDEMAND_FIRMWARE_UPGRADE + SPACE);
            request.append(VOLT_NE_NAMESPACE + ANGLE_RIGHT + NEW_LINE);
            request.append(buildStartTag(PARTICIPANT_LIST));

            for (count = ZERO; count < onuList.length; count++) {
                String[] onuId = onuList[count].split(HYPHEN);
                if (onuId.length != TWO) {
                    log.error("Invalid ONU identifier");
                    return null;
                }

                try {
                    int pon;
                    pon = Integer.parseInt(onuId[FIRST_PART]);
                    if (pon <= ZERO) {
                        log.error("Invalid integer for ponlink-id:{}", onuId[FIRST_PART]);
                        return null;
                    }
                    int onu;
                    onu = Integer.parseInt(onuId[SECOND_PART]);
                    if (onu <= ZERO) {
                        log.error("Invalid integer for onu-id:{}", onuId[SECOND_PART]);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    log.error("Non-number input");
                    return null;
                }

                request.append(buildStartTag(MEMBER))
                    .append(buildStartTag(PONLINK_ID))
                    .append(onuId[FIRST_PART])
                    .append(buildEndTag(PONLINK_ID))
                    .append(buildStartTag(ONU_ID))
                    .append(onuId[SECOND_PART])
                    .append(buildEndTag(ONU_ID))
                    .append(buildEndTag(MEMBER));
            }
            request.append(buildEndTag(PARTICIPANT_LIST))
                .append(buildStartTag(IMAGE_NAME))
                .append(data[FIRST_PART])
                .append(buildEndTag(IMAGE_NAME));
            if (data.length == THREE) {
                request.append(buildStartTag(REBOOT_MODE))
                    .append(data[THIRD_PART])
                    .append(buildEndTag(REBOOT_MODE));
            }
            request.append(buildEndTag(ONDEMAND_FIRMWARE_UPGRADE));

            reply = controller
                    .getDevicesMap()
                    .get(ncDeviceId)
                    .getSession()
                    .doWrappedRpc(request.toString());
        } catch (NetconfException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
        }
        return reply;
    }

}
