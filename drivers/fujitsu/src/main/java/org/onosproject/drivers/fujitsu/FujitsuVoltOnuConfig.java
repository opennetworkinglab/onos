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
import org.onosproject.drivers.fujitsu.behaviour.VoltOnuConfig;
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
 * Implementation to get and set parameters available in vOLT
 * through the Netconf protocol.
 */
public class FujitsuVoltOnuConfig extends AbstractHandlerBehaviour
        implements VoltOnuConfig {

    private final Logger log = getLogger(FujitsuVoltOnuConfig.class);
    private final Set<String> onuConfigParams = ImmutableSet.of(
            "admin-state", "pm-enable", "fec-enable",
            "security-enable", "password");
    private static final String VOLT_ONUS = "volt-onus";
    private static final String ONUS_PERLINK = "onus-perlink";
    private static final String ONUS_LIST = "onus-list";
    private static final String ONU_INFO = "onu-info";
    private static final String ONU_SET_CONFIG = "onu-set-config";
    private static final String CONFIG_INFO = "config-info";
    private static final String VOLT_STATISTICS = "volt-statistics";
    private static final String ONU_STATISTICS = "onu-statistics";
    private static final String ONU_ETH_STATS = "onu-eth-stats";
    private static final String ETH_STATS = "eth-stats";
    private static final String ONU_GEM_STATS = "onu-gem-stats";
    private static final String GEM_STATS = "gem-stats";
    private int pon;
    private int onu;


    @Override
    public String getOnus(String target) {
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

        if (target != null) {
            onuId = target.split(HYPHEN);
            if (onuId.length > 2) {
                log.error("Invalid number of arguments");
                return reply;
            }
            try {
                pon = Integer.parseInt(onuId[0]);
                if (onuId.length > 1) {
                    onu = Integer.parseInt(onuId[1]);
                }
            } catch (NumberFormatException e) {
                log.error("Non-number input");
                return reply;
            }
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT).append(NEW_LINE);
            if (onuId != null) {
                request.append(buildStartTag(VOLT_ONUS));
                request.append(buildStartTag(ONUS_PERLINK));
                request.append(buildStartTag(PONLINK_ID, false));
                request.append(onuId[0]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > 1) {
                    request.append(buildStartTag(ONUS_LIST));
                    request.append(buildStartTag(ONU_INFO));
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[1]);
                    request.append(buildEndTag(ONU_ID));
                    request.append(buildEndTag(ONU_INFO));
                    request.append(buildEndTag(ONUS_LIST));
                }
                request.append(buildEndTag(ONUS_PERLINK));
                request.append(buildEndTag(VOLT_ONUS));
            } else {
                request.append(buildEmptyTag(VOLT_ONUS));
            }
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
    public String setOnu(String target) {
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

        String[] data = target.split(COLON);
        if (data.length != 3) {
            log.error("Invalid number of arguments");
            return reply;
        }

        String[] onuId = data[0].split(HYPHEN);
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

        if (!onuConfigParams.contains(data[1])) {
            log.error("Unsupported parameter: " + data[1]);
            return reply;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT).append(ONU_SET_CONFIG).append(SPACE);
            request.append(VOLT_NE_NAMESPACE).append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(PONLINK_ID, false));
            request.append(onuId[0]);
            request.append(buildEndTag(PONLINK_ID));
            request.append(buildStartTag(ONU_ID, false));
            request.append(onuId[1]);
            request.append(buildEndTag(ONU_ID));
            request.append(buildStartTag(CONFIG_INFO));
            request.append(buildStartTag(data[1], false));
            request.append(data[2]);
            request.append(buildEndTag(data[1]));
            request.append(buildEndTag(CONFIG_INFO));
            request.append(buildEndTag(ONU_SET_CONFIG));

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    doWrappedRpc(request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

    @Override
    public String getOnuStatistics(String target) {
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

        if (target != null) {
            onuId = target.split(HYPHEN);
            if (onuId.length > 2) {
                log.error("Invalid number of arguments:" + onuId.length);
                return reply;
            }
            try {
                pon = Integer.parseInt(onuId[0]);
                if (onuId.length > 1) {
                    onu = Integer.parseInt(onuId[1]);
                }
            } catch (NumberFormatException e) {
                log.error("Non-number input");
                return reply;
            }
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN).append(VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(VOLT_STATISTICS));
            if (onuId != null) {
                request.append(buildStartTag(ONU_STATISTICS));
                request.append(buildStartTag(ONU_GEM_STATS));
                request.append(buildStartTag(GEM_STATS));
                request.append(buildStartTag(PONLINK_ID, false));
                request.append(onuId[0]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > 1) {
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[1]);
                    request.append(buildEndTag(ONU_ID));
                }
                request.append(buildEndTag(GEM_STATS));
                request.append(buildEndTag(ONU_GEM_STATS));

                request.append(buildStartTag(ONU_ETH_STATS));
                request.append(buildStartTag(ETH_STATS));
                request.append(buildStartTag(PONLINK_ID, false));
                request.append(onuId[0]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > 1) {
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[1]);
                    request.append(buildEndTag(ONU_ID));
                }
                request.append(buildEndTag(ETH_STATS));
                request.append(buildEndTag(ONU_ETH_STATS));

                request.append(buildEndTag(ONU_STATISTICS));
            } else {
                request.append(buildEmptyTag(ONU_STATISTICS));
            }
            request.append(buildEndTag(VOLT_STATISTICS));
            request.append(VOLT_NE_CLOSE);

            reply = controller.
                    getDevicesMap().get(ncDeviceId).getSession().
                    get(request.toString(), REPORT_ALL);
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception ", ncDeviceId, e);
        }
        return reply;
    }

}
