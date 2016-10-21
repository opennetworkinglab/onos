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

import org.onlab.packet.VlanId;
import org.onosproject.net.behaviour.ExtensionSelectorResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.criteria.ExtensionSelector;
import org.onosproject.net.flow.criteria.ExtensionSelectorType;
import org.onosproject.net.flow.criteria.ExtensionSelectorType.ExtensionSelectorTypes;
import org.onosproject.openflow.controller.ExtensionSelectorInterpreter;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaMplsL2Port;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaOvid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVidMasked;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interpreter for OFDPA3 OpenFlow selector extensions.
 */
public class Ofdpa3ExtensionSelectorInterpreter extends AbstractHandlerBehaviour
        implements ExtensionSelectorInterpreter, ExtensionSelectorResolver {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean supported(ExtensionSelectorType extensionSelectorType) {
        if (extensionSelectorType.equals(ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            return true;
        } else if (extensionSelectorType.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
            return true;
        } else if (extensionSelectorType.equals(ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFOxm<?> mapSelector(OFFactory factory, ExtensionSelector extensionSelector) {
        ExtensionSelectorType type = extensionSelector.type();
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            VlanId vlanId = ((OfdpaMatchVlanVid) extensionSelector).vlanId();
            // Special VLAN 0x0000/0x1FFF required by OFDPA
            if (vlanId.equals(VlanId.NONE)) {
                OFVlanVidMatch vid = OFVlanVidMatch.ofRawVid((short) 0x0000);
                OFVlanVidMatch mask = OFVlanVidMatch.ofRawVid((short) 0x1FFF);
                return factory.oxms().vlanVidMasked(vid, mask);
                // Normal case
            } else if (vlanId.equals(VlanId.ANY)) {
                return factory.oxms().vlanVidMasked(OFVlanVidMatch.PRESENT, OFVlanVidMatch.PRESENT);
            } else {
                return factory.oxms().vlanVid(OFVlanVidMatch.ofVlanVid(VlanVid.ofVlan(vlanId.toShort())));
            }
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
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
        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionSelector: " + extensionSelector.toString());
    }

    @Override
    public ExtensionSelector mapOxm(OFOxm<?> oxm) {

        if (oxm.getMatchField().equals(MatchField.VLAN_VID)) {
            VlanId vlanId;
            if (oxm.isMasked()) {
                OFVlanVidMatch vid = ((OFOxmVlanVidMasked) oxm).getValue();
                OFVlanVidMatch mask = ((OFOxmVlanVidMasked) oxm).getMask();

                if (vid.equals(OFVlanVidMatch.ofRawVid((short) 0))) {
                    vlanId = VlanId.NONE;
                } else if (vid.equals(OFVlanVidMatch.PRESENT) &&
                        mask.equals(OFVlanVidMatch.PRESENT)) {
                    vlanId = VlanId.ANY;
                } else {
                    vlanId = VlanId.vlanId(vid.getVlan());
                }
            } else {
                OFVlanVidMatch vid = ((OFOxmVlanVid) oxm).getValue();

                if (!vid.isPresentBitSet()) {
                    vlanId = VlanId.NONE;
                } else {
                    vlanId = VlanId.vlanId(vid.getVlan());
                }
            }
            return new OfdpaMatchVlanVid(vlanId);
        } else if (oxm.getMatchField().equals(MatchField.OFDPA_OVID)) {
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
        }
        throw new UnsupportedOperationException(
                "Unexpected OXM: " + oxm.toString());
    }

    @Override
    public ExtensionSelector getExtensionSelector(ExtensionSelectorType type) {
        if (type.equals(ExtensionSelectorType.ExtensionSelectorTypes.OFDPA_MATCH_VLAN_VID.type())) {
            return new OfdpaMatchVlanVid();
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_OVID.type())) {
            return new Ofdpa3MatchOvid();
        } else if (type.equals(ExtensionSelectorTypes.OFDPA_MATCH_MPLS_L2_PORT.type())) {
            return new Ofdpa3MatchMplsL2Port();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
