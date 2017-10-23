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

package org.onosproject.segmentrouting.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.Config;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intf.InterfaceService;
import org.onosproject.segmentrouting.pwaas.DefaultL2Tunnel;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelDescription;
import org.onosproject.segmentrouting.pwaas.DefaultL2TunnelPolicy;
import org.onosproject.segmentrouting.pwaas.L2Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * App configuration object for Pwaas.
 */
public class PwaasConfig extends Config<ApplicationId> {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    public InterfaceService intfService;

    private static Logger log = LoggerFactory
            .getLogger(PwaasConfig.class);

    private static final String SRC_CP = "cP1";
    private static final String DST_CP = "cP2";
    private static final String SRC_OUTER_TAG = "cP1OuterTag";
    private static final String DST_OUTER_TAG = "cP2OuterTag";
    private static final String SRC_INNER_TAG = "cP1InnerTag";
    private static final String DST_INNER_TAG = "cP2InnerTag";
    private static final String MODE = "mode";
    private static final String SD_TAG = "sdTag";
    private static final String PW_LABEL = "pwLabel";

    public PwaasConfig(DeviceService devS, InterfaceService intfS) {

        super();

        deviceService = devS;
        intfService = intfS;
    }

    public PwaasConfig() {

        super();

        deviceService = AbstractShellCommand.get(DeviceService.class);
        intfService = AbstractShellCommand.get(InterfaceService.class);
    }
    /**
     * Error message for missing parameters.
     */
    private static final String MISSING_PARAMS = "Missing parameters in pseudo wire description";

    /**
     * Error message for invalid l2 mode.
     */
    private static final String INVALID_L2_MODE = "Invalid pseudo wire mode";

    /**
     * Error message for invalid VLAN.
     */
    private static final String INVALID_VLAN = "Vlan should be either int or */-";

    /**
     * Error message for invalid PW label.
     */
    private static final String INVALID_PW_LABEL = "Pseudowire label should be an integer";

    /**
     * Verify if the pwaas configuration block is valid.
     *
     * Here we try to ensure that the provided pseudowires will get instantiated
     * correctly in the network. We also check for any collisions with already used
     * interfaces and also between different pseudowires. Most of the restrictions stem
     * from the fact that all vlan matching is done in table 10 of ofdpa.
     *
     * @return true, if the configuration block is valid.
     *         False otherwise.
     */
    @Override
    public boolean isValid() {

        Set<DefaultL2TunnelDescription> pseudowires;
        try {
            pseudowires = getPwIds().stream()
                    .map(this::getPwDescription)
                    .collect(Collectors.toSet());

        } catch (IllegalArgumentException e) {
            log.warn("{}", e.getMessage());
            return false;
        }

        // check semantics now and return
        return configurationValidity(pseudowires);
    }

    /**
     * Helper method to verify if the tunnel is whether or not
     * supported.
     *
     * @param l2Tunnel the tunnel to verify
     * @return the result of the verification
     */
    private void verifyTunnel(DefaultL2Tunnel l2Tunnel) {

        // Service delimiting tag not supported yet.
        if (!l2Tunnel.sdTag().equals(VlanId.NONE)) {
            throw new IllegalArgumentException(String.format("Service delimiting tag not supported yet for " +
                                                             "pseudowire %d.", l2Tunnel.tunnelId()));
        }

        // Tag mode not supported yet.
        if (l2Tunnel.pwMode() == L2Mode.TAGGED) {
            throw new IllegalArgumentException(String.format("Tagged mode not supported yet for pseudowire %d.",
                                                                            l2Tunnel.tunnelId()));
        }

        // Raw mode without service delimiting tag
        // is the only mode supported for now.
    }

    /**
     * Helper method to verify if the policy is whether or not
     * supported and if policy will be successfully instantiated in the
     * network.
     *
     * @param ingressInner the ingress inner tag
     * @param ingressOuter the ingress outer tag
     * @param egressInner the egress inner tag
     * @param egressOuter the egress outer tag
     * @return the result of verification
     */
    private void verifyPolicy(ConnectPoint cP1,
                              ConnectPoint cP2,
                              VlanId ingressInner,
                              VlanId ingressOuter,
                              VlanId egressInner,
                              VlanId egressOuter,
                              Long tunnelId) {

        if (cP1.deviceId().equals(cP2.deviceId())) {
            throw new IllegalArgumentException(String.format("Pseudowire connection points can not reside in the " +
                                                             "same node, in pseudowire %d.", tunnelId));
        }

        // We can have multiple tags, all of them can be NONE,
        // indicating untagged traffic, however, the outer tag can
        // not have value if the inner tag is None
        if (ingressInner.equals(VlanId.NONE) && !ingressOuter.equals(VlanId.NONE)) {
            throw new IllegalArgumentException(String.format("Inner tag should not be empty when " +
                                                             "outer tag is set for pseudowire %d for cP1.",
                                                              tunnelId));
        }

        if (egressInner.equals(VlanId.NONE) && !egressOuter.equals(VlanId.NONE)) {
            throw new IllegalArgumentException(String.valueOf(String.format("Inner tag should not be empty when" +
                                                                            " outer tag is set for pseudowire %d " +
                                                                            "for cP2.", tunnelId)));
        }

        if (ingressInner.equals(VlanId.ANY) ||
                ingressOuter.equals(VlanId.ANY) ||
                egressInner.equals(VlanId.ANY) ||
                egressOuter.equals(VlanId.ANY)) {
            throw new IllegalArgumentException(String.valueOf(String.format("Wildcard VLAN matching not yet " +
                                                                            "supported for pseudowire %d.",
                                                                            tunnelId)));
        }

        if (((!ingressOuter.equals(VlanId.NONE) && !ingressOuter.equals(VlanId.NONE)) &&
                (egressOuter.equals(VlanId.NONE) && egressInner.equals(VlanId.NONE)))
                || ((ingressOuter.equals(VlanId.NONE) && ingressOuter.equals(VlanId.NONE)) &&
                (!egressOuter.equals(VlanId.NONE) && !egressInner.equals(VlanId.NONE)))) {
            throw new IllegalArgumentException(String.valueOf(String.format("Support for double tag <-> untag is not" +
                                                                                    "supported for pseudowire %d.",
                                                                            tunnelId)));
        }
        if ((!ingressInner.equals(VlanId.NONE) &&
                ingressOuter.equals(VlanId.NONE) &&
                !egressOuter.equals(VlanId.NONE))
           || (!ingressOuter.equals(VlanId.NONE) &&
                egressOuter.equals(VlanId.NONE) &&
                !ingressOuter.equals(VlanId.NONE))) {
                throw new IllegalArgumentException(String.valueOf(String.format("Support for double-tag<->" +
                                                                                "single-tag is not supported" +
                                                                                " for pseudowire %d.", tunnelId)));
        }

        if ((ingressInner.equals(VlanId.NONE) && !egressInner.equals(VlanId.NONE))
                || (!ingressInner.equals(VlanId.NONE) && egressInner.equals(VlanId.NONE))) {
            throw new IllegalArgumentException(String.valueOf(String.format("single-tag <-> untag is not supported" +
                                                                            " for pseudowire %d.", tunnelId)));
        }


        if (!ingressInner.equals(egressInner) && !ingressOuter.equals(egressOuter)) {
            throw new IllegalArgumentException(String.valueOf(String.format("We do not support changing both tags " +
                                                                             "in double tagged pws, only the outer," +
                                                                             " for pseudowire %d.", tunnelId)));
        }

        // check if cp1 and port of cp1 exist
        if (deviceService.getDevice(cP1.deviceId()) == null) {
            throw new IllegalArgumentException(String.valueOf(String.format("cP1 device %s does not exist for" +
                                                                            " pseudowire %d.", cP1.deviceId(),
                                                                            tunnelId)));
        }

        if (deviceService.getPort(cP1) == null) {
            throw new IllegalArgumentException(String.valueOf(String.format("Port %s for cP1 device %s does not" +
                                                                            " exist for pseudowire %d.", cP1.port(),
                                                                            cP1.deviceId(), tunnelId)));
        }

        // check if cp2 and port of cp2 exist
        if (deviceService.getDevice(cP2.deviceId()) == null) {
            throw new IllegalArgumentException(String.valueOf(String.format("cP2 device %s does not exist for" +
                                                                            " pseudowire %d.", cP2.deviceId(),
                                                                            tunnelId)));
        }

        if (deviceService.getPort(cP2) == null) {
            throw new IllegalArgumentException(String.valueOf(String.format("Port %s for cP2 device %s does " +
                                                                            "not exist for pseudowire %d.",
                                                                            cP2.port(), cP2.deviceId(), tunnelId)));
        }
    }

    /**
     * Verifies that the pseudowires will not conflict with each other.
     *
     * Further, check if vlans for connect points are already used.
     *
     * @param tunnel Tunnel for pw
     * @param policy Policy for pw
     * @param labelSet Label set used so far with this configuration
     * @param vlanSet Vlan set used with this configuration
     * @param tunnelSet Tunnel set used with this configuration
     */
    private void verifyGlobalValidity(DefaultL2Tunnel tunnel,
                                      DefaultL2TunnelPolicy policy,
                                      Set<MplsLabel> labelSet,
                                      Map<ConnectPoint, Set<VlanId>> vlanSet,
                                      Set<Long> tunnelSet) {

        if (tunnelSet.contains(tunnel.tunnelId())) {
            throw new IllegalArgumentException(String.valueOf(String.format("Tunnel Id %d already used by" +
                                                                           " another pseudowire, in " +
                                                                           "pseudowire %d!", tunnel.tunnelId(),
                                                                            tunnel.tunnelId())));
        }
        tunnelSet.add(tunnel.tunnelId());

        // check if tunnel id is used again
        ConnectPoint cP1 = policy.cP1();
        ConnectPoint cP2 = policy.cP2();

        // insert cps to hashmap if this is the first time seen
        if (!vlanSet.containsKey(cP1)) {
            vlanSet.put(cP1, new HashSet<VlanId>());
        }
        if (!vlanSet.containsKey(cP2)) {
            vlanSet.put(cP2, new HashSet<VlanId>());
        }

        // if single tagged or untagged vlan is the inner
        // if double tagged vlan is the outer
        VlanId vlanToCheckCP1;
        if (policy.cP1OuterTag().equals(VlanId.NONE)) {
            vlanToCheckCP1 = policy.cP1InnerTag();
        } else {
            vlanToCheckCP1 = policy.cP1OuterTag();
        }

        VlanId vlanToCheckCP2;
        if (policy.cP2OuterTag().equals(VlanId.NONE)) {
            vlanToCheckCP2 = policy.cP2InnerTag();
        } else {
            vlanToCheckCP2 = policy.cP2OuterTag();
        }

        if (labelSet.contains(tunnel.pwLabel())) {
            throw new IllegalArgumentException(String.valueOf(String.format("Label %s already used by another" +
                                                                            " pseudowire, in pseudowire %d!",
                                                                            tunnel.pwLabel(), tunnel.tunnelId())));
        }
        labelSet.add(tunnel.pwLabel());

        if (vlanSet.get(cP1).contains(vlanToCheckCP1)) {
            throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP1 %s already used " +
                                                                            "by another pseudowire, in pseudowire" +
                                                                            " %d!", vlanToCheckCP1,  cP1,
                                                                            tunnel.tunnelId())));
        }
        vlanSet.get(cP1).add(vlanToCheckCP1);

        if (vlanSet.get(cP2).contains(vlanToCheckCP2)) {
            throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP2 %s already used" +
                                                                            " by another pseudowire, in" +
                                                                            " pseudowire %d!", vlanToCheckCP2, cP2,
                                                                            tunnel.tunnelId())));
        }
        vlanSet.get(cP2).add(vlanToCheckCP2);

        // check that vlans for the connect points are not used
        intfService.getInterfacesByPort(cP1).stream()
                .forEach(intf -> {

                    // check if tagged pw affects tagged interface
                    if (intf.vlanTagged().contains(vlanToCheckCP1)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP1 %s already" +
                                                                                        " used for this interface, in" +
                                                                                        " pseudowire %d!",
                                                                                        vlanToCheckCP1, cP1,
                                                                                        tunnel.tunnelId())));
                    }

                    // if vlanNative != null this interface is configured with untagged traffic also
                    // check if it collides with untagged interface
                    if ((intf.vlanNative() != null) && vlanToCheckCP1.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for cP1 " +
                                                                                         "%s already used for this " +
                                                                                         "interface, in pseudowire " +
                                                                                         "%d!", cP1,
                                                                                         tunnel.tunnelId())));
                    }

                    // if vlanUntagged != null this interface is configured only with untagged traffic
                    // check if it collides with untagged interface
                    if ((intf.vlanUntagged() != null) && vlanToCheckCP1.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for " +
                                                                                         "cP1 %s already" +
                                                                                         " used for this interface," +
                                                                                         " in pseudowire %d!",
                                                                                         cP1, tunnel.tunnelId())));
                    }
                });

        intfService.getInterfacesByPort(cP2).stream()
                .forEach(intf -> {
                    if (intf.vlanTagged().contains(vlanToCheckCP2)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP2 %s already" +
                                                                                        " used for  this interface, " +
                                                                                        "in pseudowire %d!",
                                                                                        vlanToCheckCP2, cP2,
                                                                                        tunnel.tunnelId())));
                    }

                    // if vlanNative != null this interface is configured with untagged traffic also
                    // check if it collides with untagged interface
                    if ((intf.vlanNative() != null) && vlanToCheckCP2.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for cP2 %s " +
                                                                                        "already used for this" +
                                                                                        " interface, " +
                                                                                        "in pseudowire %d!",
                                                                                        cP2, tunnel.tunnelId())));
                    }

                    // if vlanUntagged != null this interface is configured only with untagged traffic
                    // check if it collides with untagged interface
                    if ((intf.vlanUntagged() != null) && vlanToCheckCP2.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for cP2 %s" +
                                                                                        " already" +
                                                                                        " used for this interface, " +
                                                                                        "in pseudowire %d!",
                                                                                        cP2, tunnel.tunnelId())));
                    }
                });

    }

    /**
     * Helper method to verify the integrity of the pseudo wire.
     *
     * @param l2TunnelDescription the pseudo wire description
     * @return the result of the check
     */
    private void verifyPseudoWire(DefaultL2TunnelDescription l2TunnelDescription,
                                  Set<MplsLabel> labelSet,
                                  Map<ConnectPoint, Set<VlanId>> vlanset,
                                  Set<Long> tunnelSet) {

        DefaultL2Tunnel l2Tunnel = l2TunnelDescription.l2Tunnel();
        DefaultL2TunnelPolicy l2TunnelPolicy = l2TunnelDescription.l2TunnelPolicy();

        verifyTunnel(l2Tunnel);

        verifyPolicy(
                l2TunnelPolicy.cP1(),
                l2TunnelPolicy.cP2(),
                l2TunnelPolicy.cP1InnerTag(),
                l2TunnelPolicy.cP1OuterTag(),
                l2TunnelPolicy.cP2InnerTag(),
                l2TunnelPolicy.cP2OuterTag(),
                l2Tunnel.tunnelId()
        );

        verifyGlobalValidity(l2Tunnel,
                             l2TunnelPolicy,
                             labelSet,
                             vlanset,
                             tunnelSet);

    }

    /**
     * Checks if the configured pseudowires will create problems in the network.
     * If yes, then no pseudowires is deployed from this configuration.
     *
     * @param pseudowires Set of pseudowries to validate
     * @return returns true if everything goes well.
     */
    public boolean configurationValidity(Set<DefaultL2TunnelDescription> pseudowires) {

        // structures to keep pw information
        // in order to see if instantiating them will create
        // problems
        Set<Long> tunIds = new HashSet<>();
        Set<MplsLabel> labelsUsed = new HashSet<>();
        Map<ConnectPoint, Set<VlanId>> vlanIds = new HashMap<>();

        // check that pseudowires can be instantiated in the network
        // we try to guarantee that all the pws will work before
        // instantiating any of them
        for (DefaultL2TunnelDescription pw : pseudowires) {
            verifyPseudoWire(pw, labelsUsed, vlanIds, tunIds);
        }

        return true;
    }

    /**
     * Returns all pseudo wire keys.
     *
     * @return all keys (tunnels id)
     * @throws IllegalArgumentException if wrong format
     */
    public Set<Long> getPwIds() {
        ImmutableSet.Builder<Long> builder = ImmutableSet.builder();
        object.fields().forEachRemaining(entry -> {
            Long tunnelId = Long.parseLong(entry.getKey());
            builder.add(tunnelId);
        });
        return builder.build();
    }

    /**
     * Parses a vlan as a string. Returns the VlanId if
     * provided String can be parsed as an integer or is '' / '*'
     *
     * @param vlan string as read from configuration
     * @return VlanId
     * @throws IllegalArgumentException if wrong format of vlan
     */
    public VlanId parseVlan(String vlan) {

        if (vlan.equals("*") || vlan.equals("Any")) {
            return VlanId.vlanId("Any");
        } else if (vlan.equals("") || vlan.equals("None")) {
            return VlanId.vlanId("None");
        } else {
            try {
                VlanId newVlan = VlanId.vlanId(vlan);
                return newVlan;
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(INVALID_VLAN);
            }
        }
    }

    /**
     *
     * @param mode RAW or TAGGED
     * @return the L2Mode if input is correct
     * @throws  IllegalArgumentException if not supported mode
     */
    public L2Mode parseMode(String mode) {

        if (!mode.equals("RAW") && !mode.equals("TAGGED")) {
            throw  new IllegalArgumentException(INVALID_L2_MODE);
        }

        return L2Mode.valueOf(mode);
    }

    /**
     *
     * @param label the mpls label of the pseudowire
     * @return the MplsLabel
     * @throws IllegalArgumentException if label is invalid
     */
    public MplsLabel parsePWLabel(String label) {

        try {
            MplsLabel pwLabel = MplsLabel.mplsLabel(label);
            return pwLabel;
        } catch (Exception e) {
            throw new IllegalArgumentException(INVALID_PW_LABEL);
        }
    }

    /**
     * Returns pw description of given pseudo wire id.
     *
     * @param tunnelId pseudo wire key
     * @return set of l2 tunnel descriptions
     * @throws IllegalArgumentException if wrong format
     */
    public DefaultL2TunnelDescription getPwDescription(Long tunnelId) {
        JsonNode pwDescription = object.get(tunnelId.toString());
        if (!hasFields((ObjectNode) pwDescription,
                      SRC_CP, SRC_INNER_TAG, SRC_OUTER_TAG,
                      DST_CP, DST_INNER_TAG, DST_OUTER_TAG,
                      MODE, SD_TAG, PW_LABEL)) {
            throw new IllegalArgumentException(MISSING_PARAMS);
        }
        String tempString;

        tempString = pwDescription.get(SRC_CP).asText();
        ConnectPoint srcCp = ConnectPoint.deviceConnectPoint(tempString);

        tempString = pwDescription.get(DST_CP).asText();
        ConnectPoint dstCp = ConnectPoint.deviceConnectPoint(tempString);

        tempString = pwDescription.get(SRC_INNER_TAG).asText();
        VlanId srcInnerTag = parseVlan(tempString);

        tempString = pwDescription.get(SRC_OUTER_TAG).asText();
        VlanId srcOuterTag = parseVlan(tempString);

        tempString = pwDescription.get(DST_INNER_TAG).asText();
        VlanId dstInnerTag = parseVlan(tempString);

        tempString = pwDescription.get(DST_OUTER_TAG).asText();
        VlanId dstOuterTag = parseVlan(tempString);

        tempString = pwDescription.get(MODE).asText();
        L2Mode l2Mode = parseMode(tempString);

        tempString = pwDescription.get(SD_TAG).asText();
        VlanId sdTag = parseVlan(tempString);

        tempString = pwDescription.get(PW_LABEL).asText();
        MplsLabel pwLabel = parsePWLabel(tempString);

        DefaultL2Tunnel l2Tunnel = new DefaultL2Tunnel(
                l2Mode,
                sdTag,
                tunnelId,
                pwLabel
        );

        DefaultL2TunnelPolicy l2TunnelPolicy = new DefaultL2TunnelPolicy(
                tunnelId,
                srcCp,
                srcInnerTag,
                srcOuterTag,
                dstCp,
                dstInnerTag,
                dstOuterTag
        );

        return new DefaultL2TunnelDescription(l2Tunnel, l2TunnelPolicy);
    }

    /**
     * Removes a pseudowire from the configuration tree.
     * @param pwId Pseudowire id
     * @return null if pwId did not exist, or the object representing the
     * udpated configuration tree
     */
    public ObjectNode removePseudowire(String pwId) {

        JsonNode value = object.remove(pwId);
        if (value == null) {
            return (ObjectNode) value;
        } else {
            return object;
        }
    }

    /**
     * Adds a pseudowire to the configuration tree of pwwas. It also checks
     * if the configuration is valid, if not return null and does not add the node,
     * if yes return the new configuration. Caller will propagate update events.
     *
     * If the pseudowire already exists in the configuration it gets updated.
     *
     * @param tunnelId Id of tunnel
     * @param pwLabel PW label of tunnel
     * @param cP1 Connection point 1
     * @param cP1InnerVlan Inner vlan of cp1
     * @param cP1OuterVlan Outer vlan of cp2
     * @param cP2 Connection point 2
     * @param cP2InnerVlan Inner vlan of cp2
     * @param cP2OuterVlan Outer vlan of cp2
     * @param mode Mode for the pw
     * @param sdTag Service delimiting tag for the pw
     * @return The ObjectNode config if configuration is valid with the new pseudowire
     * or null.
     */
    public ObjectNode addPseudowire(String tunnelId, String pwLabel, String cP1,
                                    String cP1InnerVlan, String cP1OuterVlan, String cP2,
                                    String cP2InnerVlan, String cP2OuterVlan,
                                    String mode, String sdTag) {


        ObjectNode newPw = new ObjectNode(JsonNodeFactory.instance);

        // add fields for pseudowire
        newPw.put(SRC_CP, cP1);
        newPw.put(DST_CP, cP2);
        newPw.put(PW_LABEL, pwLabel);
        newPw.put(SRC_INNER_TAG, cP1InnerVlan);
        newPw.put(SRC_OUTER_TAG, cP1OuterVlan);
        newPw.put(DST_INNER_TAG, cP2InnerVlan);
        newPw.put(DST_OUTER_TAG, cP2OuterVlan);
        newPw.put(SD_TAG, sdTag);
        newPw.put(MODE, mode);

        object.set(tunnelId, newPw);
        try {
            isValid();
        } catch (IllegalArgumentException e) {
            log.info("Pseudowire could not be created : {}", e);
            object.remove(tunnelId);
            return null;
        }

        return object;
    }
}
