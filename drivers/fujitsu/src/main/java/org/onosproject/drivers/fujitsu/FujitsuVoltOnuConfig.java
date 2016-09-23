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
    private static final String ADMIN_STATE = "admin-state";
    private static final String PASSWORD = "password";
    private static final Set<String> ONUCONFIGPARAMS =
            ImmutableSet.of(ADMIN_STATE, "pm-enable", "fec-enable", "security-enable", PASSWORD);
    private static final Set<String> ADMINSTATES =
            ImmutableSet.of("enable", "disable");
    private static final Set<String> ENABLES =
            ImmutableSet.of("true", "false");
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
    private static final String PASSWORD_PATTERN = "^[a-zA-Z0-9]+$";

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
            return null;
        }

        if (target != null) {
            onuId = checkIdString(target);
            if (onuId == null) {
                log.error("Invalid ONU identifier {}", target);
                return null;
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
                request.append(onuId[FIRST_PART]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > ONE) {
                    request.append(buildStartTag(ONUS_LIST));
                    request.append(buildStartTag(ONU_INFO));
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[SECOND_PART]);
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
            return null;
        }

        String[] data = target.split(COLON);
        if (data.length != THREE) {
            log.error("Invalid number of arguments");
            return null;
        }

        String[] onuId = checkIdString(data[FIRST_PART]);
        if ((onuId == null) || (onuId.length != TWO)) {
            log.error("Invalid ONU identifier {}", target);
            return null;
        }

        if (!checkSetParam(data[SECOND_PART],
                            data[THIRD_PART])) {
            log.error("Failed to check input {}", target);
            return null;
        }

        try {
            StringBuilder request = new StringBuilder();
            request.append(ANGLE_LEFT).append(ONU_SET_CONFIG).append(SPACE);
            request.append(VOLT_NE_NAMESPACE).append(ANGLE_RIGHT).append(NEW_LINE);
            request.append(buildStartTag(PONLINK_ID, false));
            request.append(onuId[FIRST_PART]);
            request.append(buildEndTag(PONLINK_ID));
            request.append(buildStartTag(ONU_ID, false));
            request.append(onuId[SECOND_PART]);
            request.append(buildEndTag(ONU_ID));
            request.append(buildStartTag(CONFIG_INFO));
            request.append(buildStartTag(data[SECOND_PART], false));
            request.append(data[THIRD_PART]);
            request.append(buildEndTag(data[SECOND_PART]));
            request.append(buildEndTag(CONFIG_INFO));
            request.append(buildEndTag(ONU_SET_CONFIG));

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
            return null;
        }

        if (target != null) {
            onuId = checkIdString(target);
            if (onuId == null) {
                log.error("Failed to check ID: {}", target);
                return null;
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
                request.append(onuId[FIRST_PART]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > ONE) {
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[SECOND_PART]);
                    request.append(buildEndTag(ONU_ID));
                }
                request.append(buildEndTag(GEM_STATS));
                request.append(buildEndTag(ONU_GEM_STATS));

                request.append(buildStartTag(ONU_ETH_STATS));
                request.append(buildStartTag(ETH_STATS));
                request.append(buildStartTag(PONLINK_ID, false));
                request.append(onuId[FIRST_PART]);
                request.append(buildEndTag(PONLINK_ID));
                if (onuId.length > ONE) {
                    request.append(buildStartTag(ONU_ID, false));
                    request.append(onuId[SECOND_PART]);
                    request.append(buildEndTag(ONU_ID));
                }
                request.append(buildEndTag(ETH_STATS));
                request.append(buildEndTag(ONU_ETH_STATS));
                request.append(buildEndTag(ONU_STATISTICS));
            } else  {
                request.append(buildEmptyTag(ONU_STATISTICS));
            }
            request.append(buildEndTag(VOLT_STATISTICS));
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

    /**
     * Verifies input string for ponlink-id{-onu-id}.
     *
     * @param target input data in string
     * @return String array
     * @return null if an error condition is detected
     */
    private String[] checkIdString(String target) {
        String[] onuId = target.split(HYPHEN);
        int pon;
        int onu;

        if (onuId.length > TWO) {
            log.error("Invalid number of arguments for id:{}", onuId.length);
            return null;
        }
        try {
            pon = Integer.parseInt(onuId[FIRST_PART]);
            if (pon <= ZERO) {
                log.error("Invalid integer for ponlink-id:{}", onuId[FIRST_PART]);
                return null;
            }
            if (onuId.length > ONE) {
                onu = Integer.parseInt(onuId[SECOND_PART]);
                if (onu <= ZERO) {
                    log.error("Invalid integer for onu-id:{}", onuId[SECOND_PART]);
                    return null;
                }
            }
        } catch (NumberFormatException e) {
            log.error("Non-number input for id:{}", target);
            return null;
        }
        return onuId;
    }

    /**
     * Verifies input string for valid options.
     *
     * @param name input data in string
     * @param value input data in string
     * @return true if the parameter is valid
     * @return false if the parameter is invalid
     */
    private boolean checkSetParam(String name, String value) {
        if (!ONUCONFIGPARAMS.contains(name)) {
            log.error("Unsupported parameter: {}", name);
            return false;
        }

        switch (name) {
            case ADMIN_STATE:
                if (!validState(ADMINSTATES, name, value)) {
                    return false;
                }
                break;
            case PASSWORD:
                if (!value.matches(PASSWORD_PATTERN)) {
                    log.error("Invalid value for Name {} : Value {}.", name, value);
                    return false;
                }
                break;
            default:
                if (!validState(ENABLES, name, value)) {
                    return false;
                }
                break;
        }
        return true;
    }

    /**
     * Verifies input string for valid options.
     *
     * @param states input data in string for parameter state
     * @param name input data in string for parameter name
     * @param value input data in string for parameter value
     * @return true if the param is valid
     * @return false if the param is invalid
     */
    private boolean validState(Set<String> states, String name, String value) {
        if (!states.contains(value)) {
            log.error("Invalid value for Name {} : Value {}.", name, value);
            return false;
        }
        return true;
    }

}
