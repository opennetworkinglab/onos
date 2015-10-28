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
package org.onosproject.sfc;

import org.onosproject.vtnrsc.PortChain;

/**
 * SFC application that applies flows to the device.
 */
public interface SfcService {
    /**
     * Applies flow classification to OVS.
     *
     * @param portChain Port-Chain.
     */
    void InstallFlowClassification(PortChain portChain);


    /**
     * Remove flow classification from OVS.
     *
     * @param portChain Port-Chain.
     */
    void UnInstallFlowClassification(PortChain portChain);

    /**
     * Applies Service Function chain to OVS.
     *
     * @param portChain Port-Chain.
     */
    void InstallServiceFunctionChain(PortChain portChain);

    /**
     * Remove Service Function chain from OVS.
     *
     * @param portChain Port-Chain.
     */
    void UnInstallServiceFunctionChain(PortChain portChain);
}
