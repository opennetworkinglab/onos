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

package org.onosproject.driver.extensions;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEncapEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNsi;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNsp;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;

/**
 * Interpreter for Nicira OpenFlow selector extensions.
 */
public class NiciraExtensionSelectorInterpreter
        extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            return true;
        }
        if (extensionSelectorType.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_ENCAP_ETH_TYPE
                                         .type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();

        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            NiciraMatchNshSpi niciraNshSpi = (NiciraMatchNshSpi) extensionSelector;
            return factory.oxms().nsp(U32.of(niciraNshSpi.nshSpi().servicePathId()));
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            NiciraMatchNshSi niciraNshSi = (NiciraMatchNshSi) extensionSelector;
            return factory.oxms().nsi(U8.of(niciraNshSi.nshSi().serviceIndex()));
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_ENCAP_ETH_TYPE.type())) {
            NiciraMatchEncapEthType niciraEncapEthType = (NiciraMatchEncapEthType) extensionSelector;
            return factory.oxms().encapEthType(U16.of(niciraEncapEthType.encapEthType()));
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())) {
            // TODO
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            // TODO
        }
        return null;
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {

        if (oxm.getMatchField() == MatchField.NSP) {
            OFOxmNsp oxmField = (OFOxmNsp) oxm;
            return new NiciraMatchNshSpi(NshServicePathId.of(oxmField.getValue().getRaw()));
        }
        if (oxm.getMatchField() == MatchField.NSI) {
            OFOxmNsi oxmField = (OFOxmNsi) oxm;
            return new NiciraMatchNshSi(NshServiceIndex.of(oxmField.getValue().getRaw()));
        }
        if (oxm.getMatchField() == MatchField.ENCAP_ETH_TYPE) {
            OFOxmEncapEthType oxmField = (OFOxmEncapEthType) oxm;
            return new NiciraMatchEncapEthType(oxmField.getValue().getRaw());
        }

        return null;
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SPI.type())) {
            return new NiciraMatchNshSpi();
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_SI.type())) {
            return new NiciraMatchNshSi();
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_ENCAP_ETH_TYPE.type())) {
            return new NiciraMatchEncapEthType();
        }
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH1.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH2.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH3.type())
                || type.equals(ExtensionSelectorType.ExtensionSelectorTypes.NICIRA_MATCH_NSH_CH4.type())) {
            return new NiciraMatchNshContextHeader(type);
        }
        return null;
    }

    @Override
    public ObjectNode encode(ExtensionSelector extensionSelector, CodecContext context) {
        // TODO
        return null;
    }

    @Override
    public ExtensionSelector decode(ObjectNode json, CodecContext context) {
        // TODO
        return null;
    }
}
