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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

import org.onlab.packet.Ip4Address;
import org.onosproject.codec.CodecContext;
import org.onosproject.net.NshContextHeader;
import org.onosproject.net.NshServiceIndex;
import org.onosproject.net.NshServicePathId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ExtensionTreatmentResolver;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.instructions.ExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;
import org.onosproject.openflow.controller.ExtensionTreatmentInterpreter;
import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionExperimenter;
import org.projectfloodlight.openflow.protocol.action.OFActionNicira;
import org.projectfloodlight.openflow.protocol.action.OFActionNiciraMove;
import org.projectfloodlight.openflow.protocol.action.OFActionNiciraResubmit;
import org.projectfloodlight.openflow.protocol.action.OFActionNiciraResubmitTable;
import org.projectfloodlight.openflow.protocol.action.OFActionSetField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEncapEthDst;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEncapEthSrc;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmEncapEthType;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshC1;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshC2;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshC3;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshC4;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshMdtype;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNshNp;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNsi;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmNsp;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTunGpeNp;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmTunnelIpv4Dst;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U8;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Interpreter for Nicira OpenFlow treatment extensions.
 */
public class NiciraExtensionTreatmentInterpreter extends AbstractHandlerBehaviour
        implements ExtensionTreatmentInterpreter, ExtensionTreatmentResolver {

    private static final int TYPE_NICIRA = 0x2320;
    private static final int SRC_ARP_SHA = 0x00012206;
    private static final int SRC_ARP_SPA = 0x00002004;
    private static final int SRC_ETH = 0x00000406;
    private static final int SRC_IP = 0x00000e04;

    private static final int NSH_C1 = 0x0001e604;
    private static final int NSH_C2 = 0x0001e804;
    private static final int NSH_C3 = 0x0001ea04;
    private static final int NSH_C4 = 0x0001ec04;
    private static final int TUN_IPV4_DST = 0x00014004;
    private static final int TUN_ID = 0x12008;

    private static final int SUB_TYPE_RESUBMIT = 1;
    private static final int SUB_TYPE_RESUBMIT_TABLE = 14;
    private static final int SUB_TYPE_MOVE = 6;
    private static final int SUB_TYPE_PUSH_NSH = 38;
    private static final int SUB_TYPE_POP_NSH = 39;

    private static final String TUNNEL_DST = "tunnelDst";
    private static final String RESUBMIT = "resubmit";
    private static final String RESUBMIT_TABLE = "resubmitTable";
    private static final String NICIRA_NSH_SPI = "niciraNshSpi";
    private static final String NICIRA_NSH_SI = "niciraNshSi";
    private static final String NICIRA_NSH_CH = "niciraNshCh";
    private static final String NICIRA_MOVE = "niciraMove";

    private static final String TYPE = "type";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in NiciraExtensionTreatmentInterpreter";

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
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_MDTYPE.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_NP.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_SRC.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_DST.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_TYPE
                .type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_POP_NSH.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_TUN_GPE_NP.type())) {
            return true;
        }
        if (extensionTreatmentType
                .equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C1_TO_C1.type())) {
            return true;
        }
        if (extensionTreatmentType
                .equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_C2.type())) {
            return true;
        }
        if (extensionTreatmentType
                .equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C3_TO_C3.type())) {
            return true;
        }
        if (extensionTreatmentType
                .equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C4_TO_C4.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes
                                          .NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST.type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_TUN_ID_TO_TUN_ID
                .type())) {
            return true;
        }
        if (extensionTreatmentType.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_TUN_ID
                                          .type())) {
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
            NiciraResubmit resubmit = (NiciraResubmit) extensionTreatment;
            return factory.actions().niciraResubmit((int) resubmit.inPort().toLong(),
                                                  resubmit.table());
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            NiciraResubmitTable resubmitTable = (NiciraResubmitTable) extensionTreatment;
            return factory.actions().niciraResubmitTable((int) resubmitTable.inPort().toLong(),
                                                         resubmitTable.table());
        }

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            NiciraSetNshSpi niciraNshSpi = (NiciraSetNshSpi) extensionTreatment;
            return factory.actions().setField(factory.oxms().nsp(U32.of(niciraNshSpi.nshSpi().servicePathId())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            NiciraSetNshSi niciraNshSi = (NiciraSetNshSi) extensionTreatment;
            return factory.actions().setField(factory.oxms().nsi(U8.of(niciraNshSi.nshSi().serviceIndex())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshC1(U32.of(niciraNshch.nshCh().nshContextHeader())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshC2(U32.of(niciraNshch.nshCh().nshContextHeader())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshC3(U32.of(niciraNshch.nshCh().nshContextHeader())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshC4(U32.of(niciraNshch.nshCh().nshContextHeader())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_MDTYPE.type())) {
            NiciraNshMdType niciraNshMdType = (NiciraNshMdType) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshMdtype(U8.of(niciraNshMdType.nshMdType())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_NP.type())) {
            NiciraNshNp niciraNshNp = (NiciraNshNp) extensionTreatment;
            return factory.actions().setField(factory.oxms().nshNp(U8.of(niciraNshNp.nshNp())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_SRC.type())) {
            NiciraEncapEthSrc niciraEncapEthSrc = (NiciraEncapEthSrc) extensionTreatment;
            return factory.actions().setField(factory.oxms().encapEthSrc(MacAddress.of(niciraEncapEthSrc.encapEthSrc()
                    .toBytes())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_DST.type())) {
            NiciraEncapEthDst niciraEncapEthDst = (NiciraEncapEthDst) extensionTreatment;
            return factory.actions().setField(factory.oxms().encapEthDst(MacAddress.of(niciraEncapEthDst.encapEthDst()
                    .toBytes())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_TYPE.type())) {
            NiciraEncapEthType niciraEncapEthType = (NiciraEncapEthType) extensionTreatment;
            return factory.actions().setField(factory.oxms().encapEthType(U16.of(niciraEncapEthType.encapEthType())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH.type())) {
            return factory.actions().niciraPushNsh();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_POP_NSH.type())) {
            return factory.actions().niciraPopNsh();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_TUN_GPE_NP.type())) {
            NiciraTunGpeNp niciraTunGpeNp = (NiciraTunGpeNp) extensionTreatment;
            return factory.actions().setField(factory.oxms().tunGpeNp(U8.of(niciraTunGpeNp.tunGpeNp())));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C1_TO_C1.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_C2.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C3_TO_C3.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C4_TO_C4.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST
                        .type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_TUN_ID_TO_TUN_ID.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_TUN_ID.type())) {
            MoveExtensionTreatment mov = (MoveExtensionTreatment) extensionTreatment;
            OFActionNiciraMove.Builder action = factory.actions()
                    .buildNiciraMove();
            action.setDstOfs(mov.dstOffset());
            action.setSrcOfs(mov.srcOffset());
            action.setNBits(mov.nBits());
            action.setSrc(mov.src());
            action.setDst(mov.dst());
            return action.build();
        }
        return null;
    }

    @Override
    public ExtensionTreatment mapAction(OFAction action) throws UnsupportedOperationException {
        if (action.getType().equals(OFActionType.SET_FIELD)) {
            OFActionSetField setFieldAction = (OFActionSetField) action;
            OFOxm<?> oxm = setFieldAction.getField();
            switch (oxm.getMatchField().id) {
            case TUNNEL_IPV4_DST:
                OFOxmTunnelIpv4Dst tunnelIpv4Dst = (OFOxmTunnelIpv4Dst) oxm;
                return new NiciraSetTunnelDst(Ip4Address.valueOf(tunnelIpv4Dst.getValue().getInt()));
            case NSP:
                OFOxmNsp nsp = (OFOxmNsp) oxm;
                return new NiciraSetNshSpi(NshServicePathId.of((nsp.getValue().getRaw())));
            case NSI:
                OFOxmNsi nsi = (OFOxmNsi) oxm;
                return new NiciraSetNshSi(NshServiceIndex.of((nsi.getValue().getRaw())));
            case NSH_C1:
                OFOxmNshC1 nshC1 = (OFOxmNshC1) oxm;
                return new NiciraSetNshContextHeader(NshContextHeader.of((nshC1.getValue().getRaw())),
                                                     ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1
                                                     .type());
            case NSH_C2:
                OFOxmNshC2 nshC2 = (OFOxmNshC2) oxm;
                return new NiciraSetNshContextHeader(NshContextHeader.of((nshC2.getValue().getRaw())),
                                                     ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2
                                                     .type());
            case NSH_C3:
                OFOxmNshC3 nshC3 = (OFOxmNshC3) oxm;
                return new NiciraSetNshContextHeader(NshContextHeader.of((nshC3.getValue().getRaw())),
                                                     ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3
                                                     .type());
            case NSH_C4:
                OFOxmNshC4 nshC4 = (OFOxmNshC4) oxm;
                return new NiciraSetNshContextHeader(NshContextHeader.of((nshC4.getValue().getRaw())),
                                                     ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4
                                                     .type());
            case NSH_MDTYPE:
                OFOxmNshMdtype nshMdType = (OFOxmNshMdtype) oxm;
                return new NiciraNshMdType((nshMdType.getValue().getRaw()));
            case NSH_NP:
                OFOxmNshNp nshNp = (OFOxmNshNp) oxm;
                return new NiciraNshNp((nshNp.getValue().getRaw()));
            case ENCAP_ETH_SRC:
                OFOxmEncapEthSrc encapEthSrc = (OFOxmEncapEthSrc) oxm;
                return new NiciraEncapEthSrc(org.onlab.packet.MacAddress.valueOf((encapEthSrc.getValue().getBytes())));
            case ENCAP_ETH_DST:
                OFOxmEncapEthDst encapEthDst = (OFOxmEncapEthDst) oxm;
                return new NiciraEncapEthDst(org.onlab.packet.MacAddress.valueOf((encapEthDst.getValue().getBytes())));
            case ENCAP_ETH_TYPE:
                OFOxmEncapEthType encapEthType = (OFOxmEncapEthType) oxm;
                return new NiciraEncapEthType((encapEthType.getValue().getRaw()));
            case TUN_GPE_NP:
                OFOxmTunGpeNp tunGpeNp = (OFOxmTunGpeNp) oxm;
                return new NiciraTunGpeNp((tunGpeNp.getValue().getRaw()));
            default:
                throw new UnsupportedOperationException(
                        "Driver does not support extension type " + oxm.getMatchField().id);
            }
        }
        if (action.getType().equals(OFActionType.EXPERIMENTER)) {
            OFActionExperimenter experimenter = (OFActionExperimenter) action;
            if (Long.valueOf(experimenter.getExperimenter())
                    .intValue() == TYPE_NICIRA) {
                OFActionNicira nicira = (OFActionNicira) experimenter;
                switch (nicira.getSubtype()) {
                    case SUB_TYPE_MOVE:
                        OFActionNiciraMove moveAction = (OFActionNiciraMove) nicira;
                        switch (Long.valueOf(moveAction.getSrc()).intValue()) {
                            case SRC_ARP_SHA:
                                return NiciraMoveTreatmentFactory
                                        .createNiciraMovArpShaToTha();
                            case SRC_ETH:
                                return NiciraMoveTreatmentFactory
                                        .createNiciraMovEthSrcToDst();
                            case SRC_IP:
                                return NiciraMoveTreatmentFactory
                                        .createNiciraMovIpSrcToDst();
                            case SRC_ARP_SPA:
                                return NiciraMoveTreatmentFactory
                                        .createNiciraMovArpSpaToTpa();
                    case NSH_C1:
                        return NiciraMoveTreatmentFactory.createNiciraMovNshC1ToC1();
                    case NSH_C2:
                        if (Long.valueOf(moveAction.getDst()).intValue() == TUN_ID) {
                            return NiciraMoveTreatmentFactory.createNiciraMovNshC2ToTunId();
                        }
                        return NiciraMoveTreatmentFactory.createNiciraMovNshC2ToC2();
                    case NSH_C3:
                        return NiciraMoveTreatmentFactory.createNiciraMovNshC3ToC3();
                    case NSH_C4:
                        return NiciraMoveTreatmentFactory.createNiciraMovNshC4ToC4();
                    case TUN_IPV4_DST:
                        return NiciraMoveTreatmentFactory.createNiciraMovTunDstToTunDst();
                    case TUN_ID:
                        return NiciraMoveTreatmentFactory.createNiciraMovTunIdToTunId();
                            default:
                                throw new UnsupportedOperationException("Driver does not support move from "
                                + moveAction.getSrc() + " to " + moveAction.getDst() + "of length "
                                + moveAction.getNBits());
                        }
                    case SUB_TYPE_RESUBMIT:
                        OFActionNiciraResubmit resubmitAction = (OFActionNiciraResubmit) nicira;
                        return new NiciraResubmit(PortNumber.portNumber(resubmitAction.getInPort()));
                case SUB_TYPE_PUSH_NSH:
                    return new NiciraPushNsh();
                case SUB_TYPE_POP_NSH:
                    return new NiciraPopNsh();
                case SUB_TYPE_RESUBMIT_TABLE:
                    OFActionNiciraResubmitTable resubmitTable = (OFActionNiciraResubmitTable) nicira;
                    return new NiciraResubmitTable(PortNumber.portNumber(resubmitTable.getInPort()),
                                                   resubmitTable.getTable());
                    default:
                        throw new UnsupportedOperationException("Driver does not support extension subtype "
                                + nicira.getSubtype());
                }
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
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_PUSH_NSH.type())) {
            return new NiciraPushNsh();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_POP_NSH.type())) {
            return new NiciraPopNsh();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_MDTYPE.type())) {
            return new NiciraNshMdType();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_NSH_NP.type())) {
            return new NiciraNshNp();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_SRC.type())) {
            return new NiciraEncapEthSrc();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_DST.type())) {
            return new NiciraEncapEthDst();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_ENCAP_ETH_TYPE.type())) {
            return new NiciraEncapEthType();
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
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_TUN_GPE_NP.type())) {
            return new NiciraTunGpeNp();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C1_TO_C1.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovNshC1ToC1();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_C2.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovNshC2ToC2();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C3_TO_C3.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovNshC3ToC3();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C4_TO_C4.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovNshC4ToC4();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_TUN_IPV4_DST_TO_TUN_IPV4_DST
                .type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovTunDstToTunDst();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_TUN_ID_TO_TUN_ID.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovTunIdToTunId();
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_NSH_C2_TO_TUN_ID.type())) {
            return NiciraMoveTreatmentFactory.createNiciraMovNshC2ToTunId();
        }
        throw new UnsupportedOperationException("Driver does not support extension type " + type.toString());
    }

    @Override
    public ObjectNode encode(ExtensionTreatment extensionTreatment, CodecContext context) {
        checkNotNull(extensionTreatment, "Extension treatment cannot be null");
        ExtensionTreatmentType type = extensionTreatment.type();
        ObjectNode root = context.mapper().createObjectNode();

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type())) {
            NiciraSetTunnelDst tunnelDst = (NiciraSetTunnelDst) extensionTreatment;
            root.set(TUNNEL_DST, context.codec(NiciraSetTunnelDst.class).encode(tunnelDst, context));
        }

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type())) {
            NiciraResubmit resubmit = (NiciraResubmit) extensionTreatment;
            root.set(RESUBMIT, context.codec(NiciraResubmit.class).encode(resubmit, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            NiciraResubmitTable resubmitTable = (NiciraResubmitTable) extensionTreatment;
            root.set(RESUBMIT_TABLE, context.codec(NiciraResubmitTable.class).encode(resubmitTable, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            NiciraSetNshSpi niciraNshSpi = (NiciraSetNshSpi) extensionTreatment;
            root.set(NICIRA_NSH_SPI, context.codec(NiciraSetNshSpi.class).encode(niciraNshSpi, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            NiciraSetNshSi niciraNshSi = (NiciraSetNshSi) extensionTreatment;
            root.set(NICIRA_NSH_SI, context.codec(NiciraSetNshSi.class).encode(niciraNshSi, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            root.set(NICIRA_NSH_CH, context.codec(NiciraSetNshContextHeader.class).encode(niciraNshch, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            root.set(NICIRA_NSH_CH, context.codec(NiciraSetNshContextHeader.class).encode(niciraNshch, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            root.set(NICIRA_NSH_CH, context.codec(NiciraSetNshContextHeader.class).encode(niciraNshch, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            NiciraSetNshContextHeader niciraNshch = (NiciraSetNshContextHeader) extensionTreatment;
            root.set(NICIRA_NSH_CH, context.codec(NiciraSetNshContextHeader.class).encode(niciraNshch, context));
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())) {
            MoveExtensionTreatment mov = (MoveExtensionTreatment) extensionTreatment;
            root.set(NICIRA_MOVE, context.codec(MoveExtensionTreatment.class).encode(mov, context));
        }

        return root;
    }

    @Override
    public ExtensionTreatment decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse extension type
        int typeInt = nullIsIllegal(json.get(TYPE), TYPE + MISSING_MEMBER_MESSAGE).asInt();
        ExtensionTreatmentType type = new ExtensionTreatmentType(typeInt);

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_TUNNEL_DST.type())) {
            return context.codec(NiciraSetTunnelDst.class).decode(json, context);
        }

        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT.type())) {
            return context.codec(NiciraResubmit.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_RESUBMIT_TABLE.type())) {
            return context.codec(NiciraResubmitTable.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SPI.type())) {
            return context.codec(NiciraSetNshSpi.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_SI.type())) {
            return context.codec(NiciraSetNshSi.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH1.type())) {
            return context.codec(NiciraSetNshContextHeader.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH2.type())) {
            return context.codec(NiciraSetNshContextHeader.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH3.type())) {
            return context.codec(NiciraSetNshContextHeader.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_SET_NSH_CH4.type())) {
            return context.codec(NiciraSetNshContextHeader.class).decode(json, context);
        }
        if (type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SHA_TO_THA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ARP_SPA_TO_TPA.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_ETH_SRC_TO_DST.type())
                || type.equals(ExtensionTreatmentType.ExtensionTreatmentTypes.NICIRA_MOV_IP_SRC_TO_DST.type())) {
            return context.codec(MoveExtensionTreatment.class).decode(json, context);
        }
        throw new UnsupportedOperationException(
                "Driver does not support extension type " + type.toString());
    }
}
