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
package org.onosproject.drivers.microsemi;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.onlab.packet.EthType;
import org.onlab.packet.EthType.EtherType;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.VlanId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.microsemi.yang.MseaSaFilteringNetconfService;
import org.onosproject.drivers.microsemi.yang.MseaUniEvcServiceNetconfService;
import org.onosproject.drivers.microsemi.yang.UniSide;
import org.onosproject.drivers.microsemi.yang.custom.CustomEvcPerUnic;
import org.onosproject.drivers.microsemi.yang.custom.CustomEvcPerUnin;
import org.onosproject.drivers.microsemi.yang.utils.CeVlanMapUtils;
import org.onosproject.net.PortNumber;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.Criterion.Type;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanHeaderInstruction;
import org.onosproject.net.flow.instructions.L2ModificationInstruction.ModVlanIdInstruction;
import org.onosproject.net.meter.MeterId;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFilteringOpParam;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.DefaultSourceIpaddressFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.SourceIpaddressFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.DefaultInterfaceEth0;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.InterfaceEth0;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.DefaultSourceAddressRange;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.FilterAdminStateEnum;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.SourceAddressRange;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.Identifier45;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.ServiceListType;
import org.onosproject.yang.gen.v1.mseatypes.rev20160229.mseatypes.VlanIdType;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcService;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.MseaUniEvcServiceOpParam;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.DefaultMefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.MefServices;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.DefaultFlowMapping;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.FlowMapping;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.TagManipulation;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.DefaultTagOverwrite;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.DefaultTagPop;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.DefaultTagPush;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.TagOverwrite;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.TagPop;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.TagPush;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.evcperuniextensionattributes.tagmanipulation.tagpush.tagpush.PushTagTypeEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.DefaultProfiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.DefaultUni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.Profiles;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.Uni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.BwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.profiles.DefaultBwpGroup;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.DefaultEvc;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.Evc;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.UniSideInterfaceAssignmentEnum;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.DefaultEvcPerUni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.EvcPerUni;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnic;
import org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice.mefservices.uni.evc.evcperuni.EvcPerUnin;
import org.slf4j.Logger;

/**
 * An implementation of the FlowRuleProgrammable behaviour for the EA10000 device.
 *
 * This device is not a native Open Flow device. It has only a NETCONF interface for configuration
 * status retrieval and notifications. It supports only a small subset of OpenFlow rules.<br>
 *
 * The device supports only:<br>
 * 1) Open flow rules that blocks certain IP address ranges, but only those incoming on Port 0
 *    and has a limit of 10 such rules<br>
 * 2) Open flow rules that PUSH, POP and OVERWRITE VLAN tags on both ports. This can push and overwrite
 *    both C-TAGs (0x8100) and S-TAGs (0x88a8).
 */
public class EA1000FlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    public static final int RADIX_16 = 16;
    protected final Logger log = getLogger(getClass());
    public static final String MICROSEMI_DRIVERS = "com.microsemi.drivers";
    public static final int PRIORITY_DEFAULT = 50000;
    //To protect the NETCONF session from concurrent access across flow addition and removal
    static Semaphore sessionMutex = new Semaphore(1);

    /**
     * Get the flow entries that are present on the EA1000.
     * Since the EA1000 does not have any 'real' flow entries these are retrieved from 2 configuration
     * areas on the EA1000 NETCONF model - from SA filtering YANG model and from EVC UNI YANG model.<br>
     * The flow entries must match exactly the FlowRule entries in the ONOS store. If they are not an
     * exact match the device will be requested to remove those flows and the FlowRule will stay in a
     * PENDING_ADD state.
     * @return A collection of Flow Entries
     */
    @Override
    public Collection<FlowEntry> getFlowEntries() {
        Collection<FlowEntry> flowEntryCollection = new HashSet<>();

        UniSideInterfaceAssignmentEnum portAssignment = UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST;
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfDevice ncDevice = controller.getDevicesMap().get(handler().data().deviceId());
        if (ncDevice == null) {
            log.error("Internal ONOS Error. Device has been marked as reachable, " +
                            "but deviceID {} is not in Devices Map. Continuing with empty description",
                    handler().data().deviceId());
            return flowEntryCollection;
        }
        NetconfSession session = ncDevice.getSession();
        CoreService coreService = checkNotNull(handler().get(CoreService.class));
        ApplicationId appId = coreService.getAppId(MICROSEMI_DRIVERS);
        MseaSaFilteringNetconfService mseaSaFilteringService =
                checkNotNull(handler().get(MseaSaFilteringNetconfService.class));
        MseaUniEvcServiceNetconfService mseaUniEvcServiceSvc =
                checkNotNull(handler().get(MseaUniEvcServiceNetconfService.class));
        log.debug("getFlowEntries() called on EA1000FlowRuleProgrammable");

        //First get the MseaSaFiltering rules
        SourceIpaddressFiltering sip =
                new DefaultSourceIpaddressFiltering();

        MseaSaFilteringOpParam op =
                new MseaSaFilteringOpParam();
        op.sourceIpaddressFiltering(sip);

        try {
            MseaSaFiltering saFilteringCurrent =
                    mseaSaFilteringService.getMseaSaFiltering(op, session);
            if (saFilteringCurrent != null &&
                    saFilteringCurrent.sourceIpaddressFiltering() != null) {
                flowEntryCollection.addAll(
                        convertSaFilteringToFlowRules(saFilteringCurrent, appId));
            }
        } catch (NetconfException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("Timeout exception getting SA Filt Flow Entries from {}",
                        handler().data().deviceId());
                return flowEntryCollection;
            } else {
                log.error("Unexpected error on SA Filt getFlowEntries on {}",
                        handler().data().deviceId(), e);
                return flowEntryCollection;
            }
        }


        //Then get the EVCs - there will be a flow entry per EVC
        MefServices mefServices = new DefaultMefServices();
        mefServices.uni(new DefaultUni());

        MseaUniEvcServiceOpParam mseaUniEvcServiceFilter = new MseaUniEvcServiceOpParam();
        mseaUniEvcServiceFilter.mefServices(mefServices);
        try {
            MseaUniEvcService uniEvcCurrent =
                    mseaUniEvcServiceSvc.getConfigMseaUniEvcService(mseaUniEvcServiceFilter,
                            session, DatastoreId.RUNNING);

            flowEntryCollection.addAll(
                    convertEvcUniToFlowRules(uniEvcCurrent, portAssignment));

        } catch (NetconfException e) {
            if (e.getCause() instanceof TimeoutException) {
                log.warn("Timeout exception getting EVC Flow Entries from {}",
                        handler().data().deviceId());
                return flowEntryCollection;
            } else {
                log.error("Unexpected error on EVC getFlowEntries on {}",
                        handler().data().deviceId(), e);
            }
        }

        return flowEntryCollection;
    }

    /**
     * Apply the flow entries to the EA1000.
     * Since the EA1000 does not have any 'real' flow entries these are converted 2 configuration
     * areas on the EA1000 NETCONF model - to SA filtering YANG model and to EVC UNI YANG model.<br>
     * Only a subset of the possible OpenFlow rules are supported. Any rule that's not handled
     * will not be in the returned set.
     *
     * @param rules A collection of Flow Rules to be applied to the EA1000
     * @return A collection of the Flow Rules that have been added.
     */
    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        Collection<FlowRule> frAdded = new HashSet<>();
        if (rules == null || rules.isEmpty()) {
            return rules;
        }
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaSaFilteringNetconfService mseaSaFilteringService =
                checkNotNull(handler().get(MseaSaFilteringNetconfService.class));
        MseaUniEvcServiceNetconfService mseaUniEvcServiceSvc =
                checkNotNull(handler().get(MseaUniEvcServiceNetconfService.class));
        log.debug("applyFlowRules() called on EA1000FlowRuleProgrammable with {} rules.", rules.size());
        // TODO: Change this so it's dynamically driven
        UniSideInterfaceAssignmentEnum portAssignment = UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST;

        List<SourceAddressRange> saRangeList = new ArrayList<>();
        Map<Integer, Evc> evcMap = new HashMap<>();

        //Retrieve the list of actual EVCs and the CeVlanMaps from device
        List<Evc> activeEvcs = new ArrayList<>();
        try {
            sessionMutex.acquire();
            MseaUniEvcService evcResponse =
                    mseaUniEvcServiceSvc.getmseaUniEvcCeVlanMaps(session, DatastoreId.RUNNING);
            //There could be zero or more EVCs
            if (evcResponse != null && evcResponse.mefServices() != null && evcResponse.mefServices().uni() != null) {
                activeEvcs.addAll(evcResponse.mefServices().uni().evc());
            }
        } catch (NetconfException | InterruptedException e1) {
            log.warn("Unexpected error on applyFlowRules", e1);
        }

        for (FlowRule fr : rules) {

            // IP SA Filtering can only apply to Port 0 optics
            if (fr.selector().getCriterion(Type.IPV4_SRC) != null &&
                    fr.selector().getCriterion(Type.IN_PORT) != null &&
                    ((PortCriterion) fr.selector().getCriterion(Type.IN_PORT)).port().toLong() == 0) {
                parseFrForSaRange(frAdded, saRangeList, fr);

            // EVCs will be defined by Flow Rules relating to VIDs
            } else if (fr.selector().getCriterion(Type.VLAN_VID) != null &&
                    fr.selector().getCriterion(Type.IN_PORT) != null) {
                //There could be many Flow Rules for one EVC depending on the ceVlanMap
                //Cannot build up the EVC until we know the details - the key is the tableID and port
                parseFrForEvcs(frAdded, evcMap, activeEvcs, portAssignment, fr);
            } else {
                log.info("Unexpected Flow Rule type applied: " + fr);
            }
        }

        //If there are IPv4 Flow Rules created commit them now through the
        //MseaSaFiltering service
        if (!saRangeList.isEmpty()) {
            try {
                mseaSaFilteringService.setMseaSaFiltering(
                            buildSaFilteringObject(saRangeList), session, DatastoreId.RUNNING);
            } catch (NetconfException e) {
                log.error("Error applying Flow Rules to SA Filtering - " +
                        "will try again: " + e.getMessage(), e);
                sessionMutex.release();
                return frAdded;
            }
        }
        //If there are EVC flow rules then populate the MseaUniEvc part of EA1000
        if (evcMap.size() > 0) {
            List<Evc> evcList = evcMap.entrySet().stream()
                    .map(x -> x.getValue())
                    .collect(Collectors.toList());
            Uni uni = new DefaultUni();
            URI deviceName = handler().data().deviceId().uri();
            uni.name(new Identifier45("Uni-on-"
                    + deviceName.getSchemeSpecificPart()));
            uni.evc(evcList);

            List<BwpGroup> bwpGroupList = new ArrayList<>();
            BwpGroup bwpGrp = new DefaultBwpGroup();
            bwpGrp.groupIndex((short) 0);
            bwpGroupList.add(bwpGrp);
            Profiles profiles = new DefaultProfiles();
            profiles.bwpGroup(bwpGroupList);

            MefServices mefServices = new DefaultMefServices();
            mefServices.uni(uni);
            mefServices.profiles(profiles);

            MseaUniEvcServiceOpParam mseaUniEvcServiceFilter = new MseaUniEvcServiceOpParam();
            mseaUniEvcServiceFilter.mefServices(mefServices);
            try {
                mseaUniEvcServiceSvc.setMseaUniEvcService(mseaUniEvcServiceFilter, session, DatastoreId.RUNNING);
            } catch (NetconfException e) {
                log.error("Error applying Flow Rules to EVC - will try again: " + e.getMessage(), e);
                sessionMutex.release();
                return frAdded;
            }
        }
        sessionMutex.release();
        return frAdded;
    }

    /**
     * Remove flow rules from the EA1000.
     * Since the EA1000 does not have any 'real' flow entries these are converted 2 configuration
     * areas on the EA1000 NETCONF model - to SA filtering YANG model and to EVC UNI YANG model.
     *
     * @param rulesToRemove A collection of Flow Rules to be removed to the EA1000
     * @return A collection of the Flow Rules that have been removed.
     */
    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rulesToRemove) {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        MseaSaFilteringNetconfService mseaSaFilteringService =
                checkNotNull(handler().get(MseaSaFilteringNetconfService.class));
        MseaUniEvcServiceNetconfService mseaUniEvcServiceSvc =
                checkNotNull(handler().get(MseaUniEvcServiceNetconfService.class));
        UniSideInterfaceAssignmentEnum portAssignment = UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST;
        log.debug("removeFlowRules() called on EA1000FlowRuleProgrammable with {} rules.", rulesToRemove.size());

        if (rulesToRemove.isEmpty()) {
            return rulesToRemove;
        }

        //Retrieve the list of actual EVCs and the CeVlanMaps from device
        List<Evc> activeEvcs = new ArrayList<>();
        List<Short> acvtiveFiltRanges = new ArrayList<>();
        try {
            sessionMutex.acquire();
            MseaUniEvcService evcResponse =
                    mseaUniEvcServiceSvc.getmseaUniEvcCeVlanMaps(session, DatastoreId.RUNNING);
            //There could be zero or more EVCs
            if (evcResponse != null && evcResponse.mefServices() != null && evcResponse.mefServices().uni() != null) {
                activeEvcs.addAll(evcResponse.mefServices().uni().evc());
            }
            mseaSaFilteringService.getConfigMseaSaFilterIds(session).forEach(
                    r -> acvtiveFiltRanges.add(r.rangeId()));

        } catch (NetconfException | InterruptedException e1) {
            log.warn("Error on removeFlowRules.", e1);
        }

        List<SourceAddressRange> saRangeList = new ArrayList<>();
        Map<Integer, String> ceVlanMapMap = new HashMap<>();
        Map<Integer, List<Short>> flowIdMap = new HashMap<>();

        Collection<FlowRule> rulesRemoved = new HashSet<>();
        for (FlowRule ruleToRemove : rulesToRemove) {
            // IP SA Filtering can only apply to Port 0 optics
            if (ruleToRemove.selector().getCriterion(Type.IPV4_SRC) != null &&
                    ruleToRemove.selector().getCriterion(Type.IN_PORT) != null &&
                    ((PortCriterion) ruleToRemove.selector().getCriterion(Type.IN_PORT)).port().toLong() == 0) {
                SourceAddressRange sar = new DefaultSourceAddressRange();
                sar.rangeId((short) ruleToRemove.tableId());
                acvtiveFiltRanges.remove(Short.valueOf((short) ruleToRemove.tableId()));
                rulesRemoved.add(ruleToRemove);
                saRangeList.add(sar);

            } else if (ruleToRemove.selector().getCriterion(Type.VLAN_VID) != null &&
                    ruleToRemove.selector().getCriterion(Type.IN_PORT) != null) {
                PortNumber portNumber = ((PortCriterion) ruleToRemove.selector().getCriterion(Type.IN_PORT)).port();
                VlanId vlanId = ((VlanIdCriterion) ruleToRemove.selector().getCriterion(Type.VLAN_VID)).vlanId();
                int evcId = ruleToRemove.tableId();
                int evcKey = (evcId << 2) + (int) portNumber.toLong();
                String activeCeVlanMap = "";
                //If this is one of many VLANs belonging to an EVC then we should only remove this VLAN
                // from the ceVlanMap and not the whole EVC
                if (!ceVlanMapMap.containsKey(evcKey)) {
                    for (Evc activeEvc:activeEvcs) {
                        if (activeEvc.evcIndex() == evcId) {
                            if (Ea1000Port.fromNum(portNumber.toLong()).nOrC(portAssignment) ==
                                    UniSide.CUSTOMER) {
                                activeCeVlanMap = activeEvc.evcPerUni().evcPerUnic().ceVlanMap().string();
                            } else if (Ea1000Port.fromNum(portNumber.toLong()).nOrC(portAssignment) ==
                                    UniSide.NETWORK) {
                                activeCeVlanMap = activeEvc.evcPerUni().evcPerUnin().ceVlanMap().string();
                            }
                        }
                    }
                }

                ceVlanMapMap.put(evcKey, CeVlanMapUtils.removeFromCeVlanMap(activeCeVlanMap, vlanId.id()));
                if (!flowIdMap.containsKey(evcKey)) {
                    flowIdMap.put(evcKey, new ArrayList<>());
                }
                flowIdMap.get(evcKey).add(vlanId.id());
                rulesRemoved.add(ruleToRemove);

            } else {
                log.warn("Unexpected Flow Rule type removal: " + ruleToRemove);
            }
        }

        //If there are IPv4 Flow Rules created commit them now through the
        //MseaSaFiltering service
        if (!saRangeList.isEmpty() && acvtiveFiltRanges.isEmpty()) {
            try {
                SourceIpaddressFiltering saFilter =
                        new DefaultSourceIpaddressFiltering();
                MseaSaFilteringOpParam mseaSaFiltering = new MseaSaFilteringOpParam();
                mseaSaFiltering.sourceIpaddressFiltering(saFilter);

                mseaSaFilteringService.deleteMseaSaFilteringRange(
                        mseaSaFiltering, session, DatastoreId.RUNNING);
            } catch (NetconfException e) {
                log.debug("Remove FlowRule on MseaSaFilteringService could not delete all SARules - "
                        + "they may already have been deleted: " + e.getMessage(), e);
            }
        } else if (!saRangeList.isEmpty()) {
            try {
                mseaSaFilteringService.deleteMseaSaFilteringRange(
                        buildSaFilteringObject(saRangeList), session, DatastoreId.RUNNING);
            } catch (NetconfException e) {
                log.warn("Remove FlowRule on MseaSaFilteringService could not delete SARule - "
                        + "it may already have been deleted: " + e.getMessage(), e);
            }
        }

        if (!ceVlanMapMap.isEmpty()) {
            try {
                mseaUniEvcServiceSvc.removeEvcUniFlowEntries(ceVlanMapMap, flowIdMap,
                        session, DatastoreId.RUNNING, portAssignment);
            } catch (NetconfException e) {
                log.debug("Remove FlowRule on MseaUniEvcService could not delete EVC - "
                        + "it may already have been deleted: " + e.getMessage(), e);
            }
        }

        sessionMutex.release();
        return rulesRemoved;
    }

    /**
     * An internal method for extracting one EVC from a list and returning its ceVlanMap.
     *
     * @param evcList - the list of known EVCs
     * @param evcIndex - the index of the EVC we're looking for
     * @param side - the side of the UNI
     * @return - the CEVlanMap we're looking for
     */
    private String getCeVlanMapForIdxFromEvcList(List<Evc> evcList, long evcIndex, UniSide side) {
        if (evcList != null && !evcList.isEmpty()) {
            for (Evc evc:evcList) {
                if (evc.evcIndex() == evcIndex && evc.evcPerUni() != null) {
                    if (side == UniSide.CUSTOMER &&
                        evc.evcPerUni().evcPerUnic() != null &&
                        evc.evcPerUni().evcPerUnic().ceVlanMap() != null) {
                        return evc.evcPerUni().evcPerUnic().ceVlanMap().string();
                    } else if (side == UniSide.NETWORK &&
                        evc.evcPerUni().evcPerUnin() != null &&
                        evc.evcPerUni().evcPerUnin().ceVlanMap() != null) {
                        return evc.evcPerUni().evcPerUnin().ceVlanMap().string();
                    }
                }
            }
        }

        return ""; //The EVC required was not in the list
    }

    /**
     * An internal method to convert from a FlowRule to SARange.
     *
     * @param frList A collection of flow rules
     * @param saRangeList A list of SARanges
     * @param fr A flow rule
     */
    private void parseFrForSaRange(Collection<FlowRule> frList, List<SourceAddressRange> saRangeList, FlowRule fr) {
        String ipAddrStr = fr.selector().getCriterion(Type.IPV4_SRC).toString().substring(9);
        log.debug("Applying IP address to " + ipAddrStr
                + " (on Port 0) to IP SA Filtering on EA1000 through NETCONF");

        SourceAddressRange sar =
                new DefaultSourceAddressRange();

        sar.rangeId((short) fr.tableId());
        sar.name("Flow:" + fr.id().toString());
        sar.ipv4AddressPrefix(ipAddrStr);

        frList.add(fr);
        saRangeList.add(sar);
    }

    private void parseFrForEvcs(Collection<FlowRule> frList, Map<Integer, Evc> evcMap,
            List<Evc> activeEvcs, UniSideInterfaceAssignmentEnum portAssignment, FlowRule fr) {
        //There could be many Flow Rules for one EVC depending on the ceVlanMap
        //Cannot build up the EVC until we know the details - the key is the tableID and port
        Ea1000Port port = Ea1000Port.fromNum(
                ((PortCriterion) fr.selector().getCriterion(Type.IN_PORT)).port().toLong());
        Integer evcKey = (fr.tableId() << 2) + port.portNum();
        VlanId sourceVid = ((VlanIdCriterion) fr.selector().getCriterion(Type.VLAN_VID)).vlanId();
        FlowMapping fm = new DefaultFlowMapping();
        fm.ceVlanId(VlanIdType.of(sourceVid.id()));
        fm.flowId(BigInteger.valueOf(fr.id().value()));

        if (evcMap.containsKey(evcKey)) { //Is there an entry already for this EVC and port?
            //Replace ceVlanMap
            if (port.nOrC(portAssignment) == UniSide.CUSTOMER) {
                evcMap.get(evcKey).evcPerUni().evcPerUnic().addToFlowMapping(fm);
                ServiceListType newCeVlanMap = new ServiceListType(
                        CeVlanMapUtils.addtoCeVlanMap(
                                evcMap.get(evcKey).evcPerUni().evcPerUnic().ceVlanMap().toString(),
                                sourceVid.toShort()));
                evcMap.get(evcKey).evcPerUni().evcPerUnic().ceVlanMap(newCeVlanMap);
            } else {
                evcMap.get(evcKey).evcPerUni().evcPerUnin().addToFlowMapping(fm);
                ServiceListType newCeVlanMap = new ServiceListType(
                        CeVlanMapUtils.addtoCeVlanMap(
                                evcMap.get(evcKey).evcPerUni().evcPerUnin().ceVlanMap().toString(),
                                sourceVid.toShort()));
                evcMap.get(evcKey).evcPerUni().evcPerUnin().ceVlanMap(newCeVlanMap);
            }
        } else if (evcMap.containsKey((evcKey ^ 1))) { //Is there an entry for this EVC but the opposite port?
            TagManipulation tm = getTagManipulation(fr);
            if (port.nOrC(portAssignment) == UniSide.NETWORK) {
                ServiceListType newCeVlanMap = new ServiceListType(
                        CeVlanMapUtils.addtoCeVlanMap(
                                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnin().ceVlanMap().toString(),
                                sourceVid.toShort()));
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnin().ceVlanMap(newCeVlanMap);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnin().tagManipulation(tm);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnin().addToFlowMapping(fm);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnin().ingressBwpGroupIndex(getMeterId(fr.treatment()));
            } else {
                ServiceListType newCeVlanMap = new ServiceListType(
                        CeVlanMapUtils.addtoCeVlanMap(
                                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnic().ceVlanMap().toString(),
                                sourceVid.toShort()));
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnic().ceVlanMap(newCeVlanMap);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnic().tagManipulation(tm);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnic().addToFlowMapping(fm);
                evcMap.get(evcKey ^ 1).evcPerUni().evcPerUnic().ingressBwpGroupIndex(getMeterId(fr.treatment()));
            }
        } else {
            Evc evc = new DefaultEvc();
            EvcPerUnin epun = new CustomEvcPerUnin();
            EvcPerUnic epuc = new CustomEvcPerUnic();
            TagManipulation tm = getTagManipulation(fr);

            UniSide side = port.nOrC(portAssignment);
            String oldCeVlanMap = getCeVlanMapForIdxFromEvcList(activeEvcs, fr.tableId(), side);
            String newCeVlanMap =
                    CeVlanMapUtils.addtoCeVlanMap(oldCeVlanMap, sourceVid.id());
            String oppositeCeVlanMap =
                    getCeVlanMapForIdxFromEvcList(activeEvcs, fr.tableId(),
                            port.opposite().nOrC(portAssignment));
            oppositeCeVlanMap = oppositeCeVlanMap.isEmpty() ? "0" : oppositeCeVlanMap;
            if (side == UniSide.NETWORK) {
                epun.ceVlanMap(new ServiceListType(newCeVlanMap));
                epun.tagManipulation(tm);
                epun.addToFlowMapping(fm);
                epun.ingressBwpGroupIndex(getMeterId(fr.treatment()));

                epuc.ceVlanMap(new ServiceListType(oppositeCeVlanMap));
                epuc.ingressBwpGroupIndex(0);
            } else {
                epuc.ceVlanMap(new ServiceListType(newCeVlanMap));
                epuc.tagManipulation(tm);
                epuc.addToFlowMapping(fm);
                epuc.ingressBwpGroupIndex(getMeterId(fr.treatment()));

                epun.ceVlanMap(new ServiceListType(oppositeCeVlanMap));
                epun.ingressBwpGroupIndex(0);
            }

            evc.evcIndex(fr.tableId());
            evc.name(new Identifier45("EVC-" + fr.tableId()));

            DefaultEvcPerUni epu = new DefaultEvcPerUni();
            epu.evcPerUnin(epun);
            epu.evcPerUnic(epuc);
            evc.evcPerUni(epu);

            evcMap.put(evcKey, evc);
        }

        frList.add(fr);
    }


    private MseaSaFilteringOpParam buildSaFilteringObject(List<SourceAddressRange> saRangeList) {
        InterfaceEth0 saIf = new DefaultInterfaceEth0();
        for (SourceAddressRange sa:saRangeList) {
            saIf.addToSourceAddressRange(sa);
        }
        saIf.filterAdminState(FilterAdminStateEnum.BLACKLIST);

        SourceIpaddressFiltering saFilter =
                new DefaultSourceIpaddressFiltering();
        saFilter.interfaceEth0(saIf);

        MseaSaFilteringOpParam mseaSaFiltering = new MseaSaFilteringOpParam();
        mseaSaFiltering.sourceIpaddressFiltering(saFilter);

        return mseaSaFiltering;
    }

    private Collection<FlowEntry> convertSaFilteringToFlowRules(
            MseaSaFiltering saFilteringCurrent, ApplicationId appId) {
        Collection<FlowEntry> flowEntryCollection = new HashSet<>();

        List<SourceAddressRange> saRangelist =
                saFilteringCurrent.sourceIpaddressFiltering().interfaceEth0().sourceAddressRange();
        Criterion matchInPort = Criteria.matchInPort(PortNumber.portNumber(0));
        TrafficSelector.Builder tsBuilder = DefaultTrafficSelector.builder();

        if (saRangelist != null) {
            for (SourceAddressRange sa : saRangelist) {
                Criterion matchIpSrc = Criteria.matchIPSrc(IpPrefix.valueOf(sa.ipv4AddressPrefix()));

                TrafficSelector selector = tsBuilder.add(matchIpSrc).add(matchInPort).build();

                TrafficTreatment.Builder trBuilder = DefaultTrafficTreatment.builder();
                TrafficTreatment treatment = trBuilder.drop().build();

                FlowRule.Builder feBuilder = new DefaultFlowRule.Builder();
                if (sa.name() != null && sa.name().startsWith("Flow:")) {
                    String[] nameParts = sa.name().split(":");
                    Long cookie = Long.valueOf(nameParts[1], RADIX_16);
                    feBuilder = feBuilder.withCookie(cookie);
                } else {
                    feBuilder = feBuilder.fromApp(appId);
                }

                FlowRule fr = feBuilder
                        .forDevice(handler().data().deviceId())
                        .withSelector(selector)
                        .withTreatment(treatment)
                        .forTable(sa.rangeId())
                        .makePermanent()
                        .withPriority(PRIORITY_DEFAULT)
                        .build();

                flowEntryCollection.add(
                        new DefaultFlowEntry(fr, FlowEntryState.ADDED, 0, 0, 0));
            }
        }
        return flowEntryCollection;
    }


    private Collection<FlowEntry> convertEvcUniToFlowRules(
            MseaUniEvcService uniEvcCurrent, UniSideInterfaceAssignmentEnum portAssignment) {
        Collection<FlowEntry> flowEntryCollection = new HashSet<>();

        if (uniEvcCurrent == null || uniEvcCurrent.mefServices() == null ||
                uniEvcCurrent.mefServices().uni() == null || uniEvcCurrent.mefServices().uni().evc() == null) {
            log.info("No EVC's found when getting flow rules");
            return flowEntryCollection;
        }

        for (Evc evc:uniEvcCurrent.mefServices().uni().evc()) {
            FlowRule.Builder frBuilder = new DefaultFlowRule.Builder();
            TrafficSelector.Builder tsBuilder = DefaultTrafficSelector.builder();

            TrafficTreatment uniNTreatment = treatmentForUniSde(evc.evcPerUni(), true);
            //Depending on the ceVlanMap there may be multiple VLans and hence multiple flow entries
            Short[] vlanIdsUniN =
                    CeVlanMapUtils.getVlanSet(ceVlanMapForUniSide(evc.evcPerUni(), true));
            for (Short vlanId:vlanIdsUniN) {
                if (vlanId == 0) {
                    continue;
                }
                Criterion uniNportCriterion = criterionPortForUniSide(portAssignment, true);
                TrafficSelector tsUniN = tsBuilder.matchVlanId(VlanId.vlanId(vlanId)).add(uniNportCriterion).build();
                long flowId = getFlowIdForVlan(evc.evcPerUni().evcPerUnin().flowMapping(), vlanId);

                FlowRule frUniN = frBuilder
                    .forDevice(handler().data().deviceId())
                    .withSelector(tsUniN)
                    .withTreatment(uniNTreatment)
                    .forTable(Math.toIntExact(evc.evcIndex())) //narrowing to int
                    .makePermanent()
                    .withPriority(PRIORITY_DEFAULT)
                    .withCookie(flowId)
                    .build();
                flowEntryCollection.add(new DefaultFlowEntry(frUniN, FlowEntryState.ADDED, 0, 0, 0));
            }

            TrafficTreatment uniCTreatment = treatmentForUniSde(evc.evcPerUni(), false);
            //Depending on the ceVlanMap there may be multiple VLans and hence multiple flow entries
            Short[] vlanIdsUniC =
                    CeVlanMapUtils.getVlanSet(ceVlanMapForUniSide(evc.evcPerUni(), false));
            if (vlanIdsUniC != null && vlanIdsUniC.length > 0) {
                for (Short vlanId:vlanIdsUniC) {
                    if (vlanId == 0) {
                        continue;
                    }
                    Criterion uniCportCriterion = criterionPortForUniSide(portAssignment, false);
                    TrafficSelector tsUniC =
                            tsBuilder.matchVlanId(VlanId.vlanId(vlanId)).add(uniCportCriterion).build();
                    long flowId = getFlowIdForVlan(evc.evcPerUni().evcPerUnic().flowMapping(), vlanId);

                    FlowRule frUniC = frBuilder
                            .forDevice(handler().data().deviceId())
                            .withSelector(tsUniC)
                            .withTreatment(uniCTreatment)
                            .forTable(Math.toIntExact(evc.evcIndex())) //narrowing to int
                            .makePermanent()
                            .withPriority(PRIORITY_DEFAULT)
                            .withCookie(flowId)
                            .build();
                    flowEntryCollection.add(new DefaultFlowEntry(frUniC, FlowEntryState.ADDED, 0, 0, 0));
                }
            }
        }

        return flowEntryCollection;
    }

    private long getFlowIdForVlan(List<FlowMapping> fmList, Short vlanId) {
        if (fmList == null || vlanId == null) {
            log.warn("Flow Mapping list is null when reading EVCs");
            return -1L;
        }
        for (FlowMapping fm:fmList) {
            if (fm.ceVlanId().uint16() == vlanId.intValue()) {
                return fm.flowId().longValue();
            }
        }
        return 0L;
    }

    private String ceVlanMapForUniSide(
            EvcPerUni evcPerUni, boolean portN) {
        if (portN) {
            return evcPerUni.evcPerUnin().ceVlanMap().string();
        } else {
            return evcPerUni.evcPerUnic().ceVlanMap().string();
        }
    }

    private Criterion criterionPortForUniSide(
            UniSideInterfaceAssignmentEnum portAssignment, boolean portN) {
        boolean cOnOptics = (portAssignment == UniSideInterfaceAssignmentEnum.UNI_C_ON_OPTICS);
        //If both are true or both are false then return 1
        int portNum = (cOnOptics == portN) ? 1 : 0;
        return Criteria.matchInPort(PortNumber.portNumber(portNum));
    }

    private TrafficTreatment treatmentForUniSde(
            EvcPerUni evcPerUni, boolean portN) {
        TrafficTreatment.Builder trBuilder = DefaultTrafficTreatment.builder();

        TagManipulation tm;
        short meterId;
        if (portN) {
            tm = evcPerUni.evcPerUnin().tagManipulation();
            meterId = (short) evcPerUni.evcPerUnin().ingressBwpGroupIndex();
        } else {
            tm = evcPerUni.evcPerUnic().tagManipulation();
            meterId = (short) evcPerUni.evcPerUnic().ingressBwpGroupIndex();
        }

        if (meterId > 0L) {
            trBuilder = trBuilder.meter(MeterId.meterId((long) meterId));
        }

        if (tm == null) {
            return trBuilder.build(); //no tag manipulation found
        }

        if (tm.getClass().equals(DefaultTagPush.class)) {
            VlanId pushVlanNum = VlanId.vlanId((short) ((TagPush) tm).tagPush().outerTagVlan().uint16());
            PushTagTypeEnum pushTagType = ((TagPush) tm).tagPush().pushTagType();
            //Note - the order of elements below MUST match the order of the Treatment in the stored FlowRule
            // to be an exactMatch. See DefaultFlowRule.exactMatch()
            trBuilder = trBuilder
                    .pushVlan(pushTagType.equals(PushTagTypeEnum.PUSHCTAG) ?
                            EtherType.VLAN.ethType() : EtherType.QINQ.ethType())
                    .setVlanId(pushVlanNum).transition(Integer.valueOf(0));

        } else if (tm.getClass().equals(DefaultTagPop.class)) {
            trBuilder = trBuilder.popVlan();

        } else if (tm.getClass().equals(DefaultTagOverwrite.class)) {
            TagOverwrite to = (TagOverwrite) tm;
            VlanId ovrVlanNum = VlanId
                    .vlanId((short) (
                            //There are 2 classes TagOverwrite - the other one is already imported
                            to
                            .tagOverwrite()
                            .outerTagVlan()
                            .uint16()));
            trBuilder = trBuilder.setVlanId(ovrVlanNum);

        }

        return trBuilder.build();
    }

    private static TagManipulation getTagManipulation(FlowRule fr) {
        boolean isPop = false;
        boolean isPush = false;
        VlanId vlanId = null;
        EthType ethType = EtherType.VLAN.ethType(); //Default
        for (Instruction inst:fr.treatment().allInstructions()) {
            if (inst.type() == Instruction.Type.L2MODIFICATION) {
                L2ModificationInstruction l2Mod = (L2ModificationInstruction) inst;
                if (l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_POP) {
                    isPop = true;
                } else if (l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_PUSH) {
                    isPush = true;
                    ethType = ((ModVlanHeaderInstruction) l2Mod).ethernetType();
                } else if (l2Mod.subtype() == L2ModificationInstruction.L2SubType.VLAN_ID) {
                    vlanId = ((ModVlanIdInstruction) l2Mod).vlanId();
                }
            }
        }

        if (isPop) {
            //The should be no vlanId in this case
            TagPop pop = new DefaultTagPop();
            org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice
                .evcperuniextensionattributes.tagmanipulation
                .tagpop.TagPop popInner =
                    new org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317
                        .mseaunievcservice.evcperuniextensionattributes
                        .tagmanipulation.tagpop.DefaultTagPop();
            pop.tagPop(popInner);
            return pop;

        } else if (isPush && vlanId != null) {
            TagPush push = new DefaultTagPush();
            org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice
                .evcperuniextensionattributes.tagmanipulation
                .tagpush.TagPush pushInner =
                    new org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317
                        .mseaunievcservice.evcperuniextensionattributes
                        .tagmanipulation.tagpush.DefaultTagPush();
            pushInner.outerTagVlan(new VlanIdType(vlanId.id()));
            pushInner.pushTagType(ethType.equals(EtherType.VLAN.ethType()) ?
                                PushTagTypeEnum.PUSHCTAG : PushTagTypeEnum.PUSHSTAG);
            push.tagPush(pushInner);
            return push;

        } else if (vlanId != null) { //This is overwrite, as it has vlanId, but not push or pop
            TagOverwrite ovr = new DefaultTagOverwrite();
            org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317.mseaunievcservice
                .evcperuniextensionattributes.tagmanipulation
                .tagoverwrite.TagOverwrite ovrInner =
                    new org.onosproject.yang.gen.v1.mseaunievcservice.rev20160317
                        .mseaunievcservice.evcperuniextensionattributes
                        .tagmanipulation.tagoverwrite.DefaultTagOverwrite();
            ovrInner.outerTagVlan(new VlanIdType(vlanId.id()));
            ovr.tagOverwrite(ovrInner);
            return ovr;
        }

        return null;
    }

    private static long getMeterId(TrafficTreatment treatment) {
        return (treatment.metered() != null && treatment.metered().meterId() != null)
                ? treatment.metered().meterId().id() : 0L;
    }

    /**
     * An enumerated type that characterises the 2 port layout of the EA1000 device.
     * The device is in an SFP package and has only 2 ports, the HOST port which
     * plugs in to the chassis (Port 1) and the Optics Port on the rear (Port 0).
     */
    public enum Ea1000Port {
        HOST(1),
        OPTICS(0);

        private int num = 0;
        private Ea1000Port(int num) {
            this.num = num;
        }

        /**
         * The numerical assignment of this port.
         * @return The port number
         */
        public int portNum() {
            return num;
        }

        /**
         * Return the enumerated value from a port number.
         * @param num The port number
         * @return An enumerated value
         */
        public static Ea1000Port fromNum(long num) {
            for (Ea1000Port a:Ea1000Port.values()) {
                if (a.num == num) {
                    return a;
                }
            }
            return HOST;
        }

        /**
         * Get the port that the UNI-N is present on.
         * @param side The assignment of UNI-side to port
         * @return An enumerated value
         */
        public static Ea1000Port uniNNum(UniSideInterfaceAssignmentEnum side) {
            if (side.equals(UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST)) {
                return OPTICS;
            } else {
                return HOST;
            }
        }

        /**
         * Get the port that the UNI-C is present on.
         * @param side The assignment of UNI-side to port
         * @return An enumerated value
         */
        public static Ea1000Port uniCNum(UniSideInterfaceAssignmentEnum side) {
            if (side.equals(UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST)) {
                return HOST;
            } else {
                return OPTICS;
            }
        }

        /**
         * Get the port opposite the current port.
         * @return An enumerated value for the opposite side
         */
        public Ea1000Port opposite() {
            if (this.equals(HOST)) {
                return OPTICS;
            } else {
                return HOST;
            }
        }

        /**
         * Evaluate which side of the UNI on the EA1000 device this port refers to.
         * @param side The assignment of UNI-side to port
         * @return An enumerated value representing the UniSide
         */
        public UniSide nOrC(UniSideInterfaceAssignmentEnum side) {
            if ((this == HOST && side == UniSideInterfaceAssignmentEnum.UNI_C_ON_HOST) ||
                    (this == OPTICS && side == UniSideInterfaceAssignmentEnum.UNI_C_ON_OPTICS)) {
                return UniSide.CUSTOMER;
            } else {
                return UniSide.NETWORK;
            }
        }
    }
}
