/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.net.flowobjective.Objective;
import org.onosproject.net.NshServicePathId;
import org.onosproject.vtnrsc.PortChain;

/**
 * Abstraction of an entity which provides Service function forwarder.
 */
public interface ServiceFunctionForwarderService {

    /**
     * Install Forwarding rule.
     *
     * @param portChain port-chain
     * @param nshSPI nsh spi
     */
    void installForwardingRule(PortChain portChain, NshServicePathId nshSPI);

    /**
     * Uninstall Forwarding rule.
     *
     * @param portChain port-chain
     * @param nshSPI nsh spi
     */
    void unInstallForwardingRule(PortChain portChain, NshServicePathId nshSPI);

    /**
     * Prepare forwarding object for Service Function.
     *
     * @param portChain port-chain
     * @param nshSPI nsh spi
     * @param type forwarding objective operation type
     */
    void prepareServiceFunctionForwarder(PortChain portChain, NshServicePathId nshSPI, Objective.Operation type);
}
