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

package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.domain.DomainId;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.AbstractIntentTest;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onosproject.net.NetTestTools.*;

/**
 * Abstract class to hold the common variables and pieces
 * of code.
 */
abstract class AbstractLinkCollectionTest extends AbstractIntentTest {

    static final String LABEL_SELECTION = "FIRST_FIT";
    static final String LABEL = "1";

    final ApplicationId appId = new TestApplicationId("test");

    final DomainId domain = DomainId.domainId("d1");

    final DeviceId d2Id = DeviceId.deviceId("of:s2");
    final ConnectPoint d2p0 = connectPoint("s2", 0);
    final ConnectPoint d2p1 = connectPoint("s2", 1);
    final ConnectPoint d2p10 = connectPoint("s2", 10);

    final DeviceId d3Id = DeviceId.deviceId("of:s3");
    final ConnectPoint d3p0 = connectPoint("s3", 0);
    final ConnectPoint d3p1 = connectPoint("s3", 1);
    final ConnectPoint d3p10 = connectPoint("s3", 10);

    final DeviceId d4Id = DeviceId.deviceId("of:s4");
    final ConnectPoint d4p0 = connectPoint("s4", 0);
    final ConnectPoint d4p1 = connectPoint("s4", 1);
    final ConnectPoint d4p10 = connectPoint("s4", 10);

    final DeviceId of1Id = DeviceId.deviceId("of:of1");
    final DeviceId of2Id = DeviceId.deviceId("of:of2");
    final DeviceId of3Id = DeviceId.deviceId("of:of3");
    final DeviceId of4Id = DeviceId.deviceId("of:of4");

    final ConnectPoint of1p1 = connectPoint("of1", 1);
    final ConnectPoint of1p2 = connectPoint("of1", 2);
    final ConnectPoint of1p3 = connectPoint("of1", 3);
    final ConnectPoint of2p1 = connectPoint("of2", 1);
    final ConnectPoint of2p2 = connectPoint("of2", 2);
    final ConnectPoint of2p3 = connectPoint("of2", 3);
    final ConnectPoint of3p1 = connectPoint("of3", 1);
    final ConnectPoint of3p2 = connectPoint("of3", 2);
    final ConnectPoint of4p1 = connectPoint("of4", 1);
    final ConnectPoint of4p2 = connectPoint("of4", 2);

    final DeviceId d1Id = DeviceId.deviceId("of:s1");
    final ConnectPoint d1p0 = connectPoint("s1", 0);
    final ConnectPoint d1p1 = connectPoint("s1", 1);
    final ConnectPoint d1p10 = connectPoint("s1", 10);
    final ConnectPoint d1p11 = connectPoint("s1", 11);
    final ConnectPoint d1p12 = connectPoint("s1", 12);

    final Set<Link> links = ImmutableSet.of(
            link(d1p1, d2p0),
            link(d2p1, d3p1),
            link(d1p1, d3p1)
    );

    final Set<Link> linksForMp2Sp = ImmutableSet.of(
            link(d1p0, d2p0),
            link(d2p1, d3p0)
    );

    final Set<Link> linksForSp2Mp = ImmutableSet.of(
            link(d3p0, d2p1),
            link(d2p0, d1p0)
    );

    final Set<Link> p2pLinks = ImmutableSet.of(
            link(d1p0, d2p0),
            link(d2p1, d3p1)
    );

    final Set<Link> domainP2Plinks = ImmutableSet.of(
            link(d1p0, d2p0),
            link(d2p1, d4p1),
            link(d4p0, d3p0)
    );

    final Set<Link> linksForSp2MpCoLoc = ImmutableSet.of(
            link(d1p0, d2p0),
            link(d2p1, d3p0)
    );

    final Set<Link> linksForMp2SpCoLoc = ImmutableSet.of(
            link(d2p0, d1p0)
    );

    final TrafficTreatment treatment = emptyTreatment();
    final TrafficTreatment ethDstTreatment = macDstTreatment("C0:FF:EE:C0:FF:EE");
    final TrafficTreatment decTllTreatment = decTtlTreatment();

    final TrafficSelector selector = emptySelector();
    final TrafficSelector vlan69Selector = vlanSelector("69");
    final TrafficSelector vlan1Selector = vlanSelector("1");
    final TrafficSelector vlan100Selector = vlanSelector("100");
    final TrafficSelector vlan200Selector = vlanSelector("200");
    final TrafficSelector vlan300Selector = vlanSelector("300");
    final TrafficSelector mpls69Selector = mplsSelector("69");
    final TrafficSelector mpls80Selector = mplsSelector("80");
    final TrafficSelector mpls100Selector = mplsSelector("100");
    final TrafficSelector mpls200Selector = mplsSelector("200");
    final TrafficSelector ipPrefixSelector = ipPrefixDstSelector("192.168.100.0/24");
    final TrafficSelector ethDstSelector = ethDstSelector("C0:FF:EE:C0:FF:EE");
    final ResourceGroup resourceGroup1 = ResourceGroup.of(1L);
    final ResourceGroup resourceGroup2 = ResourceGroup.of(1L);

    final List<Constraint> constraintsForVlan = vlanConstraint();
    final List<Constraint> constraintsForMPLS = mplsConstraint();

    CoreService coreService;
    DomainService domainService;
    IntentExtensionService intentExtensionService;
    IntentConfigurableRegistrator registrator;

    LinkCollectionIntent intent;

    LinkCollectionIntentCompiler sut;

    List<FlowRule> getFlowRulesByDevice(DeviceId deviceId, Collection<FlowRule> flowRules) {
        return flowRules.stream()
                .filter(fr -> fr.deviceId().equals(deviceId))
                .collect(Collectors.toList());
    }

}
