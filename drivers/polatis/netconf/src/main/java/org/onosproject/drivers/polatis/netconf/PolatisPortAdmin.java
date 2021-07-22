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

package org.onosproject.drivers.polatis.netconf;

import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PortAdmin;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;

import static org.slf4j.LoggerFactory.getLogger;

import static org.onosproject.drivers.polatis.netconf.PolatisUtility.*;

import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfGet;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.netconfRpc;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_OK;


/**
 * Handles port administration for Polatis switches using NETCONF.
 */
public class PolatisPortAdmin extends AbstractHandlerBehaviour implements PortAdmin {

    public static final Logger log = getLogger(PolatisPortAdmin.class);


    /**
     * Sets the administrative state of the given port to the given value.
     *
     * @param portNumber Port number
     * @param state      State, PC_ENABLED or PC_DISABLED
     * @return           True if successfully set
     */
    private CompletableFuture<Boolean> setAdminState(PortNumber portNumber, String state) {

        boolean result = false;
        try {
            log.debug("Sending RPC to {} to set port {} to {}", handler().data().deviceId(), portNumber, state);
            String cmdBody = getRpcSetPortStateBody(state.equals(PORT_ENABLED) ? KEY_ENABLE : KEY_DISABLE, portNumber);
            String response = netconfRpc(handler(),
                              getRpcSetPortStateBody(state.equals(PORT_ENABLED) ? KEY_ENABLE : KEY_DISABLE,
                              portNumber));
            log.trace("Response from RPC: " + response);
            result = response.contains(KEY_OK);
        } catch (IllegalStateException e) {
            log.error("Unable to set port admin state for {}/{} to {}", handler().data().deviceId(), portNumber,
                      state, e);
        }
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<Boolean> enable(PortNumber portNumber) {
        return setAdminState(portNumber, PORT_ENABLED);
    }

    @Override
    public CompletableFuture<Boolean> disable(PortNumber portNumber) {
        return setAdminState(portNumber, PORT_DISABLED);
    }

    @Override
    public CompletableFuture<Boolean> isEnabled(PortNumber portNumber) {
        boolean result = false;
        try {
            log.debug("Querying port state for port {} from device {}", portNumber, handler().data().deviceId());
            String response = netconfGet(handler(), getPortStatusFilter(portNumber));
            result = response.equals(PORT_ENABLED);
            log.debug("Port {}/{} is {}", handler().data().deviceId(), portNumber, result);
        } catch (IllegalStateException e) {
            log.error("Unable to query port state for port {} from device {}", portNumber,
                      handler().data().deviceId(), e);
        }
        return CompletableFuture.completedFuture(result);
    }
}
