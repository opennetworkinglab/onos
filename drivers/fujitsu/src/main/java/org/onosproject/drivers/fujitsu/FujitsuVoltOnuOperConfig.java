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

import com.google.common.collect.ImmutableSet;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.drivers.fujitsu.behaviour.VoltOnuOperConfig;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Set;

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
    private static final String LOOPBACK_MODE = "mode";
    private static final Set<String> LOOPBACKMODES =
            ImmutableSet.of("operate", "release");

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
            return null;
        }

        onuId = checkIdString(target, TWO);
        if (onuId == null) {
            log.error("Invalid ONU identifier {}", target);
            return null;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT + ONU_REBOOT + SPACE);
            request.append(VOLT_NE_NAMESPACE + ANGLE_RIGHT + NEW_LINE);

            request.append(buildStartTag(PONLINK_ID, false))
                .append(onuId[FIRST_PART])
                .append(buildEndTag(PONLINK_ID))
                .append(buildStartTag(ONU_ID, false))
                .append(onuId[SECOND_PART])
                .append(buildEndTag(ONU_ID))
                .append(buildEndTag(ONU_REBOOT));

            reply = controller
                    .getDevicesMap()
                    .get(ncDeviceId)
                    .getSession()
                    .doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
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
        String[] data = null;
        String[] ethId = null;

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return null;
        }

        data = target.split(COLON);
        if (data.length > TWO) {
            log.error("Invalid number of parameters {}", target);
            return null;
        }

        ethId = checkIdString(data[FIRST_PART], THREE);
        if (ethId == null) {
            log.error("Invalid ETH port identifier {}", data[FIRST_PART]);
            return null;
        }

        if (data.length > ONE) {
            if (!LOOPBACKMODES.contains(data[SECOND_PART])) {
                log.error("Unsupported parameter: {}", data[SECOND_PART]);
                return null;
            }
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT + ONU_ETHPORT_LOOPBACK + SPACE);
            request.append(VOLT_NE_NAMESPACE + ANGLE_RIGHT + NEW_LINE);

            request.append(buildStartTag(PONLINK_ID, false))
                .append(ethId[FIRST_PART])
                .append(buildEndTag(PONLINK_ID))
                .append(buildStartTag(ONU_ID, false))
                .append(ethId[SECOND_PART])
                .append(buildEndTag(ONU_ID))
                .append(buildStartTag(ETHPORT_ID, false))
                .append(ethId[THIRD_PART])
                .append(buildEndTag(ETHPORT_ID));
            if (data.length > ONE) {
                request.append(buildStartTag(LOOPBACK_MODE, false))
                    .append(data[SECOND_PART])
                    .append(buildEndTag(LOOPBACK_MODE));
            }
            request.append(buildEndTag(ONU_ETHPORT_LOOPBACK));

            reply = controller
                    .getDevicesMap()
                    .get(ncDeviceId)
                    .getSession()
                    .doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
        }
        return reply;
    }

    /**
     * Verifies input string for ponlink-id{-onu-id}{-ethport-id}.
     *
     * @param target input data in string
     * @param expected number of IDs expected
     * @return String array containing IDs; may be null if an error is detected
     */
    private String[] checkIdString(String target, int expected) {
        String[] id = target.split(HYPHEN);
        int pon;
        int onu;

        if (id.length < TWO) {
            log.error("Invalid number of arguments for id: {}", id.length);
            return null;
        }
        if (id.length != expected) {
            log.error("Invalid number of arguments for id: {}", id.length);
            return null;
        }
        try {
            pon = Integer.parseInt(id[FIRST_PART]);
            if (pon <= ZERO) {
                log.error("Invalid integer for ponlink-id: {}", id[FIRST_PART]);
                return null;
            }
            onu = Integer.parseInt(id[SECOND_PART]);
            if (onu <= ZERO) {
                log.error("Invalid integer for onu-id: {}", id[SECOND_PART]);
                return null;
            }
            if (expected > TWO) {
                int port = Integer.parseInt(id[THIRD_PART]);
                if (port <= ZERO) {
                    log.error("Invalid integer for port-id: {}", id[THIRD_PART]);
                    return null;
                }
            }
        } catch (NumberFormatException e) {
            log.error("Non-number input for id: {}", target);
            return null;
        }
        return id;
    }

}
