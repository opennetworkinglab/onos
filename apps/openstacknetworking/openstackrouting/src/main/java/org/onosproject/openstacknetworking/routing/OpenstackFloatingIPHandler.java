/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.openstacknetworking.routing;

import org.onosproject.openstackinterface.OpenstackFloatingIP;
import org.onosproject.openstacknetworking.OpenstackPortInfo;

/**
 * Handle FloatingIP Event for Managing Flow Rules In Openstack Nodes.
 */
public class OpenstackFloatingIPHandler implements Runnable {

    private final OpenstackFloatingIP floatingIP;
    private final OpenstackRoutingRulePopulator rulePopulator;
    private boolean associate;
    private final OpenstackPortInfo portInfo;

    OpenstackFloatingIPHandler(OpenstackRoutingRulePopulator rulePopulator,
                               OpenstackFloatingIP openstackFloatingIP, boolean associate, OpenstackPortInfo portInfo) {
        this.floatingIP = openstackFloatingIP;
        this.rulePopulator = rulePopulator;
        this.associate = associate;
        this.portInfo = portInfo;
    }

    @Override
    public void run() {
        if (associate) {
            rulePopulator.populateFloatingIpRules(floatingIP);
        } else {
            rulePopulator.removeFloatingIpRules(floatingIP, portInfo);
        }

    }
}
