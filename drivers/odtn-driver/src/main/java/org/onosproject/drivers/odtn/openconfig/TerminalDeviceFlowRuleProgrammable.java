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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openconfig;

import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TerminalDeviceFlowRuleProgrammable extends AbstractTerminalDeviceFlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(TerminalDeviceFlowRuleProgrammable.class);

    public void setOpticalChannelFrequency(NetconfSession session,
                                           String optChannel, Frequency freq)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();
        sb.append(
                "<components xmlns='http://openconfig.net/yang/platform'>"
                        + "<component nc:operation='merge'>"
                        + "<name>" + optChannel + "</name>"
                        + "<oc-opt-term:optical-channel "
                        +
                        "    xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>"
                        + "  <oc-opt-term:config>"
                        + "  <oc-opt-term:frequency>" + (long) freq.asMHz() +
                        "</oc-opt-term:frequency>"
                        + "    </oc-opt-term:config>"
                        + " </oc-opt-term:optical-channel>"
                        + "</component>"
                        + "</components>");
        log.info("Optical Channel Frequency {}", sb.toString());
        boolean ok = session.editConfig(DatastoreId.CANDIDATE, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error committing channel frequency");
        }
    }


    @Override
    protected String applyFlowRule(NetconfSession session, FlowRule r) throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(session, frp.getPortNumber());
            setOpticalChannelFrequency(session, optChannel,
                                       frp.getCentralFrequency());
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }

    @Override
    protected String removeFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(session, frp.getPortNumber());
            setOpticalChannelFrequency(session, optChannel, Frequency.ofMHz(0));
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }
}
