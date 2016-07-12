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
import org.onosproject.drivers.fujitsu.behaviour.VoltOnuOperConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation to take actions on ONU available in vOLT
 * through the Netconf protocol.
 */
public class FujitsuVoltOnuOperConfig extends AbstractHandlerBehaviour
        implements VoltOnuOperConfig {

    private final Logger log = getLogger(FujitsuVoltOnuOperConfig.class);
    private static final String ONU_REBOOT = "onu-reboot";
    private static final String ONU_ETHPORT_LOOPBACK = "onu-ethport-loopback";
    private static final String ETHPORT_ID = "ethport-id";
    private int pon;
    private int onu;
    private int eth;


    @Override
    public String rebootOnu(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        String reply = null;
        String[] onuId = null;

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return reply;
        }

        onuId = target.split(HYPHEN);
        if (onuId.length != 2) {
            log.error("Invalid number of arguments");
            return reply;
        }
        try {
            pon = Integer.parseInt(onuId[0]);
            onu = Integer.parseInt(onuId[1]);
        } catch (NumberFormatException e) {
            log.error("Non-number input");
            return reply;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT).append(ONU_REBOOT).append(SPACE);
            request.append(VOLT_NE_NAMESPACE).append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(PONLINK_ID, false));
            request.append(onuId[0]);
            request.append(buildEndTag(PONLINK_ID));
            request.append(buildStartTag(ONU_ID, false));
            request.append(onuId[1]);
            request.append(buildEndTag(ONU_ID));
            request.append(buildEndTag(ONU_REBOOT));

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

    @Override
    public String loopbackEthOnu(String target) {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        DeviceId ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");
        String reply = null;
        String[] ethId = null;

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return reply;
        }

        ethId = target.split(HYPHEN);
        if (ethId.length != 3) {
            log.error("Invalid number of arguments");
            return reply;
        }
        try {
            pon = Integer.parseInt(ethId[0]);
            onu = Integer.parseInt(ethId[1]);
            eth = Integer.parseInt(ethId[2]);
        } catch (NumberFormatException e) {
            log.error("Non-number input");
            return reply;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT).append(ONU_ETHPORT_LOOPBACK).append(SPACE);
            request.append(VOLT_NE_NAMESPACE).append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(PONLINK_ID, false));
            request.append(ethId[0]);
            request.append(buildEndTag(PONLINK_ID));
            request.append(buildStartTag(ONU_ID, false));
            request.append(ethId[1]);
            request.append(buildEndTag(ONU_ID));
            request.append(buildStartTag(ETHPORT_ID, false));
            request.append(ethId[2]);
            request.append(buildEndTag(ETHPORT_ID));
            request.append(buildEndTag(ONU_ETHPORT_LOOPBACK));

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

}
