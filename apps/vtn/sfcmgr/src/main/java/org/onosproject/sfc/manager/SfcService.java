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
package org.onosproject.sfc.manager;

import org.onosproject.vtnrsc.PortPair;
import org.onosproject.vtnrsc.PortPairGroup;
import org.onosproject.vtnrsc.FlowClassifier;
import org.onosproject.vtnrsc.PortChain;

/**
 * SFC application that applies flows to the device.
 */
public interface SfcService {

    /**
     * When port-pair is created, check whether Forwarding Rule needs to be
     * updated in OVS.
     *
     * @param portPair port-pair
     */
    void onPortPairCreated(PortPair portPair);

    /**
     * When port-pair is deleted, check whether Forwarding Rule needs to be
     * updated in OVS.
     *
     * @param portPair port-pair
     */
    void onPortPairDeleted(PortPair portPair);

    /**
     * When port-pair-group is created, check whether Forwarding Rule needs to
     * be updated in OVS.
     *
     * @param portPairGroup port-pair-group
     */
    void onPortPairGroupCreated(PortPairGroup portPairGroup);

    /**
     * When port-pair-group is deleted, check whether Forwarding Rule needs to
     * be updated in OVS.
     *
     * @param portPairGroup port-pair-group
     */
    void onPortPairGroupDeleted(PortPairGroup portPairGroup);

    /**
     * When flow-classifier is created, check whether Forwarding Rule needs to
     * be updated in OVS.
     *
     * @param flowClassifier flow-classifier
     */
    void onFlowClassifierCreated(FlowClassifier flowClassifier);

    /**
     * When flow-classifier is deleted, check whether Forwarding Rule needs to
     * be updated in OVS.
     *
     * @param flowClassifier flow-classifier
     */
    void onFlowClassifierDeleted(FlowClassifier flowClassifier);

    /**
     * When port-chain is created, check whether Forwarding Rule needs to be
     * updated in OVS.
     *
     * @param portChain port-chain
     */
    void onPortChainCreated(PortChain portChain);

    /**
     * When port-chain is deleted, check whether Forwarding Rule needs to be
     * updated in OVS.
     *
     * @param portChain port-chain
     */
    void onPortChainDeleted(PortChain portChain);
}
