/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 * This Work is contributed by Sterlite Technologies
 */

package org.onosproject.drivers.odtn.util;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class NetconfSessionUtility {

    private static final Logger log = LoggerFactory.getLogger(NetconfSessionUtility.class);

    private NetconfSessionUtility() {
    }

    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId   device identifier
     * @param controller NetconfController
     * @return The netconf session or null
     */
    public static NetconfSession getNetconfSession(DeviceId deviceId,
                                                   NetconfController controller) {
        log.debug("Inside getNetconfSession () method for device : {}", deviceId);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
    }

    /**
     * Execute RPC request.
     *
     * @param session Netconf session
     * @param message Netconf message in XML format
     * @return XMLConfiguration object
     */

    public static XMLConfiguration executeRpc(NetconfSession session, String message) {
        try {
            if (log.isDebugEnabled()) {
                try {
                    StringWriter stringWriter = new StringWriter();
                    XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(message);
                    xconf.setExpressionEngine(new XPathExpressionEngine());
                    xconf.save(stringWriter);
                    log.debug("Request {}", stringWriter.toString());
                } catch (ConfigurationException e) {
                    log.error("XML Config Exception ", e);
                }
            }
            CompletableFuture<String> fut = session.rpc(message);
            String rpcReply = fut.get();
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(new XPathExpressionEngine());
            if (log.isDebugEnabled()) {
                try {
                    StringWriter stringWriter = new StringWriter();
                    xconf.save(stringWriter);
                    log.debug("Response {}", stringWriter.toString());
                } catch (ConfigurationException e) {
                    log.error("XML Config Exception ", e);
                }
            }
            return xconf;
        } catch (NetconfException ne) {
            log.error("Exception on Netconf protocol: {}.", ne);
        } catch (InterruptedException ie) {
            log.error("Interrupted Exception: {}.", ie);
        } catch (ExecutionException ee) {
            log.error("Concurrent Exception while executing Netconf operation: {}.", ee);
        }
        return null;
    }

}
