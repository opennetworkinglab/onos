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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import org.onlab.packet.EthType;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;

/**
 * Constants common to ForwardingFunctionType operations.
 */
final class Commons {

    static final Criterion MATCH_ETH_TYPE_IPV4 = Criteria.matchEthType(
      EthType.EtherType.IPV4.ethType());
    static final Criterion MATCH_ETH_TYPE_IPV6 = Criteria.matchEthType(
      EthType.EtherType.IPV6.ethType());
    static final Criterion MATCH_ETH_DST_NONE = Criteria.matchEthDst(
      MacAddress.NONE);
    static final Criterion MATCH_ETH_TYPE_MPLS = Criteria.matchEthType(
      EthType.EtherType.MPLS_UNICAST.ethType());
    static final Criterion MATCH_MPLS_BOS_TRUE = Criteria.matchMplsBos(true);
    static final Criterion MATCH_MPLS_BOS_FALSE = Criteria.matchMplsBos(false);

    private Commons() {
        // hides constructor.
    }
}
