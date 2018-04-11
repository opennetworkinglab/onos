/*
 * Copyright 2015-present Open Networking Foundation
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

package org.onosproject.segmentrouting.pwaas;

import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onosproject.net.device.DeviceService;
import org.onosproject.net.intf.InterfaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Utility class with static methods that help
 * parse pseudowire related information and also
 * verify that a pseudowire combination is valid.
 */
public final class PwaasUtil {

    private static final Logger log = LoggerFactory.getLogger(PwaasUtil.class);

    private static DeviceService deviceService = AbstractShellCommand.get(DeviceService.class);

    private static InterfaceService intfService = AbstractShellCommand.get(InterfaceService.class);

    private PwaasUtil() {
        return;
    }

    /**
     * Parses a vlan as a string. Returns the VlanId if
     * provided String can be parsed as an integer or is '' / '*'
     *
     * @param vlan string as read from configuration
     * @return VlanId null if error
     */
    public static VlanId parseVlan(String vlan) {

        if (vlan.equals("*") || vlan.equals("Any")) {
            return VlanId.vlanId("Any");
        } else if (vlan.equals("") || vlan.equals("None")) {
            return VlanId.vlanId("None");
        } else {
            return VlanId.vlanId(vlan);
        }
    }

    /**
     *
     * @param mode RAW or TAGGED
     * @return the L2Mode if input is correct
     */
    public static L2Mode parseMode(String mode) {
        checkArgument(mode.equals("RAW") || mode.equals("TAGGED"),
                      "Invalid pseudowire mode of operation, should be TAGGED or RAW.");
        return L2Mode.valueOf(mode);
    }

    /**
     *
     * @param label the mpls label of the pseudowire
     * @return the MplsLabel
     * @throws IllegalArgumentException if label is invalid
     */
    public static MplsLabel parsePWLabel(String label) {
        return MplsLabel.mplsLabel(label);
    }

    /**
     * Parses a string as a pseudowire id - which is an integer.
     *
     * @param id The id of pw in string form
     * @return The id of pw as an Integer or null if it failed the conversion.
     */
    public static Integer parsePwId(String id) {
        return Integer.parseInt(id);
    }

    /**
     * Helper method to verify if the tunnel is whether or not
     * supported.
     *
     * @param l2Tunnel the tunnel to verify
     */
    private static void verifyTunnel(L2Tunnel l2Tunnel) {

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
     */
    private static void verifyPolicy(ConnectPoint cP1,
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
                                                                                    " outer tag is set for " +
                                                                                    "pseudowire %d " +
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

        if (((!ingressOuter.equals(VlanId.NONE) && !ingressInner.equals(VlanId.NONE)) &&
                (egressOuter.equals(VlanId.NONE) && egressInner.equals(VlanId.NONE)))
                || ((ingressOuter.equals(VlanId.NONE) && ingressInner.equals(VlanId.NONE)) &&
                (!egressOuter.equals(VlanId.NONE) && !egressInner.equals(VlanId.NONE)))) {
            throw new IllegalArgumentException(String.valueOf(String.format("Support for double tag <-> untag is not" +
                                                                                    "supported for pseudowire %d.",
                                                                            tunnelId)));
        }
        if ((!ingressInner.equals(VlanId.NONE) &&
                ingressOuter.equals(VlanId.NONE) &&
                !egressOuter.equals(VlanId.NONE))
                || (egressOuter.equals(VlanId.NONE) &&
                !egressInner.equals(VlanId.NONE) &&
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
                                                                                    "in double tagged pws, only the " +
                                                                                    "outer," +
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
                                                                                    " exist for pseudowire %d.",
                                                                            cP1.port(),
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
    private static void verifyGlobalValidity(L2Tunnel tunnel,
                                      L2TunnelPolicy policy,
                                      Set<MplsLabel> labelSet,
                                      Map<ConnectPoint, Set<VlanId>> vlanSet,
                                      Set<Long> tunnelSet) {

        if (tunnelSet.contains(tunnel.tunnelId())) {
            throw new IllegalArgumentException(String.valueOf(String.format("Tunnel Id %d already used by" +
                                                                                    " another pseudowire, in " +
                                                                                    "pseudowire %d!",
                                                                            tunnel.tunnelId(),
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
                                                                                    "by another pseudowire, in " +
                                                                                    "pseudowire" +
                                                                                    " %d!", vlanToCheckCP1,  cP1,
                                                                            tunnel.tunnelId())));
        }
        vlanSet.get(cP1).add(vlanToCheckCP1);

        if (vlanSet.get(cP2).contains(vlanToCheckCP2)) {
            throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP2 %s already used" +
                                                                                    " by another pseudowire, in" +
                                                                                    " pseudowire %d!", vlanToCheckCP2,
                                                                            cP2,
                                                                            tunnel.tunnelId())));
        }
        vlanSet.get(cP2).add(vlanToCheckCP2);

        // check that vlans for the connect points are not used
        intfService.getInterfacesByPort(cP1).stream()
                .forEach(intf -> {

                    // check if tagged pw affects tagged interface
                    if (intf.vlanTagged().contains(vlanToCheckCP1)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP1 %s already" +
                                                                                                " used for this" +
                                                                                                " interface, in" +
                                                                                                " pseudowire %d!",
                                                                                        vlanToCheckCP1, cP1,
                                                                                        tunnel.tunnelId())));
                    }

                    // if vlanNative != null this interface is configured with untagged traffic also
                    // check if it collides with untagged interface
                    if ((intf.vlanNative() != null) && vlanToCheckCP1.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for cP1 " +
                                                                                                "%s already used " +
                                                                                                "for this " +
                                                                                                "interface, in " +
                                                                                                "pseudowire " +
                                                                                                "%d!", cP1,
                                                                                        tunnel.tunnelId())));
                    }

                    // if vlanUntagged != null this interface is configured only with untagged traffic
                    // check if it collides with untagged interface
                    if ((intf.vlanUntagged() != null) && vlanToCheckCP1.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for " +
                                                                                                "cP1 %s already" +
                                                                                                " used for this" +
                                                                                                " interface," +
                                                                                                " in pseudowire %d!",
                                                                                        cP1, tunnel.tunnelId())));
                    }
                });

        intfService.getInterfacesByPort(cP2).stream()
                .forEach(intf -> {
                    if (intf.vlanTagged().contains(vlanToCheckCP2)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Vlan '%s' for cP2 %s " +
                                                                                                " used for  " +
                                                                                                "this interface, " +
                                                                                                "in pseudowire %d!",
                                                                                        vlanToCheckCP2, cP2,
                                                                                        tunnel.tunnelId())));
                    }

                    // if vlanNative != null this interface is configured with untagged traffic also
                    // check if it collides with untagged interface
                    if ((intf.vlanNative() != null) && vlanToCheckCP2.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic " +
                                                                                                "for cP2 %s " +
                                                                                                "already " +
                                                                                                "used for this" +
                                                                                                " interface, " +
                                                                                                "in pseudowire %d!",
                                                                                        cP2, tunnel.tunnelId())));
                    }

                    // if vlanUntagged != null this interface is configured only with untagged traffic
                    // check if it collides with untagged interface
                    if ((intf.vlanUntagged() != null) && vlanToCheckCP2.equals(VlanId.NONE)) {
                        throw new IllegalArgumentException(String.valueOf(String.format("Untagged traffic for cP2 %s" +
                                                                                                " already" +
                                                                                                " used for " +
                                                                                                "this interface, " +
                                                                                                "in pseudowire %d!",
                                                                                        cP2, tunnel.tunnelId())));
                    }
                });

    }

    /**
     * Helper method to verify the integrity of the pseudo wire.
     *
     * @param l2TunnelDescription the pseudo wire description
     */
    private static void verifyPseudoWire(L2TunnelDescription l2TunnelDescription,
                                  Set<MplsLabel> labelSet,
                                  Map<ConnectPoint, Set<VlanId>> vlanset,
                                  Set<Long> tunnelSet) {

        L2Tunnel l2Tunnel = l2TunnelDescription.l2Tunnel();
        L2TunnelPolicy l2TunnelPolicy = l2TunnelDescription.l2TunnelPolicy();

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

    public static L2TunnelHandler.Result configurationValidity(List<L2TunnelDescription> pseudowires) {

        // structures to keep pw information
        // in order to see if instantiating them will create
        // problems
        Set<Long> tunIds = new HashSet<>();
        Set<MplsLabel> labelsUsed = new HashSet<>();
        Map<ConnectPoint, Set<VlanId>> vlanIds = new HashMap<>();

        // TODO : I know we should not use exceptions for flow control,
        // however this code was originally implemented in the configuration
        // addition where the exceptions were propagated and the configuration was
        // deemed not valid. I plan in the future to refactor the parts that
        // check the pseudowire validity.
        //
        // Ideally we would like to return a String which could also return to
        // the user issuing the rest request for adding the pseudowire.
        try {
            // check that pseudowires can be instantiated in the network
            // we try to guarantee that all the pws will work before
            // instantiating any of them
            for (L2TunnelDescription pw : pseudowires) {
                log.debug("Verifying pseudowire {}", pw);
                verifyPseudoWire(pw, labelsUsed, vlanIds, tunIds);
            }

            return L2TunnelHandler.Result.SUCCESS;
        } catch (Exception e) {
            log.error("Caught exception while validating pseudowire : {}", e.getMessage());
            return L2TunnelHandler.Result.CONFIGURATION_ERROR
                    .appendError(e.getMessage());
        }
    }
}
