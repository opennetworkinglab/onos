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
package org.onosproject.drivers.ciena.waveserverai.netconf;

import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.drivers.netconf.TemplateManager;
import org.slf4j.Logger;
import org.w3c.dom.Node;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.ciena.waveserverai.netconf.CienaWaveserverAiDeviceDescription.portIdConvert;
import static org.onosproject.drivers.ciena.waveserverai.netconf.CienaWaveserverAiDeviceDescription.portStateConvert;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Handles port administration for Ciena Waveserver Ai devices using the NETCONF
 * protocol.
 */
public class CienaWaveserverAiPortAdmin extends AbstractHandlerBehaviour implements PortAdmin {
    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();
    public static final Logger log = getLogger(CienaWaveserverAiPortAdmin.class);

    static {
        TEMPLATE_MANAGER.load(CienaWaveserverAiPortAdmin.class,
                              "/templates/requests/%s.j2", "isEnabled", "setAdminState");
    }

    /**
     * Sets the administrative state of the given port to the given value.
     *
     * @param number
     *            port number
     * @param state
     *            state, true for enabled, false for disabled
     * @return true if successfully set
     */
    private CompletableFuture<Boolean> setAdminState(PortNumber number, Boolean state) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        String port = portIdConvert(Long.valueOf(String.valueOf(number)));
        String adminState = portStateConvert(state);

        try {
            Map<String, Object> templateContext = new HashMap<String, Object>();
            templateContext.put("port-number", port);
            templateContext.put("admin-state", adminState);
            Node req = (Node) TEMPLATE_MANAGER.doRequest(session, "setAdminState", templateContext, "/",
                                                        XPathConstants.NODE);
            XPath xp = XPathFactory.newInstance().newXPath();

            // If OK element exists then it worked.
            Node ok = (Node) xp.evaluate("/rpc-reply/ok", req, XPathConstants.NODE);

            return CompletableFuture.completedFuture(ok != null);
        } catch (XPathExpressionException | NetconfException e) {
            log.error("Unable to set port admin state for port {} to {}", port, handler().data().deviceId(), adminState,
                      e);
        }
        return CompletableFuture.completedFuture(false);
    }

    @Override
    public CompletableFuture<Boolean> enable(PortNumber number) {
        return setAdminState(number, true);
    }

    @Override
    public CompletableFuture<Boolean> disable(PortNumber number) {
        return setAdminState(number, false);
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber number) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();

        try {
            log.debug("Querying port state for port {} from device {}", number, handler().data().deviceId());
            Map<String, Object> templateContext = new HashMap<String, Object>();
            templateContext.put("port-number", number.toString());
            Node port = TEMPLATE_MANAGER.doRequest(session, "isEnabled", templateContext);
            XPath xp = XPathFactory.newInstance().newXPath();
            return CompletableFuture.completedFuture(portStateConvert(
                    xp.evaluate("state/operational-state/text()", port)));
        } catch (XPathExpressionException | NetconfException e) {
            log.error("Unable to query port state for port {} from device {}", number, handler().data().deviceId(), e);
        }
        return CompletableFuture.completedFuture(false);
    }
}
