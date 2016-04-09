/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.sfc.forwarder;

import java.util.List;

import org.onosproject.net.NshServicePathId;
import org.onosproject.vtnrsc.PortChain;
import org.onosproject.vtnrsc.PortPairId;

/**
 * Abstraction of an entity which provides service function forwarder.
 */
public interface ServiceFunctionForwarderService {

    /**
     * Install forwarding rule.
     *
     * @param portChain port-chain
     * @param nshSpi nsh spi
     */
    @Deprecated
    void installForwardingRule(PortChain portChain, NshServicePathId nshSpi);

    /**
     * Uninstall forwarding rule.
     *
     * @param portChain port-chain
     * @param nshSpi nsh spi
     */
    @Deprecated
    void unInstallForwardingRule(PortChain portChain, NshServicePathId nshSpi);

    /**
     * Install load balanced forwarding rules.
     *
     * @param path load balanced path of port pairs
     * @param nshSpi nsh service path index
     */
    void installLoadBalancedForwardingRule(List<PortPairId> path, NshServicePathId nshSpi);

    /**
     * Uninstall load balanced forwarding rules.
     *
     * @param path load balanced path of port pairs
     * @param nshSpi nsh service path index
     */
    void unInstallLoadBalancedForwardingRule(List<PortPairId> path, NshServicePathId nshSpi);
}
