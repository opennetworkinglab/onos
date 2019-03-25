/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.packetthrottle;

import com.google.common.collect.ImmutableMap;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.net.packet.PacketInFilter;
import org.onosproject.net.packet.PacketService;
import org.onosproject.net.packet.packetfilter.DefaultPacketInFilter;
import org.onosproject.net.packet.packetfilter.ArpPacketClassifier;
import org.onosproject.net.packet.packetfilter.Dhcp6IndirectPacketClassifier;
import org.onosproject.net.packet.packetfilter.Dhcp6DirectPacketClassifier;
import org.onosproject.net.packet.packetfilter.DhcpPacketClassifier;
import org.onosproject.net.packet.packetfilter.NAPacketClassifier;
import org.onosproject.net.packet.packetfilter.NSPacketClassifier;
import org.onosproject.net.packet.packetfilter.IcmpPacketClassifier;
import org.onosproject.net.packet.packetfilter.Icmp6PacketClassifier;
import org.onosproject.packetthrottle.api.PacketThrottleService;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_ARP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_ARP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_DHCP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_DHCP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_NS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_NS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_NA;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_NA_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_DHCP6_DIRECT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_DHCP6_DIRECT_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_DHCP6_INDIRECT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_DHCP6_INDIRECT_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_ICMP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_ICMP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_PPS_ICMP6;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PPS_ICMP6_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_ARP_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_ARP_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_DHCP_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_DHCP_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_NA_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_NA_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_NS_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_NS_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_DHCP6_DIRECT_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_DHCP6_DIRECT_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_DHCP6_INDIRECT_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_DHCP6_INDIRECT_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_ICMP_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_ICMP_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_SIZE_ICMP6_MS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_SIZE_ICMP6_MS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_ARP_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_ARP_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_DHCP_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_DHCP_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_NS_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_NS_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_NA_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_NA_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_DHCP6_DIRECT_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_DHCP6_DIRECT_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_DHCP6_INDIRECT_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_DHCP6_INDIRECT_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_ICMP_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_ICMP_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_GUARD_TIME_ICMP6_SEC;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.GUARD_TIME_ICMP6_SEC_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_ARP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_ARP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_DHCP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_DHCP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_NS;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_NS_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_NA;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_NA_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_DHCP6_DIRECT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_DHCP6_DIRECT_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_DHCP6_INDIRECT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_DHCP6_INDIRECT_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_ICMP;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_ICMP_DEFAULT;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.PROP_WIN_THRES_ICMP6;
import static org.onosproject.packetthrottle.OsgiPropertyConstants.WIN_THRES_ICMP6_DEFAULT;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onlab.util.Tools.get;

/**
 * Manage the packet throttle for various type of packets.
 */
@Component(
        immediate = true,
        service = PacketThrottleService.class,
        property = {
                PROP_PPS_ARP + ":Integer=" + PPS_ARP_DEFAULT,
                PROP_PPS_DHCP + ":Integer=" + PPS_DHCP_DEFAULT,
                PROP_PPS_NS + ":Integer=" + PPS_NS_DEFAULT,
                PROP_PPS_NA + ":Integer=" + PPS_NA_DEFAULT,
                PROP_PPS_DHCP6_DIRECT + ":Integer=" + PPS_DHCP6_DIRECT_DEFAULT,
                PROP_PPS_DHCP6_INDIRECT + ":Integer=" + PPS_DHCP6_INDIRECT_DEFAULT,
                PROP_PPS_ICMP + ":Integer=" + PPS_ICMP_DEFAULT,
                PROP_PPS_ICMP6 + ":Integer=" + PPS_ICMP6_DEFAULT,
                PROP_WIN_SIZE_ARP_MS + ":Integer=" + WIN_SIZE_ARP_MS_DEFAULT,
                PROP_WIN_SIZE_DHCP_MS + ":Integer=" + WIN_SIZE_DHCP_MS_DEFAULT,
                PROP_WIN_SIZE_NA_MS + ":Integer=" + WIN_SIZE_NA_MS_DEFAULT,
                PROP_WIN_SIZE_NS_MS + ":Integer=" + WIN_SIZE_NS_MS_DEFAULT,
                PROP_WIN_SIZE_DHCP6_DIRECT_MS + ":Integer=" + WIN_SIZE_DHCP6_DIRECT_MS_DEFAULT,
                PROP_WIN_SIZE_DHCP6_INDIRECT_MS + ":Integer=" + WIN_SIZE_DHCP6_INDIRECT_MS_DEFAULT,
                PROP_WIN_SIZE_ICMP_MS + ":Integer=" + WIN_SIZE_ICMP_MS_DEFAULT,
                PROP_WIN_SIZE_ICMP6_MS + ":Integer=" + WIN_SIZE_ICMP6_MS_DEFAULT,
                PROP_GUARD_TIME_ARP_SEC + ":Integer=" + GUARD_TIME_ARP_SEC_DEFAULT,
                PROP_GUARD_TIME_DHCP_SEC + ":Integer=" + GUARD_TIME_DHCP_SEC_DEFAULT,
                PROP_GUARD_TIME_NS_SEC + ":Integer=" + GUARD_TIME_NS_SEC_DEFAULT,
                PROP_GUARD_TIME_NA_SEC + ":Integer=" + GUARD_TIME_NA_SEC_DEFAULT,
                PROP_GUARD_TIME_DHCP6_DIRECT_SEC + ":Integer=" + GUARD_TIME_DHCP6_DIRECT_SEC_DEFAULT,
                PROP_GUARD_TIME_DHCP6_INDIRECT_SEC + ":Integer=" + GUARD_TIME_DHCP6_INDIRECT_SEC_DEFAULT,
                PROP_GUARD_TIME_ICMP_SEC + ":Integer=" + GUARD_TIME_ICMP_SEC_DEFAULT,
                PROP_GUARD_TIME_ICMP6_SEC + ":Integer=" + GUARD_TIME_ICMP6_SEC_DEFAULT,
                PROP_WIN_THRES_ARP + ":Integer=" + WIN_THRES_ARP_DEFAULT,
                PROP_WIN_THRES_DHCP + ":Integer=" + WIN_THRES_DHCP_DEFAULT,
                PROP_WIN_THRES_NS + ":Integer=" + WIN_THRES_NS_DEFAULT,
                PROP_WIN_THRES_NA + ":Integer=" + WIN_THRES_NA_DEFAULT,
                PROP_WIN_THRES_DHCP6_DIRECT + ":Integer=" + WIN_THRES_DHCP6_DIRECT_DEFAULT,
                PROP_WIN_THRES_DHCP6_INDIRECT + ":Integer=" + WIN_THRES_DHCP6_INDIRECT_DEFAULT,
                PROP_WIN_THRES_ICMP + ":Integer=" + WIN_THRES_ICMP_DEFAULT,
                PROP_WIN_THRES_ICMP6 + ":Integer=" + WIN_THRES_ICMP6_DEFAULT
        }
)
public class PacketThrottleManager implements PacketThrottleService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected static final String ARP_FILTER = "arpFilter";
    protected static final String DHCP_FILTER = "dhcpFilter";
    protected static final String NS_FILTER = "nsFilter";
    protected static final String NA_FILTER = "naFilter";
    protected static final String DHCP6_DIRECT_FILTER = "dhcp6DirectFilter";
    protected static final String DHCP6_INDIRECT_FILTER = "dhcp6IndirectFilter";
    protected static final String ICMP_FILTER = "icmpFilter";
    protected static final String ICMP6_FILTER = "icmp6Filter";


    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    PacketService packetService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService configService;

    /**
     * Parameter to set packet per second rate for all filter types.
     */

    private int ppsArp = PPS_ARP_DEFAULT;

    private int ppsDhcp = PPS_DHCP_DEFAULT;

    private int ppsNs = PPS_NS_DEFAULT;

    private int ppsNa = PPS_NA_DEFAULT;

    private int ppsDhcp6Direct = PPS_DHCP6_DIRECT_DEFAULT;

    private int ppsDhcp6Indirect = PPS_DHCP6_INDIRECT_DEFAULT;

    private int ppsIcmp = PPS_ICMP_DEFAULT;

    private int ppsIcmp6 = PPS_ICMP6_DEFAULT;


    /**
     * Parameter to set window size in milli seconds to check overflow of packets.
     */

    private int winSizeArp = WIN_SIZE_ARP_MS_DEFAULT;

    private int winSizeDhcp = WIN_SIZE_DHCP_MS_DEFAULT;

    private int winSizeNs = WIN_SIZE_NS_MS_DEFAULT;

    private int winSizeNa = WIN_SIZE_NA_MS_DEFAULT;

    private int winSizeDhcp6Direct = WIN_SIZE_DHCP6_DIRECT_MS_DEFAULT;

    private int winSizeDhcp6Indirect = WIN_SIZE_DHCP6_INDIRECT_MS_DEFAULT;

    private int winSizeIcmp = WIN_SIZE_ICMP_MS_DEFAULT;

    private int winSizeIcmp6 = WIN_SIZE_ICMP6_MS_DEFAULT;

    /**
     * Time duration for which no more packets will be processed for a given filter type
     * provided consecutive overflow windows happens.
     */


    private int guardTimeArp = GUARD_TIME_ARP_SEC_DEFAULT;

    private int guardTimeDhcp = GUARD_TIME_DHCP_SEC_DEFAULT;

    private int guardTimeNs = GUARD_TIME_NS_SEC_DEFAULT;

    private int guardTimeNa = GUARD_TIME_NA_SEC_DEFAULT;

    private int guardTimeDhcp6Direct = GUARD_TIME_DHCP6_DIRECT_SEC_DEFAULT;

    private int guardTimeDhcp6Indirect = GUARD_TIME_DHCP6_INDIRECT_SEC_DEFAULT;

    private int guardTimeIcmp = GUARD_TIME_ICMP_SEC_DEFAULT;

    private int guardTimeIcmp6 = GUARD_TIME_ICMP6_SEC_DEFAULT;

    /**
     * Consecutive overflow window threshold.
     */


    private int winThresArp = WIN_THRES_ARP_DEFAULT;

    private int winThresDhcp = WIN_THRES_DHCP_DEFAULT;

    private int winThresNs = WIN_THRES_NS_DEFAULT;

    private int winThresNa = WIN_THRES_NA_DEFAULT;

    private int winThresDhcp6Direct = WIN_THRES_DHCP6_DIRECT_DEFAULT;

    private int winThresDhcp6Indirect = WIN_THRES_DHCP6_INDIRECT_DEFAULT;

    private int winThresIcmp = WIN_THRES_ICMP_DEFAULT;

    private int winThresIcmp6 = WIN_THRES_ICMP6_DEFAULT;




    private Map<String, PacketInFilter> mapCounterFilter = new HashMap<>();

    @Activate
    protected void activate() {
        log.info("Started");
        configService.registerProperties(getClass());
        createAllFilters();
    }

    @Deactivate
    protected void deactivate() {
        configService.unregisterProperties(getClass(), false);
        removeAllFilters();
        log.info("Stopped");
    }

    private void checkChangeInPps(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newPpsArp, newPpsDhcp, newPpsNs, newPpsNa, newPpsDhcp6Direct;
        int newPpsDhcp6Indirect, newPpsIcmp, newPpsIcmp6;
        try {
            String s = get(properties, PROP_PPS_ARP);
            newPpsArp = isNullOrEmpty(s) ? ppsArp : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_DHCP);
            newPpsDhcp = isNullOrEmpty(s) ? ppsDhcp : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_NS);
            newPpsNs = isNullOrEmpty(s) ? ppsNs : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_NA);
            newPpsNa = isNullOrEmpty(s) ? ppsNa : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_DHCP6_DIRECT);
            newPpsDhcp6Direct = isNullOrEmpty(s) ? ppsDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_DHCP6_INDIRECT);
            newPpsDhcp6Indirect = isNullOrEmpty(s) ? ppsDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_ICMP);
            newPpsIcmp = isNullOrEmpty(s) ? ppsIcmp : Integer.parseInt(s.trim());

            s = get(properties, PROP_PPS_ICMP6);
            newPpsIcmp6 = isNullOrEmpty(s) ? ppsIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newPpsArp = PPS_ARP_DEFAULT;
            newPpsDhcp = PPS_DHCP_DEFAULT;
            newPpsNs = PPS_NS_DEFAULT;
            newPpsNa = PPS_NA_DEFAULT;
            newPpsDhcp6Direct = PPS_DHCP6_DIRECT_DEFAULT;
            newPpsDhcp6Indirect = PPS_DHCP6_INDIRECT_DEFAULT;
            newPpsIcmp = PPS_ICMP_DEFAULT;
            newPpsIcmp6 = PPS_ICMP6_DEFAULT;
        }
        if (newPpsArp != ppsArp) {
            ppsArp = newPpsArp;
            mapCounterFilter.get(ARP_FILTER).setPps(ppsArp);
        }
        if (newPpsDhcp != ppsDhcp) {
            ppsDhcp = newPpsDhcp;
            mapCounterFilter.get(DHCP_FILTER).setPps(ppsDhcp);
        }
        if (newPpsNs != ppsNs) {
            ppsNs = newPpsNs;
            mapCounterFilter.get(NS_FILTER).setPps(ppsNs);
        }
        if (newPpsNa != ppsNa) {
            ppsNa = newPpsNa;
            mapCounterFilter.get(NA_FILTER).setPps(ppsNa);
        }
        if (newPpsDhcp6Direct != ppsDhcp6Direct) {
            ppsDhcp6Direct = newPpsDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setPps(ppsDhcp6Direct);
        }
        if (newPpsDhcp6Indirect != ppsDhcp6Indirect) {
            ppsDhcp6Indirect = newPpsDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setPps(ppsDhcp6Indirect);
        }
        if (newPpsIcmp != ppsIcmp) {
            ppsIcmp = newPpsIcmp;
            mapCounterFilter.get(ICMP_FILTER).setPps(ppsIcmp);
        }
        if (newPpsIcmp6 != ppsIcmp6) {
            ppsIcmp6 = newPpsIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setPps(ppsIcmp6);
        }
    }

    private void checkChangeInWinSize(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        int newWinSizeArp, newWinSizeDhcp, newWinSizeNs, newWinSizeNa;
        int newWinSizeDhcp6Direct, newWinSizeDhcp6Indirect, newWinSizeIcmp, newWinSizeIcmp6;
        try {
            String s = get(properties, PROP_WIN_SIZE_ARP_MS);
            newWinSizeArp = isNullOrEmpty(s) ? winSizeArp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_DHCP_MS);
            newWinSizeDhcp = isNullOrEmpty(s) ? winSizeDhcp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_NS_MS);
            newWinSizeNs = isNullOrEmpty(s) ? winSizeNs : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_NA_MS);
            newWinSizeNa = isNullOrEmpty(s) ? winSizeNa : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_DHCP6_DIRECT_MS);
            newWinSizeDhcp6Direct = isNullOrEmpty(s) ? winSizeDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_DHCP6_INDIRECT_MS);
            newWinSizeDhcp6Indirect = isNullOrEmpty(s) ? winSizeDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_ICMP_MS);
            newWinSizeIcmp = isNullOrEmpty(s) ? winSizeIcmp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_SIZE_ICMP6_MS);
            newWinSizeIcmp6 = isNullOrEmpty(s) ? winSizeIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {
            newWinSizeArp = WIN_SIZE_ARP_MS_DEFAULT;
            newWinSizeDhcp = WIN_SIZE_DHCP_MS_DEFAULT;
            newWinSizeNs = WIN_SIZE_NS_MS_DEFAULT;
            newWinSizeNa = WIN_SIZE_NA_MS_DEFAULT;
            newWinSizeDhcp6Direct = WIN_SIZE_DHCP6_DIRECT_MS_DEFAULT;
            newWinSizeDhcp6Indirect = WIN_SIZE_DHCP6_INDIRECT_MS_DEFAULT;
            newWinSizeIcmp = WIN_SIZE_ICMP_MS_DEFAULT;
            newWinSizeIcmp6 = WIN_SIZE_ICMP6_MS_DEFAULT;
        }
        if (newWinSizeArp != winSizeArp) {
            winSizeArp = newWinSizeArp;
            mapCounterFilter.get(ARP_FILTER).setWinSize(winSizeArp);
        }
        if (newWinSizeDhcp != winSizeDhcp) {
            winSizeDhcp = newWinSizeDhcp;
            mapCounterFilter.get(DHCP_FILTER).setWinSize(winSizeDhcp);
        }
        if (newWinSizeNs != winSizeNs) {
            winSizeNs = newWinSizeNs;
            mapCounterFilter.get(NS_FILTER).setWinSize(winSizeNs);
        }
        if (newWinSizeNa != winSizeNa) {
            winSizeNa = newWinSizeNa;
            mapCounterFilter.get(NA_FILTER).setWinSize(winSizeNa);
        }
        if (newWinSizeDhcp6Direct != winSizeDhcp6Direct) {
            winSizeDhcp6Direct = newWinSizeDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setWinSize(winSizeDhcp6Direct);
        }
        if (newWinSizeDhcp6Indirect != winSizeDhcp6Indirect) {
            winSizeDhcp6Indirect = newWinSizeDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setWinSize(winSizeDhcp6Indirect);
        }
        if (newWinSizeIcmp != winSizeIcmp) {
            winSizeIcmp = newWinSizeIcmp;
            mapCounterFilter.get(ICMP_FILTER).setWinSize(winSizeIcmp);
        }
        if (newWinSizeIcmp6 != winSizeIcmp6) {
            winSizeIcmp6 = newWinSizeIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setWinSize(winSizeIcmp6);
        }

    }

    private void checkChangeInGuardTime(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();
        int newGuardTimeArp, newGuardTimeDhcp, newGuardTimeNs, newGuardTimeNa;
        int newGuardTimeDhcp6Direct, newGuardTimeDhcp6Indirect, newGuardTimeIcmp, newGuardTimeIcmp6;
        try {
            String s = get(properties, PROP_GUARD_TIME_ARP_SEC);
            newGuardTimeArp = isNullOrEmpty(s) ? guardTimeArp : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_DHCP_SEC);
            newGuardTimeDhcp = isNullOrEmpty(s) ? guardTimeDhcp : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_NS_SEC);
            newGuardTimeNs = isNullOrEmpty(s) ? guardTimeNs : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_NA_SEC);
            newGuardTimeNa = isNullOrEmpty(s) ? guardTimeNa : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_DHCP6_DIRECT_SEC);
            newGuardTimeDhcp6Direct = isNullOrEmpty(s) ? guardTimeDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_DHCP6_INDIRECT_SEC);
            newGuardTimeDhcp6Indirect = isNullOrEmpty(s) ? guardTimeDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_ICMP_SEC);
            newGuardTimeIcmp = isNullOrEmpty(s) ? guardTimeIcmp : Integer.parseInt(s.trim());

            s = get(properties, PROP_GUARD_TIME_ICMP6_SEC);
            newGuardTimeIcmp6 = isNullOrEmpty(s) ? guardTimeIcmp6 : Integer.parseInt(s.trim());
        } catch (NumberFormatException | ClassCastException e) {

            newGuardTimeArp = GUARD_TIME_ARP_SEC_DEFAULT;
            newGuardTimeDhcp = GUARD_TIME_DHCP_SEC_DEFAULT;
            newGuardTimeNs = GUARD_TIME_NS_SEC_DEFAULT;
            newGuardTimeNa = GUARD_TIME_NA_SEC_DEFAULT;
            newGuardTimeDhcp6Direct = GUARD_TIME_DHCP6_DIRECT_SEC_DEFAULT;
            newGuardTimeDhcp6Indirect = GUARD_TIME_DHCP6_INDIRECT_SEC_DEFAULT;
            newGuardTimeIcmp = GUARD_TIME_ICMP_SEC_DEFAULT;
            newGuardTimeIcmp6 = GUARD_TIME_ICMP6_SEC_DEFAULT;
        }
        if (newGuardTimeArp != guardTimeArp) {
            guardTimeArp = newGuardTimeArp;
            mapCounterFilter.get(ARP_FILTER).setGuardTime(guardTimeArp);
        }
        if (newGuardTimeDhcp != guardTimeDhcp) {
            guardTimeDhcp = newGuardTimeDhcp;
            mapCounterFilter.get(DHCP_FILTER).setGuardTime(guardTimeDhcp);
        }
        if (newGuardTimeNs != guardTimeNs) {
            guardTimeNs = newGuardTimeNs;
            mapCounterFilter.get(NS_FILTER).setGuardTime(guardTimeNs);
        }
        if (newGuardTimeNa != guardTimeNa) {
            guardTimeNa = newGuardTimeNa;
            mapCounterFilter.get(NA_FILTER).setGuardTime(guardTimeNa);
        }
        if (newGuardTimeDhcp6Direct != guardTimeDhcp6Direct) {
            guardTimeDhcp6Direct = newGuardTimeDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setGuardTime(guardTimeDhcp6Direct);
        }
        if (newGuardTimeDhcp6Indirect != guardTimeDhcp6Indirect) {
            guardTimeDhcp6Indirect = newGuardTimeDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setGuardTime(guardTimeDhcp6Indirect);
        }
        if (newGuardTimeIcmp != guardTimeIcmp) {
            guardTimeIcmp = newGuardTimeIcmp;
            mapCounterFilter.get(ICMP_FILTER).setGuardTime(guardTimeIcmp);
        }
        if (newGuardTimeIcmp6 != guardTimeIcmp6) {
            guardTimeIcmp6 = newGuardTimeIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setGuardTime(guardTimeIcmp6);
        }


    }

    private void checkChangeInWinThres(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        int newWinThresArp, newWinThresDhcp, newWinThresNs, newWinThresNa;
        int newWinThresDhcp6Direct, newWinThresDhcp6Indirect, newWinThresIcmp, newWinThresIcmp6;
        try {

            String s = get(properties, PROP_WIN_THRES_ARP);
            newWinThresArp = isNullOrEmpty(s) ? winThresArp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_DHCP);
            newWinThresDhcp = isNullOrEmpty(s) ? winThresDhcp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_NS);
            newWinThresNs = isNullOrEmpty(s) ? winThresNs : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_NA);
            newWinThresNa = isNullOrEmpty(s) ? winThresNa : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_DHCP6_DIRECT);
            newWinThresDhcp6Direct = isNullOrEmpty(s) ? winThresDhcp6Direct : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_DHCP6_INDIRECT);
            newWinThresDhcp6Indirect = isNullOrEmpty(s) ? winThresDhcp6Indirect : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_ICMP);
            newWinThresIcmp = isNullOrEmpty(s) ? winThresIcmp : Integer.parseInt(s.trim());

            s = get(properties, PROP_WIN_THRES_ICMP6);
            newWinThresIcmp6 = isNullOrEmpty(s) ? winThresIcmp6 : Integer.parseInt(s.trim());

        } catch (NumberFormatException | ClassCastException e) {
            newWinThresArp = WIN_THRES_ARP_DEFAULT;
            newWinThresDhcp = WIN_THRES_DHCP_DEFAULT;
            newWinThresNs = WIN_THRES_NS_DEFAULT;
            newWinThresNa = WIN_THRES_NA_DEFAULT;
            newWinThresDhcp6Direct = WIN_THRES_DHCP6_DIRECT_DEFAULT;
            newWinThresDhcp6Indirect = WIN_THRES_DHCP6_INDIRECT_DEFAULT;
            newWinThresIcmp = WIN_THRES_ICMP_DEFAULT;
            newWinThresIcmp6 = WIN_THRES_ICMP6_DEFAULT;

        }

        if (newWinThresArp != winThresArp) {
            winThresArp = newWinThresArp;
            mapCounterFilter.get(ARP_FILTER).setWinThres(winThresArp);
        }
        if (newWinThresDhcp != winThresDhcp) {
            winThresDhcp = newWinThresDhcp;
            mapCounterFilter.get(DHCP_FILTER).setWinThres(winThresDhcp);
        }
        if (newWinThresNs != winThresNs) {
            winThresNs = newWinThresNs;
            mapCounterFilter.get(NS_FILTER).setWinThres(winThresNs);
        }
        if (newWinThresNa != winThresNa) {
            winThresNa = newWinThresNa;
            mapCounterFilter.get(NA_FILTER).setWinThres(winThresNa);
        }
        if (newWinThresDhcp6Direct != winThresDhcp6Direct) {
            winThresDhcp6Direct = newWinThresDhcp6Direct;
            mapCounterFilter.get(DHCP6_DIRECT_FILTER).setWinThres(winThresDhcp6Direct);
        }
        if (newWinThresDhcp6Indirect != winThresDhcp6Indirect) {
            winThresDhcp6Indirect = newWinThresDhcp6Indirect;
            mapCounterFilter.get(DHCP6_INDIRECT_FILTER).setWinThres(winThresDhcp6Indirect);
        }
        if (newWinThresIcmp != winThresIcmp) {
            winThresIcmp = newWinThresIcmp;
            mapCounterFilter.get(ICMP_FILTER).setWinThres(winThresIcmp);
        }
        if (newWinThresIcmp6 != winThresIcmp6) {
            winThresIcmp6 = newWinThresIcmp6;
            mapCounterFilter.get(ICMP6_FILTER).setWinThres(winThresIcmp6);
        }

    }

    @Modified
    private void modified(ComponentContext context) {
        if (context == null) {
            log.info("Default config");
            return;
        }

        checkChangeInPps(context);
        checkChangeInWinSize(context);
        checkChangeInGuardTime(context);
        checkChangeInWinThres(context);

        log.info("Reconfigured ppsArp: {} ppsDhcp: {} ppsNs: {} ppsNa: {} " +
                "ppsDhcp6Direct: {} ppsDhcp6Indirect: {} ppsIcmp: {} ppsIcmp6: {}",
                 ppsArp, ppsDhcp, ppsNs, ppsNa, ppsDhcp6Direct, ppsDhcp6Indirect,
                 ppsIcmp, ppsIcmp6);

        log.info("Reconfigured winSizeArp: {} winSizeDhcp: {} winSizeNs: {} winSizeNa: {} " +
                 "winSizeDhcp6Direct: {} winSizeDhcp6Indirect: {} winSizeIcmp: {} winSizeIcmp6: {}",
                 winSizeArp, winSizeDhcp, winSizeNs, winSizeNa, winSizeDhcp6Direct,
                 winSizeDhcp6Indirect, winSizeIcmp, winSizeIcmp6);

        log.info("Reconfigured guardTimeArp: {} guardTimeDhcp: {} guardTimeNs: {} guardTimeNa: {} " +
                 "guardTimeDhcp6Direct: {} guardTimeDhcp6Indirect: {} guardTimeIcmp: {} guardTimeIcmp6: {}",
                 guardTimeArp, guardTimeDhcp, guardTimeNs, guardTimeNa, guardTimeDhcp6Direct,
                 guardTimeDhcp6Indirect, guardTimeIcmp, guardTimeIcmp6);

        log.info("Reconfigured winThresArp: {} winThresDhcp: {} winThresNs: {} winThresNa: {} " +
                 "winThresDhcp6Direct: {} winThresDhcp6Indirect: {} winThresIcmp: {} winThresIcmp6: {}",
                 winThresArp, winThresDhcp, winThresNs, winThresNa, winThresDhcp6Direct,
                 winThresDhcp6Indirect, winThresIcmp, winThresIcmp6);
    }

    /**
     * Create all required filters.
     */
    private void createAllFilters() {
        DefaultPacketInFilter filter;
        ArpPacketClassifier arp = new ArpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsArp, winSizeArp, guardTimeArp, winThresArp, ARP_FILTER, arp);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        DhcpPacketClassifier dhcp4 = new DhcpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp, winSizeDhcp, guardTimeDhcp, winThresDhcp, DHCP_FILTER, dhcp4);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Dhcp6DirectPacketClassifier dhcp6Direct = new Dhcp6DirectPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp6Direct, winSizeDhcp6Direct, guardTimeDhcp6Direct,
                                           winThresDhcp6Direct, DHCP6_DIRECT_FILTER, dhcp6Direct);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Dhcp6IndirectPacketClassifier dhcp6Indirect = new Dhcp6IndirectPacketClassifier();
        filter = new DefaultPacketInFilter(ppsDhcp6Direct, winSizeDhcp6Direct, guardTimeDhcp6Direct,
                                           winThresDhcp6Direct, DHCP6_INDIRECT_FILTER, dhcp6Indirect);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        NAPacketClassifier na = new NAPacketClassifier();
        filter = new DefaultPacketInFilter(ppsNa, winSizeNa, guardTimeNa, winThresNa, NA_FILTER, na);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        NSPacketClassifier ns = new NSPacketClassifier();
        filter = new DefaultPacketInFilter(ppsNs, winSizeNs, guardTimeNs, winThresNs, NS_FILTER, ns);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        IcmpPacketClassifier icmp = new IcmpPacketClassifier();
        filter = new DefaultPacketInFilter(ppsIcmp, winSizeIcmp, guardTimeIcmp, winThresIcmp, ICMP_FILTER, icmp);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
        Icmp6PacketClassifier icmp6 = new Icmp6PacketClassifier();
        filter = new DefaultPacketInFilter(ppsIcmp6, winSizeIcmp6, guardTimeIcmp6, winThresIcmp6, ICMP6_FILTER, icmp6);
        packetService.addFilter(filter);
        mapCounterFilter.put(filter.name(), filter);
    }

    /**
     * Delete all the filters.
     */
    private void removeAllFilters() {
        packetService.clearFilters();
        mapCounterFilter.clear();
    }

    @Override
    public Map<String, PacketInFilter> filterMap() {
        return ImmutableMap.copyOf(mapCounterFilter);
    }



}
