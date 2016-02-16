/*
 * Copyright 2016 Open Networking Laboratory
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

import org.onosproject.net.packet.PacketContext;

/**
 * Handle ICMP packet processing for Managing Flow Rules In Openstack Nodes.
 */
public class OpenstackIcmpHandler implements Runnable {

    volatile PacketContext context;
    private OpenstackRoutingRulePopulator rulePopulator;
    OpenstackIcmpHandler(OpenstackRoutingRulePopulator rulePopulator, PacketContext context) {
        this.context = context;
        this.rulePopulator = rulePopulator;
    }

    @Override
    public void run() {
    }
}