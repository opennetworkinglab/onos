/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import org.apache.commons.lang3.tuple.Pair;
import org.onlab.packet.Ip4Address;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.upf.ForwardingActionRule;
import org.onosproject.net.behaviour.upf.GtpTunnel;
import org.onosproject.net.behaviour.upf.PacketDetectionRule;
import org.onosproject.net.behaviour.upf.UpfInterface;
import org.onosproject.net.behaviour.upf.UpfProgrammableException;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.PiCriterion;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiAction;
import org.onosproject.net.pi.runtime.PiActionParam;
import org.onosproject.net.pi.runtime.PiTableAction;

import java.util.Arrays;

import static org.onosproject.pipelines.fabric.FabricConstants.CTR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.DROP;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_DOWNLINK_PDRS;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_FARS;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_INTERFACES;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_DBUF_FAR;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_IFACE;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_NORMAL_FAR;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_PDR;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_PDR_QOS;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_LOAD_TUNNEL_FAR;
import static org.onosproject.pipelines.fabric.FabricConstants.FABRIC_INGRESS_SPGW_UPLINK_PDRS;
import static org.onosproject.pipelines.fabric.FabricConstants.FAR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_FAR_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_GTPU_IS_VALID;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_HAS_QFI;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_IPV4_DST_ADDR;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_QFI;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_TEID;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_TUNNEL_IPV4_DST;
import static org.onosproject.pipelines.fabric.FabricConstants.HDR_UE_ADDR;
import static org.onosproject.pipelines.fabric.FabricConstants.NEEDS_GTPU_DECAP;
import static org.onosproject.pipelines.fabric.FabricConstants.NEEDS_QFI_PUSH;
import static org.onosproject.pipelines.fabric.FabricConstants.NOTIFY_CP;
import static org.onosproject.pipelines.fabric.FabricConstants.QFI;
import static org.onosproject.pipelines.fabric.FabricConstants.SLICE_ID;
import static org.onosproject.pipelines.fabric.FabricConstants.SRC_IFACE;
import static org.onosproject.pipelines.fabric.FabricConstants.TC;
import static org.onosproject.pipelines.fabric.FabricConstants.TEID;
import static org.onosproject.pipelines.fabric.FabricConstants.TUNNEL_DST_ADDR;
import static org.onosproject.pipelines.fabric.FabricConstants.TUNNEL_SRC_ADDR;
import static org.onosproject.pipelines.fabric.FabricConstants.TUNNEL_SRC_PORT;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.DEFAULT_QFI;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.DEFAULT_SLICE_ID;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.DEFAULT_TC;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.FALSE;
import static org.onosproject.pipelines.fabric.impl.behaviour.Constants.TRUE;

/**
 * Provides logic to translate UPF entities into pipeline-specific ones and vice-versa.
 * Implementation should be stateless, with all state delegated to FabricUpfStore.
 */
public class FabricUpfTranslator {

    // UPF related constants
    public static final int INTERFACE_ACCESS = 1;
    public static final int INTERFACE_CORE = 2;
    public static final int INTERFACE_DBUF = 3;

    private final FabricUpfStore fabricUpfStore;

    public FabricUpfTranslator(FabricUpfStore fabricUpfStore) {
        this.fabricUpfStore = fabricUpfStore;
    }

    /**
     * Returns true if the given table entry is a Packet Detection Rule from the physical fabric pipeline, and
     * false otherwise.
     *
     * @param entry the entry that may or may not be a fabric.p4 PDR
     * @return true if the entry is a fabric.p4 PDR
     */
    public boolean isFabricPdr(FlowRule entry) {
        return entry.table().equals(FABRIC_INGRESS_SPGW_UPLINK_PDRS)
                || entry.table().equals(FABRIC_INGRESS_SPGW_DOWNLINK_PDRS);
    }

    /**
     * Returns true if the given table entry is a Forwarding Action Rule from the physical fabric pipeline, and
     * false otherwise.
     *
     * @param entry the entry that may or may not be a fabric.p4 FAR
     * @return true if the entry is a fabric.p4 FAR
     */
    public boolean isFabricFar(FlowRule entry) {
        return entry.table().equals(FABRIC_INGRESS_SPGW_FARS);
    }

    /**
     * Returns true if the given table entry is an interface table entry from the fabric.p4 physical pipeline, and
     * false otherwise.
     *
     * @param entry the entry that may or may not be a fabric.p4 UPF interface
     * @return true if the entry is a fabric.p4 UPF interface
     */
    public boolean isFabricInterface(FlowRule entry) {
        return entry.table().equals(FABRIC_INGRESS_SPGW_INTERFACES);
    }


    /**
     * Translate a fabric.p4 PDR table entry to a PacketDetectionRule instance for easier handling.
     *
     * @param entry the fabric.p4 entry to translate
     * @return the corresponding PacketDetectionRule
     * @throws UpfProgrammableException if the entry cannot be translated
     */
    public PacketDetectionRule fabricEntryToPdr(FlowRule entry)
            throws UpfProgrammableException {
        var pdrBuilder = PacketDetectionRule.builder();
        Pair<PiCriterion, PiTableAction> matchActionPair = FabricUpfTranslatorUtil.fabricEntryToPiPair(entry);
        PiCriterion match = matchActionPair.getLeft();
        PiAction action = (PiAction) matchActionPair.getRight();

        // Grab keys and parameters that are present for all PDRs
        int globalFarId = FabricUpfTranslatorUtil.getParamInt(action, FAR_ID);
        UpfRuleIdentifier farId = fabricUpfStore.localFarIdOf(globalFarId);
        if (farId == null) {
            throw new UpfProgrammableException(String.format("Unable to find local far id of %s", globalFarId));
        }

        pdrBuilder.withCounterId(FabricUpfTranslatorUtil.getParamInt(action, CTR_ID))
                .withLocalFarId(farId.getSessionLocalId())
                .withSessionId(farId.getPfcpSessionId());

        PiActionId actionId = action.id();
        if (actionId.equals(FABRIC_INGRESS_SPGW_LOAD_PDR_QOS)) {
            pdrBuilder.withQfi(FabricUpfTranslatorUtil.getParamByte(action, QFI));
            if (FabricUpfTranslatorUtil.getParamByte(action, NEEDS_QFI_PUSH) == TRUE) {
                pdrBuilder.withQfiPush();
            }
        }

        if (FabricUpfTranslatorUtil.fieldIsPresent(match, HDR_TEID)) {
            // F-TEID is only present for GTP-matching PDRs
            ImmutableByteSequence teid = FabricUpfTranslatorUtil.getFieldValue(match, HDR_TEID);
            Ip4Address tunnelDst = FabricUpfTranslatorUtil.getFieldAddress(match, HDR_TUNNEL_IPV4_DST);
            pdrBuilder.withTeid(teid)
                    .withTunnelDst(tunnelDst);
            if (FabricUpfTranslatorUtil.fieldIsPresent(match, HDR_HAS_QFI) &&
                    FabricUpfTranslatorUtil.getFieldByte(match, HDR_HAS_QFI) == TRUE) {
                pdrBuilder.withQfi(FabricUpfTranslatorUtil.getFieldByte(match, HDR_QFI));
                pdrBuilder.withQfiMatch();
            }
        } else if (FabricUpfTranslatorUtil.fieldIsPresent(match, HDR_UE_ADDR)) {
            // And UE address is only present for non-GTP-matching PDRs
            pdrBuilder.withUeAddr(FabricUpfTranslatorUtil.getFieldAddress(match, HDR_UE_ADDR));
        } else {
            throw new UpfProgrammableException("Read malformed PDR from dataplane!:" + entry);
        }
        return pdrBuilder.build();
    }

    /**
     * Translate a fabric.p4 FAR table entry to a ForwardActionRule instance for easier handling.
     *
     * @param entry the fabric.p4 entry to translate
     * @return the corresponding ForwardingActionRule
     * @throws UpfProgrammableException if the entry cannot be translated
     */
    public ForwardingActionRule fabricEntryToFar(FlowRule entry)
            throws UpfProgrammableException {
        var farBuilder = ForwardingActionRule.builder();
        Pair<PiCriterion, PiTableAction> matchActionPair = FabricUpfTranslatorUtil.fabricEntryToPiPair(entry);
        PiCriterion match = matchActionPair.getLeft();
        PiAction action = (PiAction) matchActionPair.getRight();

        int globalFarId = FabricUpfTranslatorUtil.getFieldInt(match, HDR_FAR_ID);
        UpfRuleIdentifier farId = fabricUpfStore.localFarIdOf(globalFarId);
        if (farId == null) {
            throw new UpfProgrammableException(String.format("Unable to find local far id of %s", globalFarId));
        }

        boolean dropFlag = FabricUpfTranslatorUtil.getParamInt(action, DROP) > 0;
        boolean notifyFlag = FabricUpfTranslatorUtil.getParamInt(action, NOTIFY_CP) > 0;

        // Match keys
        farBuilder.withSessionId(farId.getPfcpSessionId())
                .setFarId(farId.getSessionLocalId());

        // Parameters common to all types of FARs
        farBuilder.setDropFlag(dropFlag)
                .setNotifyFlag(notifyFlag);

        PiActionId actionId = action.id();

        if (actionId.equals(FABRIC_INGRESS_SPGW_LOAD_TUNNEL_FAR)
                || actionId.equals(FABRIC_INGRESS_SPGW_LOAD_DBUF_FAR)) {
            // Grab parameters specific to encapsulating FARs if they're present
            Ip4Address tunnelSrc = FabricUpfTranslatorUtil.getParamAddress(action, TUNNEL_SRC_ADDR);
            Ip4Address tunnelDst = FabricUpfTranslatorUtil.getParamAddress(action, TUNNEL_DST_ADDR);
            ImmutableByteSequence teid = FabricUpfTranslatorUtil.getParamValue(action, TEID);
            short tunnelSrcPort = (short) FabricUpfTranslatorUtil.getParamInt(action, TUNNEL_SRC_PORT);

            farBuilder.setBufferFlag(actionId.equals(FABRIC_INGRESS_SPGW_LOAD_DBUF_FAR));

            farBuilder.setTunnel(
                    GtpTunnel.builder()
                            .setSrc(tunnelSrc)
                            .setDst(tunnelDst)
                            .setTeid(teid)
                            .setSrcPort(tunnelSrcPort)
                            .build());
        }
        return farBuilder.build();
    }

    /**
     * Translate a fabric.p4 interface table entry to a UpfInterface instance for easier handling.
     *
     * @param entry the fabric.p4 entry to translate
     * @return the corresponding UpfInterface
     * @throws UpfProgrammableException if the entry cannot be translated
     */
    public UpfInterface fabricEntryToInterface(FlowRule entry)
            throws UpfProgrammableException {
        Pair<PiCriterion, PiTableAction> matchActionPair = FabricUpfTranslatorUtil.fabricEntryToPiPair(entry);
        PiCriterion match = matchActionPair.getLeft();
        PiAction action = (PiAction) matchActionPair.getRight();

        var ifaceBuilder = UpfInterface.builder()
                .setPrefix(FabricUpfTranslatorUtil.getFieldPrefix(match, HDR_IPV4_DST_ADDR));

        int interfaceType = FabricUpfTranslatorUtil.getParamInt(action, SRC_IFACE);
        if (interfaceType == INTERFACE_ACCESS) {
            ifaceBuilder.setAccess();
        } else if (interfaceType == INTERFACE_CORE) {
            ifaceBuilder.setCore();
        } else if (interfaceType == INTERFACE_DBUF) {
            ifaceBuilder.setDbufReceiver();
        }
        return ifaceBuilder.build();
    }

    /**
     * Translate a ForwardingActionRule to a FlowRule to be inserted into the fabric.p4 pipeline.
     * A side effect of calling this method is the FAR object's globalFarId is assigned if it was not already.
     *
     * @param far      The FAR to be translated
     * @param deviceId the ID of the device the FlowRule should be installed on
     * @param appId    the ID of the application that will insert the FlowRule
     * @param priority the FlowRule's priority
     * @return the FAR translated to a FlowRule
     * @throws UpfProgrammableException if the FAR to be translated is malformed
     */
    public FlowRule farToFabricEntry(ForwardingActionRule far, DeviceId deviceId, ApplicationId appId, int priority)
            throws UpfProgrammableException {
        PiAction action;
        if (!far.encaps()) {
            action = PiAction.builder()
                    .withId(FABRIC_INGRESS_SPGW_LOAD_NORMAL_FAR)
                    .withParameters(Arrays.asList(
                            new PiActionParam(DROP, far.drops() ? 1 : 0),
                            new PiActionParam(NOTIFY_CP, far.notifies() ? 1 : 0)
                    ))
                    .build();

        } else {
            if (far.tunnelSrc() == null || far.tunnelDst() == null
                    || far.teid() == null || far.tunnel().srcPort() == null) {
                throw new UpfProgrammableException(
                        "Not all action parameters present when translating " +
                                "intermediate encapsulating/buffering FAR to physical FAR!");
            }
            // TODO: copy tunnel destination port from logical switch write requests, instead of hardcoding 2152
            PiActionId actionId = far.buffers() ? FABRIC_INGRESS_SPGW_LOAD_DBUF_FAR :
                    FABRIC_INGRESS_SPGW_LOAD_TUNNEL_FAR;
            action = PiAction.builder()
                    .withId(actionId)
                    .withParameters(Arrays.asList(
                            new PiActionParam(DROP, far.drops() ? 1 : 0),
                            new PiActionParam(NOTIFY_CP, far.notifies() ? 1 : 0),
                            new PiActionParam(TEID, far.teid()),
                            new PiActionParam(TUNNEL_SRC_ADDR, far.tunnelSrc().toInt()),
                            new PiActionParam(TUNNEL_DST_ADDR, far.tunnelDst().toInt()),
                            new PiActionParam(TUNNEL_SRC_PORT, far.tunnel().srcPort())
                    ))
                    .build();
        }
        PiCriterion match = PiCriterion.builder()
                .matchExact(HDR_FAR_ID, fabricUpfStore.globalFarIdOf(far.sessionId(), far.farId()))
                .build();
        return DefaultFlowRule.builder()
                .forDevice(deviceId).fromApp(appId).makePermanent()
                .forTable(FABRIC_INGRESS_SPGW_FARS)
                .withSelector(DefaultTrafficSelector.builder().matchPi(match).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(action).build())
                .withPriority(priority)
                .build();
    }

    /**
     * Translate a PacketDetectionRule to a FlowRule to be inserted into the fabric.p4 pipeline.
     * A side effect of calling this method is the PDR object's globalFarId is assigned if it was not already.
     *
     * @param pdr      The PDR to be translated
     * @param deviceId the ID of the device the FlowRule should be installed on
     * @param appId    the ID of the application that will insert the FlowRule
     * @param priority the FlowRule's priority
     * @return the FAR translated to a FlowRule
     * @throws UpfProgrammableException if the PDR to be translated is malformed
     */
    public FlowRule pdrToFabricEntry(PacketDetectionRule pdr, DeviceId deviceId, ApplicationId appId, int priority)
            throws UpfProgrammableException {
        final PiCriterion match;
        final PiTableId tableId;
        final PiAction action;

        final PiCriterion.Builder matchBuilder = PiCriterion.builder();

        PiAction.Builder actionBuilder = PiAction.builder()
                .withParameters(Arrays.asList(
                        new PiActionParam(CTR_ID, pdr.counterId()),
                        new PiActionParam(FAR_ID, fabricUpfStore.globalFarIdOf(pdr.sessionId(), pdr.farId())),
                        new PiActionParam(NEEDS_GTPU_DECAP, pdr.matchesEncapped() ?
                                TRUE : FALSE),
                        new PiActionParam(TC, DEFAULT_TC)
                ));
        PiActionId actionId = FABRIC_INGRESS_SPGW_LOAD_PDR;
        if (pdr.matchesEncapped()) {
            tableId = FABRIC_INGRESS_SPGW_UPLINK_PDRS;
            matchBuilder.matchExact(HDR_TEID, pdr.teid().asArray())
                    .matchExact(HDR_TUNNEL_IPV4_DST, pdr.tunnelDest().toInt());
            if (pdr.matchQfi()) {
                matchBuilder.matchExact(HDR_HAS_QFI, TRUE)
                        .matchExact(HDR_QFI, pdr.qfi());
            } else {
                matchBuilder.matchExact(HDR_HAS_QFI, FALSE)
                        .matchExact(HDR_QFI, DEFAULT_QFI);
                if (pdr.hasQfi()) {
                    actionId = FABRIC_INGRESS_SPGW_LOAD_PDR_QOS;
                    actionBuilder.withParameter(new PiActionParam(QFI, pdr.qfi()))
                            .withParameter(new PiActionParam(NEEDS_QFI_PUSH, FALSE));
                }
            }
        } else if (pdr.matchesUnencapped()) {
            tableId = FABRIC_INGRESS_SPGW_DOWNLINK_PDRS;
            matchBuilder.matchExact(HDR_UE_ADDR, pdr.ueAddress().toInt());
            if (pdr.hasQfi()) {
                actionBuilder.withParameter(new PiActionParam(QFI, pdr.qfi()))
                        .withParameter(new PiActionParam(NEEDS_QFI_PUSH, pdr.pushQfi() ? TRUE : FALSE));
                actionId = FABRIC_INGRESS_SPGW_LOAD_PDR_QOS;
            }
        } else {
            throw new UpfProgrammableException("Flexible PDRs not yet supported! Cannot translate " + pdr);
        }
        match = matchBuilder.build();
        action = actionBuilder.withId(actionId)
                .build();
        return DefaultFlowRule.builder()
                .forDevice(deviceId).fromApp(appId).makePermanent()
                .forTable(tableId)
                .withSelector(DefaultTrafficSelector.builder().matchPi(match).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(action).build())
                .withPriority(priority)
                .build();
    }

    /**
     * Translate a UpfInterface to a FlowRule to be inserted into the fabric.p4 pipeline.
     *
     * @param upfInterface The interface to be translated
     * @param deviceId     the ID of the device the FlowRule should be installed on
     * @param appId        the ID of the application that will insert the FlowRule
     * @param priority     the FlowRule's priority
     * @return the UPF interface translated to a FlowRule
     * @throws UpfProgrammableException if the interface cannot be translated
     */
    public FlowRule interfaceToFabricEntry(UpfInterface upfInterface, DeviceId deviceId,
                                           ApplicationId appId, int priority)
            throws UpfProgrammableException {
        int interfaceTypeInt;
        int gtpuValidity;
        if (upfInterface.isDbufReceiver()) {
            interfaceTypeInt = INTERFACE_DBUF;
            gtpuValidity = 1;
        } else if (upfInterface.isAccess()) {
            interfaceTypeInt = INTERFACE_ACCESS;
            gtpuValidity = 1;
        } else {
            interfaceTypeInt = INTERFACE_CORE;
            gtpuValidity = 0;
        }

        PiCriterion match = PiCriterion.builder()
                .matchLpm(HDR_IPV4_DST_ADDR,
                          upfInterface.prefix().address().toInt(),
                          upfInterface.prefix().prefixLength())
                .matchExact(HDR_GTPU_IS_VALID, gtpuValidity)
                .build();
        PiAction action = PiAction.builder()
                .withId(FABRIC_INGRESS_SPGW_LOAD_IFACE)
                .withParameter(new PiActionParam(SRC_IFACE, interfaceTypeInt))
                .withParameter(new PiActionParam(SLICE_ID, DEFAULT_SLICE_ID))
                .build();
        return DefaultFlowRule.builder()
                .forDevice(deviceId).fromApp(appId).makePermanent()
                .forTable(FABRIC_INGRESS_SPGW_INTERFACES)
                .withSelector(DefaultTrafficSelector.builder().matchPi(match).build())
                .withTreatment(DefaultTrafficTreatment.builder().piTableAction(action).build())
                .withPriority(priority)
                .build();
    }
}
