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
package org.onosproject.sfc.installer;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NshServicePathId;
import org.onosproject.vtnrsc.FiveTuple;
import org.onosproject.vtnrsc.PortChain;

/**
 * Abstraction of an entity which installs flow classification rules in ovs.
 */
public interface SfcFlowRuleInstallerService {

    /**
     * Install flow classifier.
     *
     * @param portChain port-chain
     * @param nshSpiId service path index identifier
     * @return connectPoint the network identifier
     */
    ConnectPoint installFlowClassifier(PortChain portChain, NshServicePathId nshSpiId);

    /**
     * Uninstall flow classifier.
     *
     * @param portChain port-chain
     * @param nshSpiId service path index identifier
     * @return connectPoint the network identifier
     */
    ConnectPoint unInstallFlowClassifier(PortChain portChain, NshServicePathId nshSpiId);

    /**
     * Install load balanced flow rules.
     *
     * @param portChain port-chain
     * @param fiveTuple five tuple packet information
     * @param nshSpiId service path index identifier
     * @return connectPoint the network identifier
     */
    ConnectPoint installLoadBalancedFlowRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId);

    /**
     * Uninstall load balanced flow rules.
     *
     * @param portChain port-chain
     * @param fiveTuple five tuple packet information
     * @param nshSpiId service path index identifier
     * @return connectPoint the network identifier
     */
    ConnectPoint unInstallLoadBalancedFlowRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId);

    /**
     * Uninstall load balanced classifier rules.
     *
     * @param portChain port-chain
     * @param fiveTuple five tuple packet information
     * @param nshSpiId service path index identifier
     * @return connectPoint the network identifier
     */
    ConnectPoint unInstallLoadBalancedClassifierRules(PortChain portChain, FiveTuple fiveTuple,
            NshServicePathId nshSpiId);
}
