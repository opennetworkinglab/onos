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
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.criteria.MplsCriterion;
import org.onosproject.net.flow.criteria.VlanIdCriterion;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.resource.MockResourceService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.onlab.packet.EthType.EtherType.IPV4;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.domain.DomainId.LOCAL;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_LABEL;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;

/**
 * This set of tests are meant to test the encapsulation
 * in the LinkCollectionIntent.
 */
public class LinkCollectionOptimizationTest extends AbstractLinkCollectionTest {

    @Before
    public void setUp() {
        sut = new LinkCollectionIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;

        domainService = createMock(DomainService.class);
        expect(domainService.getDomain(anyObject(DeviceId.class))).andReturn(LOCAL).anyTimes();
        sut.domainService = domainService;

        super.setUp();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService.registerCompiler(LinkCollectionIntent.class, sut);
        intentExtensionService.unregisterCompiler(LinkCollectionIntent.class);

        registrator = new IntentConfigurableRegistrator();
        registrator.extensionService = intentExtensionService;
        registrator.cfgService = new ComponentConfigAdapter();
        registrator.activate();

        sut.registrator = registrator;
        sut.resourceService = new MockResourceService();

        /*
         * We activate the optimizations.
         */
        LinkCollectionCompiler.optimizeInstructions = true;
        LinkCollectionCompiler.copyTtl = true;

        replay(coreService, domainService, intentExtensionService);
    }

    /**
     * We test the proper optimization of sp2mp with dec tll
     * and dec mpls ttl.
     */
    @Test
    public void testDecTtlOptimization() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(decTllTreatment)
                .links(linksForSp2Mp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10),
                        new FilteredConnectPoint(d1p11),
                        new FilteredConnectPoint(d2p10)
                ))
                .applyTreatmentOnEgress(true)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p0.port())
                        .decMplsTtl()
                        .decNwTtl()
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .decMplsTtl()
                        .decNwTtl()
                        .setOutput(d1p10.port())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with VLAN
     * ingress point and different egress points: 1) it is
     * a simple ingress point; 2) it is a vlan ingress point;
     * 3) It is a simple ingress point. 1) and 2) share the same
     * egress switch. The outcomes of the test are the re-ordering
     * of the actions and the proper optimization of the chain.
     */
    @Test
    public void testVlanOrder() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(linksForSp2Mp)
                .applyTreatmentOnEgress(true)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10),
                        new FilteredConnectPoint(d1p11, vlan200Selector),
                        new FilteredConnectPoint(d2p10)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d2p1.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p0.port())
                        .popVlan()
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d1p0.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p11.port())
                        .popVlan()
                        .setOutput(d1p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with MPLS
     * ingress point and different egress points: 1) it is
     * a vlan ingress point; 2) it is a mpls ingress point;
     * 3) It is a simple ingress point. 1) and 2) share the same
     * egress switch. The outcomes of the test are the re-ordering
     * of the actions and the proper optimization of the chain.
     */
    @Test
    public void testMplsOrder() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(linksForSp2Mp)
                .applyTreatmentOnEgress(true)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls100Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector),
                        new FilteredConnectPoint(d1p11, mpls200Selector),
                        new FilteredConnectPoint(d2p10)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(mpls100Selector)
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder(mpls100Selector)
                        .matchInPort(d2p1.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p0.port())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(mpls100Selector)
                        .matchInPort(d1p0.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p11.port())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with untagged
     * ingress point and different egress points: 1) it is
     * a vlan ingress point; 2) it is a mpls ingress point;
     * 3) It is a simple ingress point. 1) and 2) share the same
     * egress switch. The outcomes of the test are the re-ordering
     * of the actions and the proper optimization of the chain.
     */
    @Test
    public void testUntaggedOrder() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(linksForSp2Mp)
                .applyTreatmentOnEgress(true)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector),
                        new FilteredConnectPoint(d1p11, mpls200Selector),
                        new FilteredConnectPoint(d1p12),
                        new FilteredConnectPoint(d2p10)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));


        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p0.port())
                        .setOutput(d2p10.port())
                        .build()
        ));


        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d1p12.port())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p10.port())
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with VLAN ingress point,
     * simple egress point, non trivial intent treatment and non trivial
     * intent selector. The outcomes of the test are the proper optimization
     * of the actions. It is expected that the actions related to the intent
     * treatment and intent selector are not optimized. The optimization is
     * performed only on the dec ttl actions and on the actions which create
     * traffic slices. The rationale is that in the intent treatment we expect
     * actions like set something so it is fine to perform them several times.
     */
    @Test
    public void testPoPVlanOptimization() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .links(linksForSp2Mp)
                .applyTreatmentOnEgress(true)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10),
                        new FilteredConnectPoint(d1p11),
                        new FilteredConnectPoint(d1p12),
                        new FilteredConnectPoint(d2p10)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d2p1.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p0.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .popVlan()
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d1p0.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .popVlan()
                        .setOutput(d1p12.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .setOutput(d1p10.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with the VLAN
     * encapsulation and trivial selector.
     */
    @Test
    public void testOptimizationMplsEncapTrivialSelectors() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraintsForMPLS)
                .links(linksForSp2Mp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10),
                        new FilteredConnectPoint(d1p11),
                        new FilteredConnectPoint(d2p10)
                ))
                .build();

        sut.activate();
        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .matchMplsLabel(MplsLabel.mplsLabel((LABEL)))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p0.port())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .matchMplsLabel(MplsLabel.mplsLabel((LABEL)))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .setOutput(d1p10.port())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with the VLAN
     * encapsulation, filtered selectors.
     */
    @Test
    public void testOptimizationVlanEncapMplsSelectors() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraintsForVlan)
                .links(linksForSp2Mp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls80Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, mpls100Selector),
                        new FilteredConnectPoint(d1p11, mpls200Selector),
                        new FilteredConnectPoint(d2p10, mpls69Selector)
                ))
                .build();

        sut.activate();
        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(mpls80Selector)
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d2p0.port())
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls69Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls100Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p10.port())
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with the MPLS
     * encapsulation, filtered selectors, intent selector, and
     * intent treatment.
     */
    @Test
    public void testOptimizationMplsEncapNonTrivial() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .constraints(constraintsForMPLS)
                .links(linksForSp2Mp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, vlan69Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector),
                        new FilteredConnectPoint(d1p11, vlan200Selector),
                        new FilteredConnectPoint(d2p10, vlan300Selector)
                ))
                .build();

        sut.activate();
        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d3p10.port())
                        .matchVlanId(((VlanIdCriterion) vlan69Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p0.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                           .allInstructions()
                                           .stream()
                                           .filter(instruction -> instruction instanceof ModEtherInstruction)
                                           .findFirst().get()).mac())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan300Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p10.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p11.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of sp2mp with the VLAN
     * encapsulation and filtered selectors of different type.
     */
    @Test
    public void testOptimizationVlanEncapDifferentSelectors() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraintsForVlan)
                .links(linksForSp2Mp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, vlan69Selector)))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, mpls100Selector),
                        new FilteredConnectPoint(d1p11, vlan200Selector),
                        new FilteredConnectPoint(d1p12),
                        new FilteredConnectPoint(d2p10, mpls200Selector)
                ))
                .build();

        sut.activate();
        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan69Selector)
                        .matchInPort(d3p10.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d3p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p1.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d2p0.port())
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p0.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p11.port())
                        .popVlan()
                        .setOutput(d1p12.port())
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls100Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with trivial selector,
     * trivial treatment, vlan encapsulation and co-located
     * ingress/egress points.
     */
    @Test
    public void testOptimizationCoLocatedPointsTrivialForSp() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(linksForSp2MpCoLoc)
                .constraints(constraintsForVlan)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p11),
                        new FilteredConnectPoint(d2p10),
                        new FilteredConnectPoint(d3p10)
                ))
                .build();

        sut.activate();

        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));

        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(selector)
                        .matchInPort(d1p10.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .pushVlan()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d1p0.port())
                        .popVlan()
                        .setOutput(d1p11.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));

        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p0.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d2p1.port())
                        .popVlan()
                        .setOutput(d2p10.port())
                        .build()
        ));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));

        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p0.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with trivial selector,
     * trivial treatment, mpls encapsulation and co-located
     * filtered ingress/egress points.
     */
    @Test
    public void testCoLocatedFilteredPointsTrivialForSp() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(linksForSp2MpCoLoc)
                .constraints(constraintsForMPLS)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p11, vlan200Selector),
                        new FilteredConnectPoint(d2p10, vlan300Selector),
                        new FilteredConnectPoint(d3p10, vlan69Selector)
                ))
                .build();

        sut.activate();

        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));

        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d1p10.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d1p0.port())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d1p11.port())
                        .build()
        ));


        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));

        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p0.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p1.port())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan300Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d2p10.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));

        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p0.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan69Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with trivial selector,
     * trivial treatment, vlan encapsulation and co-located
     * different filtered ingress/egress points.
     */
    @Test
    public void testCoLocatedDifferentFilteredPointsTrivialForSp() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(linksForSp2MpCoLoc)
                .constraints(constraintsForVlan)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p11, mpls100Selector),
                        new FilteredConnectPoint(d2p10, vlan200Selector),
                        new FilteredConnectPoint(d3p10, mpls200Selector)
                ))
                .build();

        sut.activate();

        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));

        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d1p10.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d1p0.port())
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls100Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p11.port())
                        .build()
        ));


        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));

        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p0.port())
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d2p1.port())
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d2p10.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));

        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchVlanId(VlanId.vlanId(LABEL))
                        .matchInPort(d3p0.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper optimization of sp2mp with selector,
     * treatment, mpls encapsulation and co-located
     * different filtered ingress/egress points.
     */
    @Test
    public void testCoLocatedDifferentFilteredPointsNonTrivialForSp() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .applyTreatmentOnEgress(true)
                .links(linksForSp2MpCoLoc)
                .constraints(constraintsForMPLS)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p11, mpls100Selector),
                        new FilteredConnectPoint(d2p10, vlan200Selector),
                        new FilteredConnectPoint(d3p10, mpls200Selector)
                ))
                .build();

        sut.activate();

        /*
         * We use the FIRST_FIT to simplify tests.
         */
        LinkCollectionCompiler.labelAllocator.setLabelSelection(LABEL_SELECTION);

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));

        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d1p10.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS1.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .copyTtlOut()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d1p0.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .setMpls(((MplsCriterion) mpls100Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d1p11.port())
                        .build()
        ));


        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));

        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p0.port())
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p1.port())
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .copyTtlIn()
                        .popMpls(IPV4.ethType())
                        .pushVlan()
                        .setVlanId(((VlanIdCriterion) vlan200Selector.getCriterion(VLAN_VID)).vlanId())
                        .setOutput(d2p10.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));

        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchInPort(d3p0.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

}
