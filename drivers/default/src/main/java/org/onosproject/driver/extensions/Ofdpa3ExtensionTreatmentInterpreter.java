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
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.protocol.action.OFActionOfdpa;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaMplsL2Port;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaMplsType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaOvid;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmOfdpaQosIndex;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interpreter for OFDPA3 OpenFlow treatment extensions.
 */
public class Ofdpa3ExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {

    private static final int TYPE_OFDPA = 0x1018;
    private static final int SUB_TYPE_PUSH_L2_HEADER = 1;
    private static final int SUB_TYPE_POP_L2_HEADER = 2;
    private static final int SUB_TYPE_PUSH_CW = 3;
    private static final int SUB_TYPE_POP_CW = 4;

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_OVID.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_L2_PORT.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_QOS_INDEX.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_L2_HEADER.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_CW.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_L2_HEADER.type())) {
            return true;
        } else if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_CW.type())) {
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
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_OVID.type())) {
            // OFDPA requires isPresent bit set to 1 for OVID.
            VlanId vlanId = ((Ofdpa3SetOvid) extensionTreatment).vlanId();
            short mask = (short) 0x1000;
            short oVid = (short) (mask | vlanId.toShort());
            return factory.actions().setField(factory.oxms().ofdpaOvid(
                    U16.ofRaw(oVid)));
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_L2_PORT.type())) {
            Integer mplsL2Port = ((Ofdpa3SetMplsL2Port) extensionTreatment).mplsL2Port();
            /*
             * 0x0000XXXX UNI Interface.
             * 0x0002XXXX NNI Interface
             */
            if ((mplsL2Port >= 0 && mplsL2Port <= 0x0000FFFF) ||
                    (mplsL2Port >= 0x00020000 && mplsL2Port <= 0x0002FFFF)) {
                return factory.actions().setField(
                        factory.oxms().ofdpaMplsL2Port(U32.ofRaw(mplsL2Port))
                );
            }
            throw new UnsupportedOperationException(
                    "Unexpected ExtensionTreatment: " + extensionTreatment.toString());
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_QOS_INDEX.type())) {
            Integer qosIndex = ((Ofdpa3SetQosIndex) extensionTreatment).qosIndex();
            /*
             * Qos index is a single byte [0...255]
             */
            if (qosIndex >= 0 && qosIndex <= 255) {
                return factory.actions().setField(
                        factory.oxms().ofdpaQosIndex(U8.ofRaw((byte) (qosIndex & 0xFF)))
                );
            }
            throw new UnsupportedOperationException(
                    "Unexpected ExtensionTreatment: " + extensionTreatment.toString());
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_L2_HEADER.type())) {
            return factory.actions().ofdpaPushL2Header();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_CW.type())) {
            return factory.actions().ofdpaPushCw();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_L2_HEADER.type())) {
            return factory.actions().ofdpaPopL2Header();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_CW.type())) {
            return factory.actions().ofdpaPopCw();
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
                case OFDPA_OVID:
                    OFOxmOfdpaOvid ovid = ((OFOxmOfdpaOvid) oxm);
                    short mask = (short) 0x0FFF;
                    short oVid = (short) (mask & ovid.getValue().getRaw());
                    VlanId vlanId = VlanId.vlanId(oVid);
                    return new Ofdpa3SetOvid(vlanId);
                case OFDPA_MPLS_L2_PORT:
                    OFOxmOfdpaMplsL2Port mplsl2Port = ((OFOxmOfdpaMplsL2Port) oxm);
                    Integer mplsL2Port = mplsl2Port.getValue().getRaw();
                    if ((mplsL2Port >= 0 && mplsL2Port <= 0x0000FFFF) ||
                            (mplsL2Port >= 0x00020000 && mplsL2Port <= 0x0002FFFF)) {
                        return new Ofdpa3SetMplsL2Port(mplsL2Port);
                    }
                    break;
                case OFDPA_QOS_INDEX:
                    OFOxmOfdpaQosIndex qosindex = ((OFOxmOfdpaQosIndex) oxm);
                    Integer qosIndex = (int) qosindex.getValue().getRaw();
                    if (qosIndex >= 0 && qosIndex <= 255) {
                        return new Ofdpa3SetQosIndex(qosIndex);
                    }
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Driver does not support extension type " + oxm.getMatchField().id);
            }
        } else if (action.getType().equals(OFActionType.EXPERIMENTER)) {
            OFActionExperimenter experimenter = (OFActionExperimenter) action;
            if (Long.valueOf(experimenter.getExperimenter()).intValue() == TYPE_OFDPA) {
                OFActionOfdpa ofdpa = (OFActionOfdpa) experimenter;
                switch (ofdpa.getExpType()) {
                    case SUB_TYPE_PUSH_L2_HEADER:
                        return new Ofdpa3PushL2Header();
                    case SUB_TYPE_POP_L2_HEADER:
                        return new Ofdpa3PopL2Header();
                    case SUB_TYPE_PUSH_CW:
                        return new Ofdpa3PushCw();
                    case SUB_TYPE_POP_CW:
                        return new Ofdpa3PopCw();
                    default:
                        throw new UnsupportedOperationException(
                                "Unexpected OFAction: " + action.toString());
                }
            }
            throw new UnsupportedOperationException(
                    "Unexpected OFAction: " + action.toString());
        }
        throw new UnsupportedOperationException(
                "Unexpected OFAction: " + action.toString());
    }

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_TYPE.type())) {
            return new Ofdpa3SetMplsType();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_OVID.type())) {
            return new Ofdpa3SetOvid();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_MPLS_L2_PORT.type())) {
            return new Ofdpa3SetMplsL2Port();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_SET_QOS_INDEX.type())) {
            return new Ofdpa3SetQosIndex();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_L2_HEADER.type())) {
            return new Ofdpa3PushL2Header();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_PUSH_CW.type())) {
            return new Ofdpa3PushCw();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_L2_HEADER.type())) {
            return new Ofdpa3PopL2Header();
        } else if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.OFDPA_POP_CW.type())) {
            return new Ofdpa3PopCw();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
