/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.teyang.utils.topology;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.node.TeTerminationPoint;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NetworkId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.rev20151208.ietfnetwork.NodeId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.TpId;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.augmentedndnode.DefaultTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.augmentedndnode.TerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.augmentedndnode.terminationpoint
                       .DefaultSupportingTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.network.topology.rev20151208
               .ietfnetworktopology.networks.network.node.augmentedndnode.terminationpoint
                       .SupportingTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.interfaceswitchingcapabilitylist.DefaultInterfaceSwitchingCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.interfaceswitchingcapabilitylist.InterfaceSwitchingCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.node.terminationpoint.AugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708
               .ietftetopology.networks.network.node.terminationpoint.DefaultAugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20160708.ietftetopology.teterminationpointaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTpId;

import com.google.common.collect.Lists;

/**
 * The termination point translations.
 */
public final class TerminationPointConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TP = "TeSubsystem terminationPoint object cannot be null";
    private static final String E_NULL_YANG_TP = "YANG terminationPoint object cannot be null";

    // no instantiation
    private TerminationPointConverter() {
    }

    /**
     * TerminationPoint object translation from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem termination point object
     * @return TerminationPoint YANG object
     */
    public static TerminationPoint teSubsystem2YangTerminationPoint(
                                       org.onosproject.tetopology.management.api.node.TerminationPoint teSubsystem) {
        checkNotNull(teSubsystem, E_NULL_TE_SUBSYSTEM_TP);

        TpId tpId = TpId.fromString(teSubsystem.id().toString());
        TerminationPoint.TerminationPointBuilder builder =
                new DefaultTerminationPoint.TerminationPointBuilder().tpId(tpId);

        if (teSubsystem.getSupportingTpIds() != null) {
            List<SupportingTerminationPoint> tps = Lists.newArrayList();
            SupportingTerminationPoint.SupportingTerminationPointBuilder
                    spTpBuilder = DefaultSupportingTerminationPoint.builder();
            for (TerminationPointKey tpKey : teSubsystem.getSupportingTpIds()) {
                tps.add(spTpBuilder.networkRef(NetworkId.fromString(tpKey.networkId().toString()))
                                   .nodeRef(NodeId.fromString(tpKey.nodeId().toString()))
                                   .tpRef(TpId.fromString(tpKey.tpId().toString()))
                                   .build());
            }
            builder = builder.supportingTerminationPoint(tps);
        }

        if (teSubsystem.getTe() != null) {
            AugmentedNtTerminationPoint.AugmentedNtTerminationPointBuilder
                    tpAugmentBuilder = DefaultAugmentedNtTerminationPoint.builder();

            TeTerminationPoint teSubsystemTe = teSubsystem.getTe();
            TeBuilder yangTeBuilder = DefaultTe.builder();

            if (teSubsystemTe.teTpId() != null) {
                yangTeBuilder = yangTeBuilder.teTpId(TeTpId.fromString(teSubsystemTe.teTpId().toString()));
            }

            Config yConfig = teSubsystem2YangTeAugConfig(teSubsystemTe);
            yangTeBuilder = yangTeBuilder.config(yConfig);

            State yState = teSubsystem2YangTeAugState(teSubsystemTe);
            yangTeBuilder = yangTeBuilder.state(yState);

            tpAugmentBuilder = tpAugmentBuilder.te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(tpAugmentBuilder.build(), AugmentedNtTerminationPoint.class);
        }

        return builder.build();
    }

    private static State teSubsystem2YangTeAugState(TeTerminationPoint teSubsystemTe) {
        State.StateBuilder yangStateBuilder = DefaultState.builder();
        yangStateBuilder.interLayerLockId(teSubsystemTe.getInterLayerLockId());

        if (teSubsystemTe.interfaceSwitchingCapabilities() != null) {
            for (org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability teIsc :
                    teSubsystemTe.interfaceSwitchingCapabilities()) {
                InterfaceSwitchingCapability.InterfaceSwitchingCapabilityBuilder isc =
                        DefaultInterfaceSwitchingCapability.builder();
                // FIXME: teIsc at this moment is empty, therefore we cannot
                // really add its attributes to isc
                yangStateBuilder.addToInterfaceSwitchingCapability(isc.build());
            }
        }
        return yangStateBuilder.build();
    }

    private static Config teSubsystem2YangTeAugConfig(TeTerminationPoint teSubsystemTe) {
        Config.ConfigBuilder yangConfigBuilder = DefaultConfig.builder();
        yangConfigBuilder = yangConfigBuilder.interLayerLockId(teSubsystemTe.getInterLayerLockId());
        if (teSubsystemTe.interfaceSwitchingCapabilities() != null) {
            for (org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability teIsc :
                    teSubsystemTe.interfaceSwitchingCapabilities()) {
                InterfaceSwitchingCapability.InterfaceSwitchingCapabilityBuilder
                    isc = DefaultInterfaceSwitchingCapability.builder();
                // FIXME: teIsc at this moment is empty, therefore we cannot
                // really add its attributes to isc
                yangConfigBuilder = yangConfigBuilder.addToInterfaceSwitchingCapability(isc.build());
            }
        }
        return yangConfigBuilder.build();
    }

    /**
     * TerminationPoint object translation from YANG to TE Topology subsystem.
     *
     * @param yangTp TerminationPoint YANG object
     * @return TerminationPoint TE Topology subsystem termination point object
     */
    public static org.onosproject.tetopology.management.api.node.TerminationPoint
                      yang2teSubsystemTerminationPoint(TerminationPoint yangTp) {
        checkNotNull(yangTp, E_NULL_YANG_TP);

        org.onosproject.tetopology.management.api.node.DefaultTerminationPoint tp = new org.onosproject.tetopology
                .management.api.node.DefaultTerminationPoint(KeyId.keyId(yangTp.tpId().uri().string()));

        if (yangTp.supportingTerminationPoint() != null) {
            List<org.onosproject.tetopology.management.api.node.TerminationPointKey> spTps = Lists.newArrayList();
            for (SupportingTerminationPoint yangSptp : yangTp.supportingTerminationPoint()) {
                org.onosproject.tetopology.management.api.node.TerminationPointKey tpKey =
                        new org.onosproject.tetopology.management.api.node.TerminationPointKey(
                                KeyId.keyId(yangSptp.networkRef().uri().string()),
                                KeyId.keyId(yangSptp.nodeRef().uri().string()),
                                KeyId.keyId(yangSptp.tpRef().uri().string()));
                spTps.add(tpKey);
            }
            tp.setSupportingTpIds(spTps);
        }

        if (yangTp.yangAugmentedInfoMap() != null && !yangTp.yangAugmentedInfoMap().isEmpty()) {
            AugmentedNtTerminationPoint yangTpAugment =
                    (AugmentedNtTerminationPoint) yangTp.yangAugmentedInfo(AugmentedNtTerminationPoint.class);
            if (yangTpAugment.te() != null && yangTpAugment.te().teTpId() != null) {
                KeyId teTpId = KeyId.keyId(yangTpAugment.te().teTpId().toString());
                if (yangTpAugment.te().config() != null) {
                    long interLayerLockId = yangTpAugment.te().config().interLayerLockId();
                    List<org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability>
                            teIscList = Lists.newArrayList();
                    if (yangTpAugment.te().config().interfaceSwitchingCapability() != null) {
                        for (InterfaceSwitchingCapability iscConfigYang :
                                yangTpAugment.te().config().interfaceSwitchingCapability()) {
                            org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability iscTe =
                                    new org.onosproject.tetopology.management.api.node.InterfaceSwitchingCapability();
                            // FIXME: at this moment, iscTe does not have any
                            // attributes. Therefore, I cannot feed it with
                            // attributes of iscConfigYang
                            teIscList.add(iscTe);
                        }
                    }

                    TeTerminationPoint teSubsystemTp = new TeTerminationPoint(teTpId,
                                                                              teIscList,
                                                                              interLayerLockId);
                    tp.setTe(teSubsystemTp);
                }
            }
        }
        return tp;
    }

}
