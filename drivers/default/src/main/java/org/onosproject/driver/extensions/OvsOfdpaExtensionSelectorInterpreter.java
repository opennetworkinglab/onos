/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.driver.extensions;

import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaActsetOutput;
import org.projectfloodlight.openflow.types.U32;

/**
 * Interpreter for OFDPA OpenFlow selector extensions for OVS.
 */
public class OvsOfdpaExtensionSelectorInterpreter extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(
                ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            PortNumber port = ((OfdpaMatchActsetOutput) extensionSelector).port();
            return factory.oxms().ofdpaActsetOutput(U32.of(port.toLong()));
        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionSelector: " + extensionSelector.toString());
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {
        if (oxm.getMatchField().equals(MatchField.OFDPA_ACTSET_OUTPUT)) {
            U32 portNumberU32 = ((OFOxmOfdpaActsetOutput) oxm).getValue();
            PortNumber portNumber = PortNumber.portNumber(portNumberU32.getValue());
            return new OfdpaMatchActsetOutput(portNumber);
        }
        throw new UnsupportedOperationException(
                "Unexpected OXM: " + oxm.toString());
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            return new OfdpaMatchActsetOutput();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
