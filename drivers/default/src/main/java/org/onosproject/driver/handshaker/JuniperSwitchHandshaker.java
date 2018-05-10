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
package org.onosproject.driver.handshaker;

import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.SwitchStateException;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JuniperSwitchHandshaker extends AbstractOpenFlowSwitch {

    private static final int LOWEST_PRIORITY = 0;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public Boolean supportNxRole() {
        return false;
    }


    @Override
    public void startDriverHandshake() {
        if (factory().getVersion() == OFVersion.OF_10) {
            OFFlowAdd.Builder fmBuilder = factory().buildFlowAdd();
            fmBuilder.setPriority(LOWEST_PRIORITY);
            sendHandshakeMessage(fmBuilder.build());
        }
        log.debug("Juniper Switch Operating OF version {}", factory().getVersion());
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return true;
    }


    @Override
    public void processDriverHandshakeMessage(OFMessage m) {
        log.debug("Juniper Switch: processDriverHandshakeMessage for sw {}", getStringId());
    }


    @Override
    public void setRole(RoleState role) {
        // Juniper switch dosen't support OpenFlow Role Request/Reply message
        log.warn("Juniper switch dosen't support OpenFlow Role Request/Reply message");
        if (this.role == null) {
            this.role = role;
        }
    }


    @Override
    public void handleRole(OFMessage m) throws SwitchStateException {
        // Juniper switch dosen't support OpenFlow Role Request/Reply message
        log.warn("Juniper switch dosen't support OpenFlow Role Request/Reply message");
    }

}
