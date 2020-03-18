/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.drivers.server.devices.nic;

import com.google.common.base.Strings;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.drivers.server.Constants.MSG_NIC_FLOW_RULE_SCOPE_NULL;

/**
* Definition of network interface card's (NIC) rule's scope.
*/
public enum NicRuleScope {

    /**
     * A NIC rules is applied to ingress traffic.
     */
    INGRESS("ingress"),
    /**
     * A NIC rules is applied to egress traffic.
     */
    EGRESS("egress");

    protected String scope;

    private NicRuleScope(String scope) {
        checkArgument(!Strings.isNullOrEmpty(scope), MSG_NIC_FLOW_RULE_SCOPE_NULL);
        this.scope = scope.toLowerCase();
    }

    @Override
    public String toString() {
        return scope;
    }

}
