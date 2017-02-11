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

package org.onosproject.teyang.utils.tunnel;

import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.tunnel.DefaultTeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.onosproject.tetunnel.api.tunnel.path.DefaultTePath;
import org.onosproject.tetunnel.api.tunnel.path.DefaultTeRouteUnnumberedLink;
import org.onosproject.tetunnel.api.tunnel.path.TePath;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTeOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.Type;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.DefaultDynamic;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.DefaultExplicit;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.explicit.DefaultExplicitRouteObjects;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.explicit.ExplicitRouteObjects;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.DefaultPrimaryPaths;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.DefaultState;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.PrimaryPaths;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.DefaultTunnels;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.Tunnels;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.tunnels.DefaultTunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.tunnels.Tunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProt1Forn;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtBidir1To1;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtReroute;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtRerouteExtra;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtUnidir1To1;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtUnprotected;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.PathSignalingRsvpte;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.PathSignalingSr;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.RouteIncludeEro;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.StateDown;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.StateUp;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TunnelP2Mp;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TunnelP2p;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TunnelType;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultUnnumberedLink;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.UnnumberedLink;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.packet.ONOSLLDP.DEFAULT_NAME;
import static org.onosproject.tetunnel.api.tunnel.TeTunnel.LspProtectionType;
import static org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject.Type.UNNUMBERED_LINK;
import static org.onosproject.teyang.utils.tunnel.BasicConverter.bytesToLong;
import static org.onosproject.teyang.utils.tunnel.BasicConverter.ipToLong;
import static org.onosproject.teyang.utils.tunnel.BasicConverter.longToByte;
import static org.onosproject.teyang.utils.tunnel.BasicConverter.longToIp;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Tunnel convert utils.
 */
public final class TunnelConverter {

    private static final Logger log = getLogger(TunnelConverter.class);
    private static final String DEFAULT_PATH_NAME = "ietfPath";
    private static final String DEFAULT_PATH_CONSTRAINT = "ietfPath";
    private static final int DEFAULT_PATH_PREFERENCE = 1;
    private static final boolean DEFAULT_CSPF_STATE = true;
    private static final boolean DEFAULT_LOCKDOWN_STATE = true;

    // no instantiation
    private TunnelConverter() {
    }

    /**
     * Build a general IETF TE object with a giving tunnel list. for there are
     * many kind of attributes in IETF TE, now we only care about the tunnel
     * attributes.
     *
     * @param tunnels tunnels in the TE network
     * @return IETF te info in the TE network
     */
    public static IetfTe buildIetfTeWithTunnels(List<Tunnel> tunnels) {
        Tunnels teTunnels = new DefaultTunnels
                .TunnelsBuilder()
                .tunnel(tunnels)
                .build();
        Te te = new DefaultTe
                .TeBuilder()
                .tunnels(teTunnels)
                .build();
        return new IetfTeOpParam
                .IetfTeBuilder()
                .te(te)
                .yangIetfTeOpType(IetfTe.OnosYangOpType.NONE)
                .build();
    }

    public static IetfTe buildIetfTe(TeTunnel teTunnel, boolean isConfig) {
        Tunnel tunnel = te2YangTunnelConverter(teTunnel, isConfig);
        return buildIetfTeWithTunnels(Lists.newArrayList(tunnel));
    }

    /**
     * Converts a specific te tunnel defined in the APP to the general te tunnel
     * defined in YANG model.
     *
     * @param tunnel te tunnel defined in APP
     * @param isConfig true if tunnel is to be built with config attributes;
     *                 false if built with state attributes
     * @return tunnel defined in YANG model
     */
    public static Tunnel te2YangTunnelConverter(TeTunnel tunnel, boolean isConfig) {
        List<PrimaryPaths> pathsList = new ArrayList<>();

        if (tunnel.primaryPaths() != null) {
            tunnel.primaryPaths()
                    .forEach(tePath -> pathsList.add(te2YangPrimaryPath(tePath)));
        }

        tunnel.primaryPaths()
                .forEach(tePath -> pathsList.add(te2YangPrimaryPath(tePath)));

        Tunnel.TunnelBuilder builder = new DefaultTunnel
                .TunnelBuilder()
                .type(te2YangTunnelType(tunnel.type()))
                .name(validName(tunnel.name()))
                .identifier(tunnel.teTunnelKey().teTunnelId())
                .state(te2YangTunnelState(tunnel))
                .primaryPaths(pathsList);
        Tunnel.TunnelBuilder tunnelBuilder = isConfig ?
                builder.config(te2YangTunnelConfig(tunnel)) :
                builder.state(te2YangTunnelState(tunnel));

        return tunnelBuilder.build();
    }

    private static State te2YangTunnelState(TeTunnel tunnel) {
        State.StateBuilder stateBuilder = new DefaultState.StateBuilder();
        stateBuilder.name(validName(tunnel.name()))
                .identifier((int) tunnel.teTunnelKey().teTunnelId())
                .source((longToIp(tunnel.srcNode().teNodeId())))
                .destination((longToIp(tunnel.dstNode().teNodeId())))
                .srcTpId(longToByte(tunnel.srcTp().ttpId()))
                .dstTpId(longToByte(tunnel.dstTp().ttpId()))
                .adminStatus(te2YangStateType(tunnel.adminStatus()))
                .lspProtectionType(
                        te2YangProtectionType(tunnel.lspProtectionType()))
                .type(te2YangTunnelType(tunnel.type()))
                .build();
        return stateBuilder.build();
    }

    private static Config te2YangTunnelConfig(TeTunnel tunnel) {
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
                rev20160705.ietfte.tunnelproperties.DefaultConfig.ConfigBuilder
                configBuilder = new org.onosproject.yang.gen.v1.urn.ietf.params.
                xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.
                DefaultConfig.ConfigBuilder();

        configBuilder.name(validName(tunnel.name()))
                .identifier((int) tunnel.teTunnelKey().teTunnelId())
                .source((longToIp(tunnel.srcNode().teNodeId())))
                .destination((longToIp(tunnel.dstNode().teNodeId())))
                .srcTpId(longToByte(tunnel.srcTp().ttpId()))
                .dstTpId(longToByte(tunnel.dstTp().ttpId()))
                .adminStatus(te2YangStateType(tunnel.adminStatus()))
                .lspProtectionType(
                        te2YangProtectionType(tunnel.lspProtectionType()))
                .type(te2YangTunnelType(tunnel.type()))
                .build();
        return configBuilder.build();
    }

    private static String validName(String name) {
        //for name is a required attribute, here we give a default name if not
        //configured
        return isNullOrEmpty(name) ? DEFAULT_NAME : name;
    }

    private static PrimaryPaths te2YangPrimaryPath(TePath tePath) {
        DefaultPrimaryPaths.PrimaryPathsBuilder builder = new DefaultPrimaryPaths
                .PrimaryPathsBuilder()
                .name(DEFAULT_PATH_NAME)
                .preference(DEFAULT_PATH_PREFERENCE)
                .state(te2YangPrimaryPathState(tePath))
                .yangPrimaryPathsOpType(IetfTe.OnosYangOpType.NONE);
        return builder.build();
    }

    private static org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf
            .te.rev20160705.ietfte.p2pprimarypathparams.State
    te2YangPrimaryPathState(TePath tePath) {

        List<TeRouteSubobject> teRouteSubobjects = tePath.explicitRoute();

        List<ExplicitRouteObjects> routeObjects = new ArrayList<>();
        teRouteSubobjects.forEach(teRouteSubobject -> {
            routeObjects.add(te2YangRouteSubobject(teRouteSubobject));
        });
        DefaultExplicit.ExplicitBuilder explicitBuilder =
                DefaultExplicit.builder().explicitRouteObjects(routeObjects);

        return new org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.
                te.rev20160705.ietfte.p2pprimarypathparams.DefaultState
                .StateBuilder()
                .type(explicitBuilder.build())
                .pathNamedConstraint(DEFAULT_PATH_CONSTRAINT)
                .noCspf(DEFAULT_CSPF_STATE)
                .lockdown(DEFAULT_LOCKDOWN_STATE)
                .build();
    }

    private static ExplicitRouteObjects
    te2YangRouteSubobject(TeRouteSubobject routeSubobject) {

        TeRouteSubobject.Type type = routeSubobject.type();
        UnnumberedLink yanglink = null;
        //TODO implement other kind of TeRouteSubobject type
        if (type == UNNUMBERED_LINK) {
            DefaultTeRouteUnnumberedLink unnumberedLink =
                    (DefaultTeRouteUnnumberedLink) routeSubobject;
            TeNodeKey nodeKey = unnumberedLink.node();
            TtpKey ttpKey = unnumberedLink.ttp();

            yanglink = DefaultUnnumberedLink.builder()
                    .routerId(longToIp(nodeKey.teNodeId()))
                    .interfaceId(ttpKey.ttpId())
                    .build();

        }

        //TODO implement other kind of explicitRoute usage type
        return DefaultExplicitRouteObjects.builder()
                .type(yanglink)
                .explicitRouteUsage(RouteIncludeEro.class)
                .build();
    }

    /**
     * Converts a YANG TE tunnel defined in the YANG model to a specific TE
     * tunnel defined in the TE tunnel APP.
     *
     * @param tunnel      yang tunnel object
     * @param topologyKey key of the TE topology to which this tunnel belongs
     * @return default Te tunnel defined in TE tunnel APP
     */
    public static DefaultTeTunnel yang2TeTunnel(org.onosproject.yang.gen.v1.
                                                        urn.ietf.params.xml.
                                                        ns.yang.ietf.te.
                                                        rev20160705.ietfte.
                                                        tunnelsgrouping.
                                                        tunnels.Tunnel
                                                        tunnel,
                                                TeTopologyKey topologyKey) {
        //get config info
        Config config = tunnel.config();

        //build basic attribute, node and ttp
        TeNodeKey srcNodeKey = new TeNodeKey(topologyKey, ipToLong(config.source()));
        TeNodeKey dstNodeKey = new TeNodeKey(topologyKey, ipToLong(config.destination()));

        TtpKey srcTtpKey = new TtpKey(srcNodeKey, bytesToLong(config.srcTpId()));
        TtpKey dstTtpKey = new TtpKey(srcNodeKey, bytesToLong(config.dstTpId()));

        //check if paths have been set
        List<PrimaryPaths> primaryPaths = tunnel.primaryPaths();
        List<TePath> paths = new ArrayList<>();
        primaryPaths.forEach(primaryPath -> paths.add(
                yang2TePrimaryPaths(primaryPath, topologyKey)));

        //build the te tunnel
        DefaultTeTunnel.Builder builder = new DefaultTeTunnel.Builder();

        return builder.teTunnelKey(new TeTunnelKey(topologyKey, config.identifier()))
                .name(config.name())
                .type(yang2TeTunnelType(config.type()))
                .lspProtectionType(yang2TeProtectionType(config.lspProtectionType()))
                .adminState(yang2TeStateType(config.adminStatus()))
                .srcNode(srcNodeKey)
                .dstNode(dstNodeKey)
                .srcTp(srcTtpKey)
                .dstTp(dstTtpKey)
                .primaryPaths(paths).build();
    }

    private static TePath yang2TePrimaryPaths(PrimaryPaths primaryPath,
                                              TeTopologyKey topologyKey) {
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
                rev20160705.ietfte.p2pprimarypathparams.Config
                pathConfig = primaryPath.config();

        TePath tePath;
        TePath.Type tePathType = null;
        Type type = pathConfig.type();

        if (type == null) {
            return new DefaultTePath(TePath.Type.DYNAMIC,
                                     Lists.newArrayList(),
                                     Lists.newArrayList(),
                                     Lists.newArrayList());
        }

        Class<? extends Type> typeClass = type.getClass();

        List<TeRouteSubobject> routeSubobjects = new ArrayList<>();

        if (typeClass.isAssignableFrom(DefaultExplicit.class)) {
            DefaultExplicit explicitPath = (DefaultExplicit) type;
            explicitPath
                    .explicitRouteObjects()
                    .forEach(o -> routeSubobjects.add(
                            yang2TeRouteSubobject(o, topologyKey)));
            tePathType = TePath.Type.EXPLICIT;

        } else if (typeClass.isAssignableFrom(DefaultDynamic.class)) {
            tePathType = TePath.Type.DYNAMIC;
        }

        tePath = new DefaultTePath(tePathType,
                                   Lists.newArrayList(),
                                   routeSubobjects,
                                   Lists.newArrayList());
        return tePath;
    }

    private static TeRouteSubobject
    yang2TeRouteSubobject(ExplicitRouteObjects routeObject,
                          TeTopologyKey topologyKey) {

        //TODO implement other types of route type
        DefaultUnnumberedLink type = (DefaultUnnumberedLink) routeObject.type();
        TeNodeKey nodeKey = new TeNodeKey(topologyKey, ipToLong(type.routerId()));
        TtpKey tpId = new TtpKey(nodeKey, type.interfaceId());
        return new DefaultTeRouteUnnumberedLink(nodeKey, tpId);
    }

    private static TeTunnel.Type yang2TeTunnelType(Class type) {
        TeTunnel.Type tunnelType = null;
        if (type.isAssignableFrom(TunnelP2Mp.class)) {
            tunnelType = TeTunnel.Type.P2MP;
        } else if (type.isAssignableFrom(TunnelP2p.class)) {
            tunnelType = TeTunnel.Type.P2P;
        } else if (type.isAssignableFrom(PathSignalingRsvpte.class)) {
            tunnelType = TeTunnel.Type.PATH_SIGNALING_RSVPTE;
        } else if (type.isAssignableFrom(PathSignalingSr.class)) {
            tunnelType = TeTunnel.Type.PATH_SIGNALING_SR;
        }
        return tunnelType;
    }


    private static Class<? extends TunnelType> te2YangTunnelType(TeTunnel.Type type) {
        Class<? extends TunnelType> tunnelType = null;
        switch (type) {

            case P2P:
                tunnelType = TunnelP2p.class;
                break;
            case P2MP:
                tunnelType = TunnelP2Mp.class;
                break;
            case PATH_SIGNALING_RSVPTE:
                tunnelType = PathSignalingRsvpte.class;

                break;
            case PATH_SIGNALING_SR:
                tunnelType = PathSignalingSr.class;
                break;
            default:
                log.error("Unknown te tunnel type {}", type.toString());
        }
        return tunnelType;
    }

    private static LspProtectionType
    yang2TeProtectionType(Class<? extends LspProtType> protType) {
        LspProtectionType type = null;
        if (protType.isAssignableFrom(LspProt1Forn.class)) {
            type = LspProtectionType.LSP_PROT_1_FOR_N;
        } else if (protType.isAssignableFrom(LspProtBidir1To1.class)) {
            type = LspProtectionType.LSP_PROT_BIDIR_1_TO_1;
        } else if (protType.isAssignableFrom(LspProtReroute.class)) {
            type = LspProtectionType.LSP_PROT_REROUTE;
        } else if (protType.isAssignableFrom(LspProtRerouteExtra.class)) {
            type = LspProtectionType.LSP_PROT_REROUTE_EXTRA;
        } else if (protType.isAssignableFrom(LspProtUnidir1To1.class)) {
            type = LspProtectionType.LSP_PROT_UNIDIR_1_TO_1;
        } else if (protType.isAssignableFrom(LspProtUnprotected.class)) {
            type = LspProtectionType.LSP_PROT_UNPROTECTED;
        }
        return type;
    }

    private static Class<? extends LspProtType>
    te2YangProtectionType(LspProtectionType protType) {
        Class<? extends LspProtType> type = null;
        switch (protType) {

            case LSP_PROT_UNPROTECTED:
                type = LspProtUnprotected.class;
                break;
            case LSP_PROT_REROUTE:
                type = LspProtReroute.class;
                break;
            case LSP_PROT_REROUTE_EXTRA:
                type = LspProtRerouteExtra.class;
                break;
            case LSP_PROT_UNIDIR_1_TO_1:
                type = LspProtUnidir1To1.class;
                break;
            case LSP_PROT_BIDIR_1_TO_1:
                type = LspProtBidir1To1.class;
                break;
            case LSP_PROT_1_FOR_N:
                type = LspProt1Forn.class;
                break;
            default:
                log.error("Unknown te tunnel type {}", protType.toString());
        }
        return type;
    }

    private static TeTunnel.State
    yang2TeStateType(Class<? extends org.onosproject.yang.gen.v1.urn.ietf.
            params.xml.ns.yang.ietf.te.types.
            rev20160705.ietftetypes.StateType> stateType) {
        TeTunnel.State teStateType = null;
        if (stateType.isAssignableFrom(StateUp.class)) {
            teStateType = TeTunnel.State.UP;
        } else if (stateType.isAssignableFrom(StateDown.class)) {
            teStateType = TeTunnel.State.DOWN;
        }
        return teStateType;
    }

    private static Class<? extends org.onosproject.yang.gen.v1.urn.ietf.params.
            xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.StateType>
    te2YangStateType(TeTunnel.State stateType) {
        Class<? extends org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.
                ietf.te.types.rev20160705.ietftetypes.StateType> state = null;

        switch (stateType) {

            case DOWN:
                state = StateDown.class;
                break;
            case UP:
                state = StateUp.class;
                break;
            default:
                log.error("Unknown te tunnel type {}", stateType.toString());

        }
        return state;
    }
}
