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

package org.onosproject.drivers.odtn;

import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.openconfig.AbstractTerminalDeviceFlowRuleProgrammable;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of FlowRuleProgrammable interface for
 * OpenConfig terminal devices.
 */
public class AdvaFlowRuleProgrammable
        extends AbstractTerminalDeviceFlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(AdvaFlowRuleProgrammable.class);


    public void setOpticalChannelFrequency(NetconfSession session,
                                            String optChannel, Frequency freq)
            throws NetconfException {
        StringBuilder sb = new StringBuilder();
        sb.append(
                    "<components xmlns='http://openconfig.net/yang/platform'>"
                  + "<component>"
                  + "<config>"
                  + "<name>" + optChannel + "</name>"
                  + "</config>"
                  + "<optical-channel xmlns='http://openconfig.net/yang/terminal-device'>"
                  + "<config>"
                  + "<frequency>" + (long) freq.asMHz() + "</frequency>"
                  + "</config>"
                  + "</optical-channel>"
                  + "</component>"
                  + "</components>");
        log.info("Optical Channel Frequency {}", sb.toString());
        boolean ok = session.editConfig(DatastoreId.RUNNING, null, sb.toString());
        if (!ok) {
            throw new NetconfException("error writing channel frequency");
        }
        ok = session.commit();
        if (!ok) {
            throw new NetconfException("error committing channel frequency");
        }
    }


}
