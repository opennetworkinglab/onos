/*
 * Copyright 2016-present Open Networking Foundation
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
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
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
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaAllowVlanTranslation;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaMplsL2Port;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaOvid;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Interpreter for OFDPA3 OpenFlow selector extensions.
 */
public class Ofdpa3ExtensionSelectorInterpreter extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    private final Logger log = getLogger(getClass());

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
            return true;
        } else if (extensionSelectorType.equals(ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type())) {
            return true;
        } else if (extensionSelectorType.equals(
                ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            return true;
        } else if (extensionSelectorType.equals(
                ExtensionSelectorTypes.OFDPA_MATCH_ALLOW_VLAN_TRANSLATION.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
            VlanId vlanId = ((Ofdpa3MatchOvid) extensionSelector).vlanId();
            if (vlanId.equals(VlanId.NONE)) {
                throw new UnsupportedOperationException(
                        "Unexpected ExtensionSelector: " + extensionSelector.toString());
            } else if (vlanId.equals(VlanId.ANY)) {
                throw new UnsupportedOperationException(
                        "Unexpected ExtensionSelector: " + extensionSelector.toString());
            } else {
                short mask = (short) 0x1000;
                short oVid = (short) (mask | vlanId.toShort());
                return factory.oxms().ofdpaOvid(U16.ofRaw(oVid));
            }
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type())) {
            int mplsL2Port = ((Ofdpa3MatchMplsL2Port) extensionSelector).mplsL2Port();
            /*
             * 0x0000XXXX UNI Interface.
             * 0x0002XXXX NNI Interface
             */
            if ((mplsL2Port >= 0 && mplsL2Port <= 0x0000FFFF) ||
                    (mplsL2Port >= 0x00020000 && mplsL2Port <= 0x0002FFFF)) {
                return factory.oxms().ofdpaMplsL2Port(U32.ofRaw(mplsL2Port));
            }
            throw new UnsupportedOperationException(
                        "Unexpected ExtensionSelector: " + extensionSelector.toString());
        } else if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            PortNumber port = ((OfdpaMatchActsetOutput) extensionSelector).port();
            return factory.oxms().ofdpaActsetOutput(U32.of(port.toLong()));
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_ALLOW_VLAN_TRANSLATION.type())) {
            Short allowVlanTranslation =
                    ((OfdpaMatchAllowVlanTranslation) extensionSelector).allowVlanTranslation();
            return factory.oxms().ofdpaAllowVlanTranslation(U8.of(allowVlanTranslation));
        }

        throw new UnsupportedOperationException(
                "Unexpected ExtensionSelector: " + extensionSelector.toString());
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {
        if (oxm.getMatchField().equals(MatchField.OFDPA_OVID)) {
            VlanId vlanId;
            if (oxm.isMasked()) {
                throw new UnsupportedOperationException(
                        "Unexpected OXM: " + oxm.toString());
            } else {
                OFOxmOfdpaOvid ovid = ((OFOxmOfdpaOvid) oxm);
                short mask = (short) 0x0FFF;
                short oVid = (short) (mask & ovid.getValue().getRaw());
                vlanId = VlanId.vlanId(oVid);
                }
            return new Ofdpa3MatchOvid(vlanId);
        } else if (oxm.getMatchField().equals(MatchField.OFDPA_MPLS_L2_PORT)) {
            Integer mplsL2Port;
            /*
             * Supported but not used for now.
             */
            if (oxm.isMasked()) {
                throw new UnsupportedOperationException(
                        "Unexpected OXM: " + oxm.toString());
            } else {
                OFOxmOfdpaMplsL2Port mplsl2port = ((OFOxmOfdpaMplsL2Port) oxm);
                mplsL2Port = mplsl2port.getValue().getRaw();
                /*
                 * 0x0000XXXX UNI Interface.
                 * 0x0002XXXX NNI Interface
                 */
                if ((mplsL2Port >= 0 && mplsL2Port <= 0x0000FFFF) ||
                        (mplsL2Port >= 0x00020000 && mplsL2Port <= 0x0002FFFF)) {
                    return new Ofdpa3MatchMplsL2Port(mplsL2Port);
                }
                throw new UnsupportedOperationException(
                        "Unexpected OXM: " + oxm.toString());
            }
        } else if (oxm.getMatchField().equals(MatchField.OFDPA_ACTSET_OUTPUT)) {
            U32 portNumberU32 = ((OFOxmOfdpaActsetOutput) oxm).getValue();
            PortNumber portNumber = PortNumber.portNumber(portNumberU32.getValue());
            return new OfdpaMatchActsetOutput(portNumber);
        } else if (oxm.getMatchField().equals(MatchField.OFDPA_ALLOW_VLAN_TRANSLATION)) {
            U8 value = ((OFOxmOfdpaAllowVlanTranslation) oxm).getValue();
            return new OfdpaMatchAllowVlanTranslation(value.getValue());
        }

        throw new UnsupportedOperationException(
                "Unexpected OXM: " + oxm.toString());
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
            return new Ofdpa3MatchOvid();
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type())) {
            return new Ofdpa3MatchMplsL2Port();
        }  else if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_ACTSET_OUTPUT.type())) {
            return new OfdpaMatchActsetOutput();
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_ALLOW_VLAN_TRANSLATION.type())) {
            return new OfdpaMatchAllowVlanTranslation();
        }

        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }

    @Override
    public ObjectNode encode(ExtensionSelector extensionSelector, CodecContext context) {
        // TODO
        log.warn("The encode method of Ofdpa3ExtensionSelectorInterpreter hasn't been implemented");
        return null;
    }

    @Override
    public ExtensionSelector decode(ObjectNode json, CodecContext context) {
        // TODO
        log.warn("The decode method of Ofdpa3ExtensionSelectorInterpreter hasn't been implemented");
        return null;
    }
}
