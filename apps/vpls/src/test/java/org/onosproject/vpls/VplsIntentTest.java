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
package org.onosproject.vpls;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.incubator.net.intf.InterfaceListener;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentServiceAdapter;
import org.onosproject.net.intent.IntentUtils;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.vpls.api.VplsData;
import org.onosproject.vpls.intent.VplsIntentUtility;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.EncapsulationType.NONE;
import static org.onosproject.net.EncapsulationType.VLAN;

/**
 * Tests for {@link VplsIntentUtility}.
 */
public class VplsIntentTest extends VplsTest {

    private Set<Host> hostsAvailable;
    private IntentService intentService;
    private InterfaceService interfaceService;

    @Before
    public void setUp() throws Exception {
        MockIdGenerator.cleanBind();
        hostsAvailable = Sets.newHashSet();
        intentService = new TestIntentService();
        interfaceService = createMock(InterfaceService.class);
        interfaceService.addListener(anyObject(InterfaceListener.class));
        expectLastCall().anyTimes();
        addIfaceConfig();
    }

    @After
    public void tearDown() {
        MockIdGenerator.unbind();
    }

    /**
     * Creates the interface configuration:
     *  On devices 1 and 2 is configured an interface on port 1 with vlan 100.
     *  On device 3 is configured an interface on port 3 with no vlan.
     *  On devices 3 and 4 is configured an interface on port 1 with vlan 200.
     *  On device 4 is an interface configured on port 2 with vlan 400.
     *  On device 5 are configured two interfaces on port 1 and 2 with no vlan.
     *  On device 5 and 6 is configured an interface on port 1 with vlan 300.
     */
    private void addIfaceConfig() {
        Set<Interface> interfaces = ImmutableSet.copyOf(AVAILABLE_INTERFACES);
        Set<Interface> vlanOneSet = ImmutableSet.of(V100H1, V100H2);
        Set<Interface> vlanTwoSet = ImmutableSet.of(V200H1, V200H2);
        Set<Interface> vlanThreeSet = ImmutableSet.of(VNONEH1, VNONEH2);
        Set<Interface> vlanFourSet = ImmutableSet.of(V400H1, VNONEH3);

        AVAILABLE_INTERFACES.forEach(intf -> {
            expect(interfaceService.getInterfacesByPort(intf.connectPoint()))
                    .andReturn(Sets.newHashSet(intf)).anyTimes();
        });
        expect(interfaceService.getInterfacesByVlan(VLAN100))
                .andReturn(vlanOneSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN200))
                .andReturn(vlanTwoSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN300))
                .andReturn(vlanThreeSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VLAN400))
                .andReturn(vlanFourSet).anyTimes();
        expect(interfaceService.getInterfacesByVlan(VlanId.NONE))
                .andReturn(vlanFourSet).anyTimes();
        expect(interfaceService.getInterfaces()).andReturn(interfaces).anyTimes();

        replay(interfaceService);
    }

    /**
     * Seven ports are configured with VLANs, while three ports are not. No hosts are
     * registered by the HostService.
     *
     * The first three ports have an interface configured on VPLS 1,
     * the other three on VPLS 2. Two ports are defined for VPLS 3, while
     * the two remaining ports are configured on VPLS 4.
     *
     * The number of intents expected is 10: three for VPLS 1, three for VPLS 2,
     * two for VPLS 3, two for VPLS 4. Eight MP2SP intents.
     * Checks if the number of intents submitted to the intent framework is
     * equal to the number of intents expected and if all intents are equivalent.
     */
    @Test
    public void activateNoHosts() {
        List<Intent> expectedIntents = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints;
        Set<Interface> interfaces;

        interfaces = ImmutableSet.of(V100H1, V200H1, V300H1);
        VplsData vplsData = createVplsData(VPLS1, VLAN, interfaces);
        Set<Intent> brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        brcIntents.forEach(intentService::submit);
        fcPoints = buildFCPoints(interfaces);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS1, VLAN));

        checkIntents(expectedIntents);

        interfaces = ImmutableSet.of(V100H2, V200H2, V300H2);
        vplsData = createVplsData(VPLS2, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        brcIntents.forEach(intentService::submit);
        fcPoints = buildFCPoints(interfaces);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS2, NONE));

        checkIntents(expectedIntents);

        interfaces = ImmutableSet.of(VNONEH1, VNONEH2);
        vplsData = createVplsData(VPLS3, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        brcIntents.forEach(intentService::submit);
        fcPoints = buildFCPoints(interfaces);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS3, NONE));

        checkIntents(expectedIntents);

        interfaces = ImmutableSet.of(V400H1, VNONEH3);
        vplsData = createVplsData(VPLS4, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        brcIntents.forEach(intentService::submit);
        fcPoints = buildFCPoints(interfaces);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS4, NONE));

        checkIntents(expectedIntents);
    }



    /**
     * Ten ports are configured with VLANs and ten hosts are registered by the
     * HostService.
     *
     * The first three ports have an interface configured on VPLS 1,
     * the other three on VPLS 2, two on VPLS3 and two on VPLS4.
     *
     * The number of intents expected is twenty: six
     * for VPLS 1, six for VPLS 2. four for VPLS 3, four for VPLS 4.
     * That is ten sp2mp intents, ten mp2sp intents. For VPLS 1
     * IPs are added to demonstrate this doesn't influence the number of intents
     * created. Checks if the number of intents submitted to the intent
     * framework is equal to the number of intents expected and if all intents
     * are equivalent.
     */
    @Test
    public void tenInterfacesConfiguredHostsPresent() {
        hostsAvailable.addAll(AVAILABLE_HOSTS);

        List<Intent> expectedIntents = Lists.newArrayList();
        Set<FilteredConnectPoint> fcPoints;
        Set<Host> hosts;
        Set<Interface> interfaces;
        VplsData vplsData;
        Set<Intent> brcIntents;
        Set<Intent> uniIntents;

        interfaces = ImmutableSet.of(V100H1, V200H1, V300H1);
        fcPoints = buildFCPoints(interfaces);
        hosts = ImmutableSet.of(V100HOST1, V200HOST1, V300HOST1);
        vplsData = createVplsData(VPLS1, VLAN, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        uniIntents = VplsIntentUtility.buildUniIntents(vplsData, hosts, APPID);
        brcIntents.forEach(intentService::submit);
        uniIntents.forEach(intentService::submit);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS1, VLAN));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS1, VLAN));

        interfaces = ImmutableSet.of(V100H2, V200H2, V300H2);
        fcPoints = buildFCPoints(interfaces);
        hosts = ImmutableSet.of(V100HOST2, V200HOST2, V300HOST2);
        vplsData = createVplsData(VPLS2, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        uniIntents = VplsIntentUtility.buildUniIntents(vplsData, hosts, APPID);
        brcIntents.forEach(intentService::submit);
        uniIntents.forEach(intentService::submit);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS2, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS2, NONE));

        interfaces = ImmutableSet.of(VNONEH1, VNONEH2);
        fcPoints = buildFCPoints(interfaces);
        hosts = ImmutableSet.of(VNONEHOST1, VNONEHOST2);
        vplsData = createVplsData(VPLS3, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        uniIntents = VplsIntentUtility.buildUniIntents(vplsData, hosts, APPID);
        brcIntents.forEach(intentService::submit);
        uniIntents.forEach(intentService::submit);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS3, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS3, NONE));

        interfaces = ImmutableSet.of(V400H1, VNONEH3);
        fcPoints = buildFCPoints(interfaces);
        hosts = ImmutableSet.of(V400HOST1, VNONEHOST3);
        vplsData = createVplsData(VPLS4, NONE, interfaces);
        brcIntents = VplsIntentUtility.buildBrcIntents(vplsData, APPID);
        uniIntents = VplsIntentUtility.buildUniIntents(vplsData, hosts, APPID);
        brcIntents.forEach(intentService::submit);
        uniIntents.forEach(intentService::submit);
        expectedIntents.addAll(generateVplsBrc(fcPoints, VPLS4, NONE));
        expectedIntents.addAll(generateVplsUni(fcPoints, hosts, VPLS4, NONE));

        checkIntents(expectedIntents);
    }

    /**
     * Generates a list of the expected sp2mp intents for a VPLS.
     *
     * @param fcPoints the filtered connect point
     * @param name the name of the VPLS
     * @param encap the encapsulation type
     * @return the list of expected sp2mp intents for the given VPLS
     */
    private List<SinglePointToMultiPointIntent>
    generateVplsBrc(Set<FilteredConnectPoint> fcPoints, String name, EncapsulationType encap) {
        List<SinglePointToMultiPointIntent> intents = Lists.newArrayList();

        fcPoints.forEach(point -> {
            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(point))
                            .collect(Collectors.toSet());

            Key brckey = buildKey(VplsIntentUtility.PREFIX_BROADCAST,
                                  point.connectPoint(),
                                  name,
                                  MacAddress.BROADCAST);

            intents.add(buildBrcIntent(brckey, point, otherPoints, encap));
        });

        return intents;
    }

    /**
     * Generates a list of expected mp2sp intents for a given VPLS.
     *
     * @param fcPoints the filtered connect point
     * @param hosts the hosts
     * @param name the name of the VPLS
     * @param encap the encapsulation type
     * @return the list of expected mp2sp intents for the given VPLS
     */
    private List<MultiPointToSinglePointIntent>
    generateVplsUni(Set<FilteredConnectPoint> fcPoints, Set<Host> hosts,
                    String name, EncapsulationType encap) {
        List<MultiPointToSinglePointIntent> intents = Lists.newArrayList();

        hosts.forEach(host -> {
            FilteredConnectPoint hostPoint = getHostPoint(host, fcPoints);

            Set<FilteredConnectPoint> otherPoints =
                    fcPoints.stream()
                            .filter(fcp -> !fcp.equals(hostPoint))
                            .collect(Collectors.toSet());

            Key uniKey = buildKey(VplsIntentUtility.PREFIX_UNICAST,
                                  host.location(), name, host.mac());

            intents.add(buildUniIntent(uniKey, otherPoints, hostPoint, host, encap));
        });

        return intents;
    }

    /**
     * Checks if the number of intents submitted to the intent framework is equal
     * to the number of intents expected and if all intents are equivalent.
     *
     * @param intents the list of intents expected
     */
    private void checkIntents(List<Intent> intents) {
        assertEquals("The number of intents submitted differs from the number" +
                             " of intents expected. ",
                     intents.size(), intentService.getIntentCount());
        for (Intent intentOne : intents) {
            boolean found = false;
            for (Intent intentTwo : intentService.getIntents()) {
                if (intentOne.key().equals(intentTwo.key())) {
                    found = true;
                    assertTrue(format("The intent submitted is different from" +
                                              " the intent expected. %s %s",
                                      intentOne, intentTwo),
                               IntentUtils.intentsAreEqual(intentOne, intentTwo));
                    break;
                }
            }
            assertTrue("The intent submitted is not equal to any of the expected" +
                               " intents. ", found);
        }
    }

    /**
     * Builds a broadcast intent.
     *
     * @param key the key to identify the intent
     * @param src the ingress connect point
     * @param dsts the egress connect points
     * @return the generated single-point to multi-point intent
     */
    private SinglePointToMultiPointIntent buildBrcIntent(Key key,
                                                         FilteredConnectPoint src,
                                                         Set<FilteredConnectPoint> dsts,
                                                         EncapsulationType encap) {
        SinglePointToMultiPointIntent.Builder intentBuilder;
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(MacAddress.BROADCAST)
                .build();
        intentBuilder = SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .filteredIngressPoint(src)
                .filteredEgressPoints(dsts)
                .constraints(VplsIntentUtility.PARTIAL_FAILURE_CONSTRAINT)
                .priority(PRIORITY_OFFSET);
        VplsIntentUtility.setEncap(intentBuilder,
                                   VplsIntentUtility.PARTIAL_FAILURE_CONSTRAINT,
                                   encap);

        return intentBuilder.build();
    }

    /**
     * Builds a unicast intent.
     *
     * @param key the key to identify the intent
     * @param srcs the ingress connect points
     * @param dst the egress connect point
     * @param host the destination Host
     * @return the generated multi-point to single-point intent
     */
    private MultiPointToSinglePointIntent buildUniIntent(Key key,
                                                         Set<FilteredConnectPoint> srcs,
                                                         FilteredConnectPoint dst,
                                                         Host host,
                                                         EncapsulationType encap) {
        MultiPointToSinglePointIntent.Builder intentBuilder;
        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchEthDst(host.mac())
                .build();
        intentBuilder = MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .key(key)
                .selector(selector)
                .filteredIngressPoints(srcs)
                .filteredEgressPoint(dst)
                .constraints(VplsIntentUtility.PARTIAL_FAILURE_CONSTRAINT)
                .priority(PRIORITY_OFFSET);
        VplsIntentUtility.setEncap(intentBuilder,
                                   VplsIntentUtility.PARTIAL_FAILURE_CONSTRAINT,
                                   encap);

        return intentBuilder.build();
    }

    /**
     * Returns the filtered connect point associated to a given host.
     *
     * @param host the target host
     * @param fcps the filtered connected points
     * @return the filtered connect point associated to the given host; null
     * otherwise
     */
    private FilteredConnectPoint getHostPoint(Host host,
                                              Set<FilteredConnectPoint> fcps) {
        return fcps.stream()
                .filter(fcp -> fcp.connectPoint().equals(host.location()))
                .filter(fcp -> {
                    VlanIdCriterion vlanCriterion =
                            (VlanIdCriterion) fcp.trafficSelector().
                                    getCriterion(Criterion.Type.VLAN_VID);
                    return vlanCriterion == null ||
                            vlanCriterion.vlanId().equals(host.vlan());
                })
                .findFirst()
                .orElse(null);
    }

    /**
     * Computes a set of filtered connect points from a list of given interfaces.
     *
     * @param interfaces the interfaces to compute
     * @return the set of filtered connect points
     */
    private Set<FilteredConnectPoint> buildFCPoints(Collection<Interface> interfaces) {
        // Build all filtered connected points in the VPLS
        return interfaces
                .stream()
                .map(intf -> {
                    TrafficSelector.Builder selectorBuilder =
                            DefaultTrafficSelector.builder();
                    if (!intf.vlan().equals(VlanId.NONE)) {
                        selectorBuilder.matchVlanId(intf.vlan());
                    }
                    return new FilteredConnectPoint(intf.connectPoint(),
                                                    selectorBuilder.build());
                })
                .collect(Collectors.toSet());
    }

    /**
     * Builds an intent Key either for a single-point to multi-point or
     * multi-point to single-point intent, based on a prefix that defines
     * the intent type, the connection point representing the source or the
     * destination and the VLAN Id representing the VPLS.
     *
     * @param prefix the key prefix
     * @param cPoint the ingress/egress connect point
     * @param vplsName the VPLS name
     * @param hostMac the ingress/egress MAC address
     * @return the key to identify the intent
     */
    private Key buildKey(String prefix,
                         ConnectPoint cPoint,
                         String vplsName,
                         MacAddress hostMac) {
        String keyString = vplsName +
                DASH +
                prefix +
                DASH +
                cPoint.deviceId() +
                DASH +
                cPoint.port() +
                DASH +
                hostMac;

        return Key.of(keyString, APPID);
    }

    /**
     * Creates VPLS data by given name, encapsulation type and network
     * interfaces.
     *
     * @param name the VPLS name
     * @param encap the encapsulation type
     * @param interfaces the network interfaces
     * @return the VPLS data
     */
    private VplsData createVplsData(String name, EncapsulationType encap,
                                    Set<Interface> interfaces) {
        VplsData vplsData = VplsData.of(name, encap);
        vplsData.addInterfaces(interfaces);
        return vplsData;
    }

    /**
     * Represents a fake IntentService class that allows to store and retrieve
     * intents without implementing the IntentService logic.
     */
    private class TestIntentService extends IntentServiceAdapter {
        private Map<Key, Intent> intents;

        public TestIntentService() {
            intents = Maps.newHashMap();
        }

        @Override
        public void submit(Intent intent) {
            intents.put(intent.key(), intent);
        }

        @Override
        public long getIntentCount() {
            return intents.size();
        }

        @Override
        public Iterable<Intent> getIntents() {
            return intents.values();
        }

        @Override
        public Intent getIntent(Key intentKey) {
            for (Intent intent : intents.values()) {
                if (intent.key().equals(intentKey)) {
                    return intent;
                }
            }
            return null;
        }
    }

}
