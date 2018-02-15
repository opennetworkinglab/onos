/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.lumentum;

import org.onosproject.driver.optical.flowrule.CrossConnectFlowRule;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lumentum cross connection abstraction.
 *
 * Required to store all information needed by Lumentum device not included in CrossConnectFlowRule.
 */
public class LumentumConnection {

    private static final Logger log = LoggerFactory.getLogger(LumentumConnection.class);

    protected boolean isAddRule;
    protected int connectionId;
    protected int connectionModule;
    protected int hashCode;
    protected PortNumber inPortNumber;
    protected PortNumber outPortNumber;
    protected OchSignal ochSignal;

    protected double attenuation;
    protected double targetAttenuation;
    protected double targetPower;
    protected double inputPower;
    protected double outputPower;

    //TODO: compute target attenuation to obtain the desired targetPower

    /**
     * Builds a LumentumConnection.
     *
     * @param id the id retrieved from the device
     * @param flowRuleHash the hash code associated to the Flow Rule
     * @param xc the cross connect flow rule
     */
    public LumentumConnection(Integer id, Integer flowRuleHash, CrossConnectFlowRule xc) {

        connectionId = id;
        hashCode = flowRuleHash;

        isAddRule = xc.isAddRule();
        ochSignal = xc.ochSignal();
        attenuation = 0.0; //dB
        inputPower = 0.0;  //dBm
        outputPower = 0.0; //dBm

        if (isAddRule) {
            outPortNumber = LumentumNetconfRoadmFlowRuleProgrammable.LINE_PORT_NUMBER;
            inPortNumber = xc.addDrop();
        } else {
            outPortNumber = xc.addDrop();
            inPortNumber = LumentumNetconfRoadmFlowRuleProgrammable.LINE_PORT_NUMBER;
        }

        log.debug("Lumentum NETCONF inPort {} outPort {} ochSignal {}", inPortNumber, outPortNumber, xc.ochSignal());
    }

    protected Integer getConnectionId() {
        return connectionId;
    }

    protected Integer getHash() {
        return hashCode;
    }

    protected void setAttenuation(double att) {
        attenuation = att;
    }

    protected void setInputPower(double power) {
        inputPower = power;
    }

    protected void setOutputPower(double power) {
        outputPower = power;
    }

}
