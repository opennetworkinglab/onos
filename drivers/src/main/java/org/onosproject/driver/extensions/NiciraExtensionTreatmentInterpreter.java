/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onlab.packet.Ip4Address;
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
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTunnelIpv4Dst;
import org.projectfloodlight.openflow.types.IPv4Address;

/**
 * Interpreter for Nicira OpenFlow treatment extensions.
 */
public class NiciraExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {

    @Override
    public boolean supported(ExtensionTreatmentType extensionTreatmentType) {
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(
                ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())) {
            return true;
        }
        return false;
    }

    @Override
    public OFAction mapInstruction(OFFactory factory, ExtensionTreatment extensionTreatment) {
        ExtensionTreatmentType type = extensionTreatment.type();
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type())) {
            NiciraSetTunnelDst tunnelDst = (NiciraSetTunnelDst) extensionTreatment;
            return factory.actions().setField(factory.oxms().tunnelIpv4Dst(
                    IPv4Address.of(tunnelDst.tunnelDst().toInt())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            // TODO this will be implemented later
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())) {
           // TODO this will be implemented later
        }
        return null;
    }

    @Override
    public ExtensionTreatment mapAction(OFAction action) {
        if (action.getType().equals(OFActionType.SET_FIELD)) {
            OFActionSetField setFieldAction = (OFActionSetField) action;
            OFOxm<?> oxm = setFieldAction.getField();
            switch (oxm.getMatchField().id) {
            case TUNNEL_IPV4_DST:
                OFOxmTunnelIpv4Dst tunnelIpv4Dst = (OFOxmTunnelIpv4Dst) oxm;
                return new NiciraSetTunnelDst(Ip4Address.valueOf(tunnelIpv4Dst.getValue().getInt()));
            default:
                throw new UnsupportedOperationException(
                        "Driver does not support extension type " + oxm.getMatchField().id);
            }
        }
        return null;
    }

    @Override
    public ExtensionTreatment getExtensionInstruction(ExtensionTreatmentType type) {
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type())) {
            return new NiciraSetTunnelDst();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type())) {
            return new NiciraResubmit();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            return new NiciraResubmitTable();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            return new NiciraSetNshSpi();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            return new NiciraSetNshSi();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            return new NiciraSetNshContextHeader(type);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovArpShaToTha();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovArpSpaToTpa();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovEthSrcToDst();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovIpSrcToDst();
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
