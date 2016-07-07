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

import org.onosproject.net.Host;
import org.onosproject.openstackinterface.OpenstackFloatingIP;

/**
 * Handle FloatingIP Event for Managing Flow Rules In Openstack Nodes.
 */
public class OpenstackFloatingIPHandler implements Runnable {

    public enum Action {
        ASSOCIATE,
        DISSASSOCIATE
    }

    private final OpenstackFloatingIP floatingIP;
    private final OpenstackRoutingRulePopulator rulePopulator;
    private final Host host;
    private final Action action;


    OpenstackFloatingIPHandler(OpenstackRoutingRulePopulator rulePopulator,
                               OpenstackFloatingIP openstackFloatingIP, Action action, Host host) {
        this.floatingIP = openstackFloatingIP;
        this.rulePopulator = rulePopulator;
        this.action = action;
        this.host = host;
    }

    @Override
    public void run() {
        if (action == Action.ASSOCIATE) {
            rulePopulator.populateFloatingIpRules(floatingIP);
        } else {
            rulePopulator.removeFloatingIpRules(floatingIP, host);
        }
    }
}
