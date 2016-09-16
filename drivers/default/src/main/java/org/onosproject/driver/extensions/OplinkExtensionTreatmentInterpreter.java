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

import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.protocol.action.OFActionOplinkAtt;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.U32;

/**
 * Interpreter for Oplink OpenFlow treatment extensions.
 */
public class OplinkExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter {

    private static final long ATTENUATION_EXP = 0xff000088L;

    @Override
    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionTreatment extensionTreatment) {
        ExtensionTreatmentType type = extensionTreatment.type();
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OPLINK_ATTENUATION.type())) {
            int att = ((OplinkAttenuation) extensionTreatment).getAttenuation();
            return factory.actions().oplinkAtt(factory.oxms().ochSigatt(U32.ofRaw(att)));
        }
        return null;
    }

    @Override
    public ExtensionTreatment mapAction(OFAction action) throws UnsupportedOperationException {
        if (action.getType().equals(OFActionType.EXPERIMENTER)) {
            OFActionExperimenter actionExp = (OFActionExperimenter) action;
            if (actionExp.getExperimenter() == ATTENUATION_EXP) {
                OFActionOplinkAtt actionAtt = (OFActionOplinkAtt) action;
                return new OplinkAttenuation(((OFOxm<U32>) actionAtt.getField()).getValue().getRaw());
            }
        }
        return null;
    }
}
