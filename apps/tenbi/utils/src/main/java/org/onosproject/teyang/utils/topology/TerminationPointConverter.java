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
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.interfaceswitchingcapabilitylist.DefaultInterfaceSwitchingCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.interfaceswitchingcapabilitylist.InterfaceSwitchingCapability;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.node.terminationpoint.AugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110
               .ietftetopology.networks.network.node.terminationpoint.DefaultAugmentedNtTerminationPoint;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkiscdattributes.DefaultMaxLspBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.telinkiscdattributes.MaxLspBandwidth;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.DefaultTe.TeBuilder;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.te.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.te.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.te.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.topology.rev20170110.ietftetopology.teterminationpointaugment.te.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TeTpId;

import com.google.common.collect.Lists;

/**
 * The termination point translations.
 */
public final class TerminationPointConverter {

    private static final String E_NULL_TE_SUBSYSTEM_TP =
            "TeSubsystem terminationPoint object cannot be null";
    private static final String E_NULL_YANG_TP =
            "YANG terminationPoint object cannot be null";

    // no instantiation
    private TerminationPointConverter() {
    }

    /**
     * TerminationPoint object translation from TE Topology subsystem to YANG.
     *
     * @param teSubsystem TE Topology subsystem termination point
     * @return Termination point in YANG Java data structure
     */
    public static TerminationPoint teSubsystem2YangTerminationPoint(org.onosproject.tetopology.management.api.node.
                                                                    TerminationPoint teSubsystem) {
        checkNotNull(teSubsystem, E_NULL_TE_SUBSYSTEM_TP);

        TpId tpId = TpId.fromString(teSubsystem.tpId().toString());
//        TpId tpId = TpId.fromString("0");
        TerminationPoint.TerminationPointBuilder builder =
                new DefaultTerminationPoint.TerminationPointBuilder().tpId(tpId);

        if (teSubsystem.supportingTpIds() != null) {
            List<SupportingTerminationPoint> tps = Lists.newArrayList();
            SupportingTerminationPoint.SupportingTerminationPointBuilder
                    spTpBuilder = DefaultSupportingTerminationPoint.builder();
            for (TerminationPointKey tpKey : teSubsystem.supportingTpIds()) {
                tps.add(spTpBuilder.networkRef(NetworkId.fromString(tpKey.networkId().toString()))
                                   .nodeRef(NodeId.fromString(tpKey.nodeId().toString()))
                                   .tpRef(TpId.fromString(tpKey.tpId().toString()))
                                   .build());
            }
            builder = builder.supportingTerminationPoint(tps);
        }

        if (teSubsystem.teTpId() != null) {
            AugmentedNtTerminationPoint.AugmentedNtTerminationPointBuilder
                    tpAugmentBuilder = DefaultAugmentedNtTerminationPoint.builder();
            tpAugmentBuilder.teTpId(TeTpId.fromString((String.valueOf(teSubsystem.teTpId()))));
            TeBuilder yangTeBuilder = DefaultTe.builder();

//            Config yConfig = teSubsystem2YangTeAugConfig(teSubsystem);
//            yangTeBuilder = yangTeBuilder.config(yConfig);
//
//            State yState = teSubsystem2YangTeAugState(teSubsystem);
//            yangTeBuilder = yangTeBuilder.state(yState);

            tpAugmentBuilder = tpAugmentBuilder.te(yangTeBuilder.build());
            builder.addYangAugmentedInfo(tpAugmentBuilder.build(), AugmentedNtTerminationPoint.class);
        }

        return builder.build();
    }

    private static State teSubsystem2YangTeAugState(org.onosproject.tetopology.management.api.node.
            TerminationPoint teSubsystemTe) {
        State.StateBuilder yangStateBuilder = DefaultState.builder();
        // FIXME: interLayerLocks is a list in core but not in yang
//        yangStateBuilder = yangStateBuilder.interLayerLockId(teLink.interLayerLocks().get(0));

        return yangStateBuilder.build();
    }

    private static Config teSubsystem2YangTeAugConfig(org.onosproject.tetopology.management.api.node.
                                                      TerminationPoint teSubsystemTe) {
        Config.ConfigBuilder yangConfigBuilder = DefaultConfig.builder();
        //FIXME: interLayerLocks is a list in core but not in yang
        // yangConfigBuilder =
        // yangConfigBuilder.interLayerLockId(teLink.interLayerLocks().get(0));

        InterfaceSwitchingCapability.InterfaceSwitchingCapabilityBuilder isc =
                DefaultInterfaceSwitchingCapability.builder();

        MaxLspBandwidth.MaxLspBandwidthBuilder maxlspBW = DefaultMaxLspBandwidth
                .builder();
//        for (float f : teLink.maxAvialLspBandwidth()) {
//            // is converting from float to long ok?
//            maxlspBW = maxlspBW.bandwidth(BigDecimal.valueOf((long) f));
//            isc = isc.addToMaxLspBandwidth(maxlspBW.build());
//        }

        yangConfigBuilder = yangConfigBuilder.addToInterfaceSwitchingCapability(isc.build());

        return yangConfigBuilder.build();
    }

    /**
     * TerminationPoint object translation from YANG to TE Topology subsystem.
     *
     * @param yangTp Termination point in YANG Java data structure
     * @return TerminationPoint TE Topology subsystem termination point
     */
    public static org.onosproject.tetopology.management.api.node.TerminationPoint
                      yang2teSubsystemTerminationPoint(TerminationPoint yangTp) {
        checkNotNull(yangTp, E_NULL_YANG_TP);

        org.onosproject.tetopology.management.api.node.DefaultTerminationPoint tp = null;
        List<org.onosproject.tetopology.management.api.node.TerminationPointKey> spTps = null;
        KeyId teTpId = null;

        if (yangTp.supportingTerminationPoint() != null) {
            spTps = Lists.newArrayList();
            for (SupportingTerminationPoint yangSptp : yangTp.supportingTerminationPoint()) {
                org.onosproject.tetopology.management.api.node.TerminationPointKey tpKey =
                        new org.onosproject.tetopology.management.api.node.TerminationPointKey(
                                KeyId.keyId(yangSptp.networkRef().uri().string()),
                                KeyId.keyId(yangSptp.nodeRef().uri().string()),
                                KeyId.keyId(yangSptp.tpRef().uri().string()));
                spTps.add(tpKey);
            }
        }

        if (yangTp.yangAugmentedInfoMap() != null && !yangTp.yangAugmentedInfoMap().isEmpty()) {
            AugmentedNtTerminationPoint yangTpAugment =
                    (AugmentedNtTerminationPoint) yangTp.yangAugmentedInfo(AugmentedNtTerminationPoint.class);
            if (yangTpAugment.teTpId() != null) {
                teTpId = KeyId.keyId(yangTpAugment.teTpId().toString());
            }
        }

        tp = new org.onosproject.tetopology.management.api.node
                .DefaultTerminationPoint(KeyId.keyId(yangTp.tpId().uri().string()),
                                         spTps,
                                         teTpId == null ? null : Long.valueOf(teTpId.toString()));
        return tp;
    }

}
