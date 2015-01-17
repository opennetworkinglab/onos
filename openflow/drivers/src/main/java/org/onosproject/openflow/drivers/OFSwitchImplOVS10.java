/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.Collections;
import java.util.List;

import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;

/**
 * OFDescriptionStatistics Vendor (Manufacturer Desc.): Nicira, Inc. Make
 * (Hardware Desc.) : Open vSwitch Model (Datapath Desc.) : None Software :
 * 1.11.90 (or whatever version + build) Serial : None
 */
public class OFSwitchImplOVS10 extends AbstractOpenFlowSwitch {

    private static final int LOWEST_PRIORITY = 0;

    public OFSwitchImplOVS10(Dpid dpid, OFDescStatsReply desc) {
        super(dpid);
        setSwitchDescription(desc);

    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "OFSwitchImplOVS10 [" + ((channel != null)
                ? channel.getRemoteAddress() : "?")
                + " DPID[" + ((getStringId() != null) ? getStringId() : "?") + "]]";
    }

    @Override
    public Boolean supportNxRole() {
        return true;
    }

    @Override
    public void startDriverHandshake() {
        OFFlowAdd.Builder fmBuilder = factory().buildFlowAdd();
        fmBuilder.setPriority(LOWEST_PRIORITY);
        write(fmBuilder.build());
    }

    @Override
    public boolean isDriverHandshakeComplete() {
        return true;
    }

    @Override
    public void processDriverHandshakeMessage(OFMessage m) {}

    @Override
    public void write(OFMessage msg) {
        channel.write(Collections.singletonList(msg));
    }

    @Override
    public void write(List<OFMessage> msgs) {
        channel.write(msgs);
    }

    @Override
    public List<OFPortDesc> getPorts() {
        return Collections.unmodifiableList(features.getPorts());
    }


}
