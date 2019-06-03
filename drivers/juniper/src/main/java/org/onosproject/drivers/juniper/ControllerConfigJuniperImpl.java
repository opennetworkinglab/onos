/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.juniper;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.juniper.JuniperUtils.cliDeleteRequestBuilder;
import static org.onosproject.drivers.juniper.JuniperUtils.cliSetRequestBuilder;
import static org.onosproject.drivers.juniper.JuniperUtils.getOpenFlowControllersFromConfig;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.collect.Lists;
import org.onosproject.drivers.utilities.XmlConfigParser;

import java.util.List;

import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;

/**
 * Set and get the openflow controller information via NETCONF for Juniper Switch. *
 */
public class ControllerConfigJuniperImpl extends AbstractHandlerBehaviour implements ControllerConfig {

    private final Logger log = getLogger(getClass());

    private static final String RPC_TAG_NETCONF_BASE = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String RPC_CLOSE_TAG = "</rpc>";

    @Override
    public List<ControllerInfo> getControllers() {
        List<ControllerInfo> controllers = Lists.newArrayList();
        String reply = retrieveResultCommand(buildRpcGetOpenFlowController());

        if (reply == null || reply.isEmpty()) {
            log.error("Cannot get the controllers from switch");
        } else {
            controllers = getOpenFlowControllersFromConfig(XmlConfigParser
                    .loadXml(new ByteArrayInputStream(reply.getBytes())));
            log.debug("controllers {}", controllers);
        }

        return controllers;
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        if (!requestCommand(buildRpcSetOpenFlowController(controllers))) {
            log.error("Cannot set the controllers to switch");
        }
    }

    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        if (!requestCommand(buildRpcRemoveOpenFlowController())) {
            log.error("Cannot remove the controllers from switch");
        }
    }

    private static String buildRpcGetOpenFlowController() {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);

        rpc.append("<get-configuration>");
        rpc.append("<configuration>");
        rpc.append("<protocols>");
        rpc.append("<openflow>");
        rpc.append("<mode>");
        rpc.append("<ofagent-mode>");
        rpc.append("<controller>");
        rpc.append("</controller>");
        rpc.append("</ofagent-mode>");
        rpc.append("</mode>");
        rpc.append("</openflow>");
        rpc.append("</protocols>");
        rpc.append("</configuration>");
        rpc.append("</get-configuration>");
        rpc.append(RPC_CLOSE_TAG);
        rpc.append("]]>]]>");

        return rpc.toString();
    }

    private static String buildRpcSetOpenFlowController(List<ControllerInfo> controllers) {
        StringBuilder request = new StringBuilder();

        request.append("<protocols>");
        request.append("<openflow operation=\"delete\"/>");
        request.append("</protocols>");

        request.append("<protocols>");
        request.append("<openflow>");
        request.append("<mode>");
        request.append("<ofagent-mode>");

        request.append("<controller>");
        for (ControllerInfo controller : controllers) {
            request.append("<ip>");
            request.append("<name>");
            request.append(controller.ip().toString());
            request.append("</name>");
            request.append("<protocol>");
            request.append("<tcp>");
            request.append("<port>");
            request.append(controller.port());
            request.append("</port>");
            request.append("</tcp>");
            request.append("</protocol>");
            request.append("</ip>");
        }
        request.append("</controller>");
        request.append("</ofagent-mode>");
        request.append("</mode>");
        request.append("</openflow>");
        request.append("</protocols>");

        return cliSetRequestBuilder(request);
    }

    private static String buildRpcRemoveOpenFlowController() {
        StringBuilder request = new StringBuilder();

        request.append("<protocols>");
        request.append("<openflow>");
        request.append("<mode>");
        request.append("<ofagent-mode>");
        request.append("<controller operation=\"delete\"/>");
        request.append("</ofagent-mode>");
        request.append("</mode>");
        request.append("</openflow>");
        request.append("</protocols>");

        return cliDeleteRequestBuilder(request);
    }

    private static String buildCommit() {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);

        rpc.append("<commit/>");
        rpc.append(RPC_CLOSE_TAG);
        rpc.append("]]>]]>");

        return rpc.toString();
    }

    private static String buildDiscardChanges() {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);

        rpc.append("<discard-changes/>");
        rpc.append(RPC_CLOSE_TAG);
        rpc.append("]]>]]>");

        return rpc.toString();
    }

    private NetconfSession getSession() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        DeviceId deviceId = handler().data().deviceId();
        NetconfDevice device = controller.getDevicesMap().get(deviceId);

        if (device == null) {
            log.error("Cannot find the netconf device : {}", deviceId);
            return null;
        }

        return device.getSession();
    }

    private String retrieveResultCommand(String command) {
        NetconfSession session = getSession();
        String reply;

        if (session == null) {
            log.error("Cannot get session : {}", command);
            return null;
        }

        try {
            reply = session.requestSync(command).trim();
            log.debug(reply);
        } catch (NetconfException e) {
            log.debug(e.getMessage());
            return null;
        }

        return reply;
    }

    private boolean requestCommand(String command) {
        NetconfSession session = getSession();

        if (session == null) {
            log.error("Cannot get session : {}", command);
            return false;
        }

        try {
            String reply = session.requestSync(command).trim();
            log.debug(reply);

            if (!isOK(reply)) {
                log.error("discard changes {}", reply);
                session.requestSync(buildDiscardChanges());
                return false;
            }

            reply = session.requestSync(buildCommit()).trim();
            log.debug("reply : {}", reply);
        } catch (NetconfException e) {
            log.debug(e.getMessage());
            return false;
        }

        return true;
    }

    private static boolean isOK(String reply) {
        return reply != null && reply.contains("<ok/>");
    }
}