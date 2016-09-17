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
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaMplsType;
import org.projectfloodlight.openflow.types.U16;

/**
 * Interpreter for OFDPA3 OpenFlow treatment extensions.
 */
public class Ofdpa3ExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {
    @Override
    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionTreatment extensionTreatment) {
        ExtensionTreatmentType type = extensionTreatment.type();
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type())) {
            short mplsType = ((Ofdpa3SetMplsType) extensionTreatment).mplsType();
            return factory.actions().setField(factory.oxms().ofdpaMplsType(
                    U16.ofRaw(mplsType)));
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
                case OFDPA_MPLS_TYPE:
                    OFOxmOfdpaMplsType mplsType = (OFOxmOfdpaMplsType) oxm;
                    return new Ofdpa3SetMplsType(mplsType.getValue().getRaw());
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
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type())) {
            return new Ofdpa3SetMplsType();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
