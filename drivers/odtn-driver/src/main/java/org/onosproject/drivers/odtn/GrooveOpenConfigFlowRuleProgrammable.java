/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.odtn;

import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.drivers.odtn.openconfig.AbstractTerminalDeviceFlowRuleProgrammable;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation of FlowRuleProgrammable interface for
 * OpenConfig terminal devices.
 */
public class GrooveOpenConfigFlowRuleProgrammable
        extends AbstractTerminalDeviceFlowRuleProgrammable {


    private static final Logger log =
            LoggerFactory.getLogger(GrooveOpenConfigFlowRuleProgrammable.class);

    public void setOpticalChannelFrequency(NetconfSession session,
                                            String optChannel, Frequency freq) throws NetconfException {

        final String transceiver = optChannel.replace("OCH", "TRANSCEIVER");
        final StringBuilder sb = new StringBuilder();
        sb.append(
                "<components xmlns='http://openconfig.net/yang/platform'>"
                        + "<component>"
                        + "<name>" + optChannel + "</name>"
                        + "<oc-opt-term:optical-channel "
                        +
                        "    xmlns:oc-opt-term='http://openconfig.net/yang/terminal-device'>"
                        + "  <oc-opt-term:config>"
                        + "   <oc-opt-term:frequency>" + (long) freq.asMHz() + "</oc-opt-term:frequency>"
                        + "  </oc-opt-term:config>"
                        + " </oc-opt-term:optical-channel>"
                        + "</component>"
                        + "<component>"
                        + "  <name>" + transceiver + "</name>"
                        + "   <transceiver xmlns='http://openconfig.net/yang/platform/transceiver'>"
                        + "     <config>"
                        + "       <enabled>true</enabled>"
                        + "     </config>"
                        + "   </transceiver>"
                        + " </component>"
                        + "</components>");

        final boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());

        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
    }

    //Overloaded setOpticalChannelFrequency() with two param as required for removeFlowRule()
    private void setOpticalChannelFrequency(NetconfSession session, StringBuilder sb) throws NetconfException {

        final boolean ok =
                session.editConfig(DatastoreId.RUNNING, null, sb.toString());

        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
    }

    @Override
    protected String removeFlowRule(NetconfSession session, FlowRule r)
            throws NetconfException {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String optChannel = getOpticalChannel(frp.getPortNumber());
            final String transceiver = optChannel.replace("OCH", "TRANSCEIVER");
            final StringBuilder sb = new StringBuilder();
            /*
            TODO imcomplete solution: doesn't set frequency to zero; see below
            sb.append(
                    "<components xmlns='http://openconfig.net/yang/platform'>"
                            + "<component>"
                            + "  <name>" + transceiver + "</name>"
                            + "   <transceiver xmlns='http://openconfig.net/yang/platform/transceiver'>"
                            + "     <config>"
                            + "       <enabled>false</enabled>"
                            + "     </config>"
                            + "   </transceiver>"
                            + " </component>"
                            + "</components>");

             */

            // NOTE: Disabling the laser via openconfig on Groove is not possible
            //       and consequently the frequency can't be set to 0.
            //       This is an error on mapping between native and openconfig model.

            // Workaround: use legacy model
            Pattern transceiverPattern = Pattern.compile(".*-[1]-[1-9][0-4]?-L[1-2]$"); // e.g. TRANSCEIVER-1-1-L1
            Matcher lineMatch = transceiverPattern.matcher(transceiver);
            lineMatch.find();
            final String[] split = lineMatch.group(0).split("-");

            sb.append("<ne xmlns=\"http://coriant.com/yang/os/ne\">" +
                              "        <shelf>" +
                              "          <shelf-id>" + split[1] + "</shelf-id>" +
                              "          <slot>" +
                              "            <slot-id>" + split[2] + "</slot-id>" +
                              "            <card>" +
                              "              <port>" +
                              "                <port-id>" + split[3].replace("L", "") + "</port-id>" +
                              "                <och-os>" +
                              "                  <frequency>0</frequency>" +
                              "                  <laser-enable>disabled</laser-enable>" +
                              "                </och-os>" +
                              "              </port>" +
                              "            </card>" +
                              "          </slot>" +
                              "        </shelf>" +
                              "      </ne>");

            setOpticalChannelFrequency(session, sb);
            return optChannel + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }
}
