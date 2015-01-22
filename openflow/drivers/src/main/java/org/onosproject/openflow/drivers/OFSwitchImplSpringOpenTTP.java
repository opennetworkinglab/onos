/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.openflow.drivers;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFMessage;

import java.util.List;

/**
 * Created by sanghoshin on 1/22/15.
 */
public class OFSwitchImplSpringOpenTTP extends AbstractOpenFlowSwitch {

    protected OFSwitchImplSpringOpenTTP(Dpid dp) {
        super(dp);
    }

    @Override
    public void write(OFMessage msg) {

    }

    @Override
    public void write(List<OFMessage> msgs) {

    }

    @Override
    public Boolean supportNxRole() {
        return null;
    }

    @Override
    public void startDriverHandshake() {

    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return false;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {

    }

}
