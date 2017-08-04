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

import org.onlab.packet.VlanId;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmVlanVid;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;

/**
 * Interpreter for OFDPA OpenFlow treatment extensions.
 */
public class OfdpaExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {
    @Override
    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_VLAN_ID.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionTreatment extensionTreatment) {
        ExtensionTreatmentType type = extensionTreatment.type();
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_VLAN_ID.type())) {
            VlanId vlanId = ((OfdpaSetVlanVid) extensionTreatment).vlanId();
            // NOTE: OFDPA requires isPresent bit set to zero.
            OFVlanVidMatch match = OFVlanVidMatch.ofRawVid(vlanId.toShort());
            return factory.actions().setField(factory.oxms().vlanVid(match));
        }
        throw new UnsupportedOperationException(
                "Unexpected ExtensionTreatment: " + extensionTreatment.toString());
    }

    @Override
    public ExtensionTreatment mapAction(OFAction action) throws UnsupportedOperationException {
        if (action.getType().equals(OFActionType.SET_FIELD)) {
            OFActionSetField setFieldAction = (OFActionSetField) action;
            OFOxm<?> oxm = setFieldAction.getField();
            switch (oxm.getMatchField().id) {
                case VLAN_VID:
                    OFOxmVlanVid vlanVid = (OFOxmVlanVid) oxm;
                    return new OfdpaSetVlanVid(VlanId.vlanId(vlanVid.getValue().getRawVid()));
                default:
                    throw new UnsupportedOperationException(
                            "Driver does not support extension type " + oxm.getMatchField().id);
            }
        }
        throw new UnsupportedOperationException(
                "Unexpected OFAction: " + action.toString());
    }

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_VLAN_ID.type())) {
            return new OfdpaSetVlanVid();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
