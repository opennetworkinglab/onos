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
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
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
public class FujitsuVoltPonLinkConfig extends AbstractHandlerBehaviour
        implements VoltPonLinkConfig {

    private final Logger log = getLogger(FujitsuVoltPonLinkConfig.class);
    private static final Map<String, List<Integer>> PON_LINK_PARAMS = new HashMap<String, List<Integer>>() {
        {
            put("onu-discovery-interval", Arrays.asList(ONU_DISCOVERY_INTERVAL_MIN, ONU_DISCOVERY_INTERVAL_MAX));
            put("dba-cycle-time", Arrays.asList(DBA_CYCLE_TIME_MIN, DBA_CYCLE_TIME_MAX));
            put("mac-age-time", Arrays.asList(MAC_AGE_TIME_MIN, MAC_AGE_TIME_MAX));
            put("lof-threshold", Arrays.asList(LOF_THRESHOLD_MIN, LOF_THRESHOLD_MAX));
            put("los-threshold", Arrays.asList(LOS_THRESHOLD_MIN, LOS_THRESHOLD_MAX));
            put(ONU_DISCOVERY_MODE, null);
            put(PM_ENABLE, null);
            put(ADMIN_STATE, null);
            put(LOOPBACK_ENABLE, null);
        }
    };
    private static final String VOLT_PORTS = "volt-ports";
    private static final String GPON_PONLINK_PORTS = "gpon-ponlink-ports";
    private static final String GPON_PONLINK_PORT = "gpon-ponlink-port";
    private static final String ADMIN_STATE = "admin-state";
    private static final String ONU_DISCOVERY_MODE = "onu-discovery-mode";
    private static final String PM_ENABLE = "pm-enable";
    private static final String LOOPBACK_ENABLE = "loopback-enable";
    private static final Set<String> ADMINSTATES =
            ImmutableSet.of("enable", "disable");
    private static final Set<String> ONUDISCOVERYMODES =
            ImmutableSet.of("auto", "manual", "disabled");
    private static final Set<String> PMENABLES =
            ImmutableSet.of("true", "false");
    private static final Set<String> LOOPBACKENABLES =
            ImmutableSet.of("true", "false");

    private static final int ONU_DISCOVERY_INTERVAL_MIN = 1;
    private static final int ONU_DISCOVERY_INTERVAL_MAX = 3600;
    private static final int DBA_CYCLE_TIME_MIN = 2;
    private static final int DBA_CYCLE_TIME_MAX = 40;
    private static final int MAC_AGE_TIME_MIN = 1000;
    private static final int MAC_AGE_TIME_MAX = 3600000;
    private static final int LOF_THRESHOLD_MIN = 1;
    private static final int LOF_THRESHOLD_MAX = 10;
    private static final int LOS_THRESHOLD_MIN = 1;
    private static final int LOS_THRESHOLD_MAX = 10;
    private static final int RANGE_MIN = 0;
    private static final int RANGE_MAX = 1;

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
            return null;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT + NEW_LINE);
            request.append(buildStartTag(VOLT_PORTS));
            if (target != null) {
                int pon;
                try {
                    pon = Integer.parseInt(target);
                    if (pon <= ZERO) {
                        log.error("Invalid integer for ponlink-id:{}", target);
                        return null;
                    }
                } catch (NumberFormatException e) {
                    log.error("Non-number input for ponlink-id:{}", target);
                    return null;
                }
                request.append(buildStartTag(GPON_PONLINK_PORTS))
                    .append(buildStartTag(GPON_PONLINK_PORT))
                    .append(buildStartTag(PONLINK_ID, false))
                    .append(target)
                    .append(buildEndTag(PONLINK_ID))
                    .append(buildEndTag(GPON_PONLINK_PORT))
                    .append(buildEndTag(GPON_PONLINK_PORTS));
            } else {
                request.append(buildEmptyTag(GPON_PONLINK_PORTS));
            }
            request.append(buildEndTag(VOLT_PORTS));
            request.append(VOLT_NE_CLOSE);

            reply = controller
                        .getDevicesMap()
                        .get(ncDeviceId)
                        .getSession()
                        .get(request.toString(), REPORT_ALL);
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
        }
        return reply;
    }

    @Override
    public boolean setPonLink(String target) {
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

        String[] data = checkSetInput(target);
        if (data == null) {
            log.error("Failed to check input: {}", target);
            return false;
        }

        boolean result = false;
        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE);
            request.append(ANGLE_RIGHT + NEW_LINE);
            request.append(buildStartTag(VOLT_PORTS))
                .append(buildStartTag(GPON_PONLINK_PORTS))
                .append(buildStartTag(GPON_PONLINK_PORT))
                .append(buildStartTag(PONLINK_ID, false))
                .append(data[FIRST_PART])
                .append(buildEndTag(PONLINK_ID))
                .append(buildStartTag(data[SECOND_PART], false))
                .append(data[THIRD_PART])
                .append(buildEndTag(data[SECOND_PART]))
                .append(buildEndTag(GPON_PONLINK_PORT))
                .append(buildEndTag(GPON_PONLINK_PORTS))
                .append(buildEndTag(VOLT_PORTS))
                .append(VOLT_NE_CLOSE);

            result = controller.getDevicesMap().get(ncDeviceId).getSession().
                    editConfig(RUNNING, null, request.toString());
        } catch (IOException e) {
            log.error("Cannot communicate to device {} exception {}", ncDeviceId, e);
        }
        return result;
    }

    /**
     * Verifies input string for valid options.
     *
     * @param target input data in string
     * @return String array containing IDs; may be null if an error is detected
     */
    private String[] checkSetInput(String target) {
        String[] data = target.split(COLON);
        String paramName = data[SECOND_PART];
        String paramValue = data[THIRD_PART];
        int pon;

        if (data.length != THREE) {
            log.error("Invalid number of arguments {}", data.length);
            return null;
        }

        try {
            pon = Integer.parseInt(data[FIRST_PART]);
            if (pon <= ZERO) {
                log.error("Invalid integer for ponlink-id: {}",
                          data[FIRST_PART]);
                return null;
            }
        } catch (NumberFormatException e) {
            log.error("Non-number input for ponlink-id: {}",
                      data[FIRST_PART]);
            return null;
        }

        if (!PON_LINK_PARAMS.containsKey(paramName)) {
            log.error("Unsupported parameter: {}", paramName);
            return null;
        }

        List<Integer> range = PON_LINK_PARAMS.get(paramName);
        if (range == null) {
            switch (paramName) {
                case ADMIN_STATE:
                    if (!validState(ADMINSTATES, paramName, paramValue)) {
                        return null;
                    }
                    break;
                case ONU_DISCOVERY_MODE:
                    if (!validState(ONUDISCOVERYMODES, paramName, paramValue)) {
                        return null;
                    }
                    break;
                case PM_ENABLE:
                    if (!validState(PMENABLES, paramName, paramValue)) {
                        return null;
                    }
                    break;
                default:
                    if (!validState(LOOPBACKENABLES, paramName, paramValue)) {
                        return null;
                    }
                    break;
            }
        } else {
            int value;

            try {
                value = Integer.parseInt(paramValue);
            } catch (NumberFormatException e) {
                log.error("Non-number input for Name {} : Value {}.",
                          paramName, paramValue);
                return null;
            }

            if ((value < range.get(RANGE_MIN)) ||
                (value > range.get(RANGE_MAX))) {
                log.error("Out of value range for Name {} : Value {}.",
                          paramName, paramValue);
                return null;
            }
        }

        return data;
    }

    /**
     * Verifies input string for valid options.
     *
     * @param states input data in string for parameter state
     * @param name input data in string for parameter name
     * @param value input data in string for parameter value
     * @return true/false if the param is valid/invalid
     */
    private boolean validState(Set<String> states, String name, String value) {
        if (!states.contains(value)) {
            log.error("Invalid value for Name {} : Value {}.", name, value);
            return false;
        }
        return true;
    }
}
