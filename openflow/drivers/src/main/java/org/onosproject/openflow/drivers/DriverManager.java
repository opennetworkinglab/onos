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


import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.RoleState;
import org.onosproject.openflow.controller.driver.AbstractOpenFlowSwitch;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriver;
import org.onosproject.openflow.controller.driver.OpenFlowSwitchDriverFactory;

import org.projectfloodlight.openflow.protocol.OFDescStatsReply;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A simple implementation of a driver manager that differentiates between
 * connected switches using the OF Description Statistics Reply message.
 */
public final class DriverManager implements OpenFlowSwitchDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);

    // Whether to use an OF 1.3 configured TTP, or to use an OF 1.0-style
    // single table with packet-ins.
    private static boolean cpqdUsePipeline13 = false;

    /**
     * Return an IOFSwitch object based on switch's manufacturer description
     * from OFDescStatsReply.
     *
     * @param desc DescriptionStatistics reply from the switch
     * @return A IOFSwitch instance if the driver found an implementation for
     *         the given description. Otherwise it returns OFSwitchImplBase
     */
    @Override
    public OpenFlowSwitchDriver getOFSwitchImpl(Dpid dpid,
            OFDescStatsReply desc, OFVersion ofv) {
        String vendor = desc.getMfrDesc();
        String hw = desc.getHwDesc();
        if (vendor.startsWith("Stanford University, Ericsson Research and CPqD Research")
                &&
                hw.startsWith("OpenFlow 1.3 Reference Userspace Switch")) {
            return new OFSwitchImplCPqD13(dpid, desc, cpqdUsePipeline13);
        }

        if (vendor.startsWith("Nicira") &&
                hw.startsWith("Open vSwitch")) {
            if (ofv == OFVersion.OF_10) {
                return new OFSwitchImplOVS10(dpid, desc);
            } else if (ofv == OFVersion.OF_13) {
                return new OFSwitchImplOVS13(dpid, desc);
            }
        }

        String sw = desc.getSwDesc();
        if (sw.startsWith("LINC-OE")) {
            log.warn("Optical Emulator LINC-OE with DPID:{} found..", dpid);
            return new OFOpticalSwitchImplLINC13(dpid, desc);
        }

        log.warn("DriverManager could not identify switch desc: {}. "
                + "Assigning AbstractOpenFlowSwich", desc);
        return new AbstractOpenFlowSwitch(dpid, desc) {

            @Override
            public void setRole(RoleState state) {
                this.role = RoleState.MASTER;
            }

            @Override
            public void write(List<OFMessage> msgs) {
                channel.write(msgs);
            }

            @Override
            public void write(OFMessage msg) {
                channel.write(Collections.singletonList(msg));

            }

            @Override
            public Boolean supportNxRole() {
                return false;
            }

            @Override
            public void startDriverHandshake() {}

            @Override
            public void processDriverHandshakeMessage(OFMessage m) {}

            @Override
            public boolean isDriverHandshakeComplete() {
                return true;
            }

            @Override
            public List<OFPortDesc> getPorts() {
                if (this.factory().getVersion() == OFVersion.OF_10) {
                    return Collections.unmodifiableList(features.getPorts());
                } else {
                    return Collections.unmodifiableList(ports.getEntries());
                }
            }
        };
    }

    /**
     * Private constructor to avoid instantiation.
     */
    private DriverManager() {
    }

    /**
     * Sets the configuration parameter which determines how the CPqD switch
     * is set up. If usePipeline13 is true, a 1.3 pipeline will be set up on
     * the switch. Otherwise, the switch will be set up in a 1.0 style with
     * a single table where missed packets are sent to the controller.
     *
     * @param usePipeline13 whether to use a 1.3 pipeline or not
     */
    public static void setConfigForCpqd(boolean usePipeline13) {
        cpqdUsePipeline13 = usePipeline13;
    }

    public static OpenFlowSwitchDriver getSwitch(Dpid dpid,
            OFDescStatsReply desc, OFVersion ofv) {
        return new DriverManager().getOFSwitchImpl(dpid, desc, ofv);
    }

}
