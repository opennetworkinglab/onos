/*
 * Copyright 2016-present Open Networking Foundation
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

import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFVersion;

/**
 * Default driver to fallback on if no other driver is available.
 */
public class DefaultSwitchHandshaker extends AbstractOpenFlowSwitch {

    private static final int LOWEST_PRIORITY = 0;

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
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {}

    @Override
    public boolean isDriverHandshakeComplete() {
        return true;
    }
}
