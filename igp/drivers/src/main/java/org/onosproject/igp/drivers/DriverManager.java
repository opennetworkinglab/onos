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
package org.onosproject.igp.drivers;

import org.onosproject.igp.controller.IgpDpid;
import org.onosproject.igp.controller.driver.AbstractIgpSwitch;
import org.onosproject.igp.controller.driver.IgpSwitchDriver;
import org.onosproject.igp.controller.driver.IgpSwitchDriverFactory;
import org.onosproject.net.flowext.FlowRuleBatchExtRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * A simple implementation of a driver manager that differentiates between
 * connected switches using the OF Description Statistics Reply message.
 */
public final class DriverManager implements IgpSwitchDriverFactory {

    private static final Logger log = LoggerFactory.getLogger(DriverManager.class);
    /**
     * Return an IOFSwitch object based on switch's manufacturer description
     * from OFDescStatsReply.
     *
     * @param desc DescriptionStatistics reply from the switch
     * @return A IOFSwitch instance if the driver found an implementation for
     *         the given description. Otherwise it returns OFSwitchImplBase
     */
    @Override
    public AbstractIgpSwitch getOFSwitchImpl(IgpDpid dpid){
        return new AbstractIgpSwitch(dpid) {
            @Override
            public void write(List<FlowRuleBatchExtRequest> msgs) {
                channel.write(msgs);
            }

            @Override
            public void write(FlowRuleBatchExtRequest msg) {
                channel.write(Collections.singletonList(msg));

            }
        };
    }

    /**
     * Private constructor to avoid instantiation.
     */
    private DriverManager() {
    	log.info("IGP Driver");
    }

    public static IgpSwitchDriver getSwitch(IgpDpid dpid) {
        return new DriverManager().getOFSwitchImpl(dpid);
    }


}
