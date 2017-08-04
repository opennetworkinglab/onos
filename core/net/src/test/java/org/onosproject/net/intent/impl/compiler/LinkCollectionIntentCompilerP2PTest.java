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

package org.onosproject.net.intent.impl.compiler;

import com.google.common.collect.ImmutableSet;
import org.hamcrest.core.Is;
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
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.domain.DomainId.LOCAL;
import static org.onosproject.net.flow.criteria.Criterion.Type.MPLS_LABEL;
import static org.onosproject.net.flow.criteria.Criterion.Type.VLAN_VID;
import static org.onosproject.net.flow.instructions.L2ModificationInstruction.ModEtherInstruction;

/**
 * This set of tests are meant to test the proper compilation
 * of p2p intents.
 */
public class LinkCollectionIntentCompilerP2PTest extends AbstractLinkCollectionTest {

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

        LinkCollectionCompiler.optimizeInstructions = false;
        LinkCollectionCompiler.copyTtl = false;

        replay(coreService, domainService, intentExtensionService);
    }

    /**
     * We test the proper compilation of p2p with
     * trivial selector and trivial treatment.
     */
    @Test
    public void testCompilationTrivialForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(p2pLinks)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d3p0)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), Is.is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d1p10.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d1p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), Is.is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d2p0.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p1.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), Is.is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p1.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d3p0.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of p2p with
     * trivial selector, trivial treatment and
     * filtered points.
     */
    @Test
    public void testCompilationFilteredPointForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(p2pLinks)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d3p0, mpls200Selector)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d1p10.port())
                        .build()
        ));
        assertThat(ruleS1.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d1p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d2p0.port())
                        .build()
        ));
        assertThat(ruleS2.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p1.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(vlan100Selector)
                        .matchInPort(d3p1.port())
                        .build()
        ));
        assertThat(ruleS3.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .popVlan()
                        .pushMpls()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p0.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of p2p with
     * selector, treatment and filtered points.
     */
    @Test
    public void testCompilationNonTrivialForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .applyTreatmentOnEgress(true)
                .links(p2pLinks)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10, vlan100Selector)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d3p0, mpls200Selector)
                ))
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        Collection<FlowRule> rules = ((FlowRuleIntent) compiled.get(0)).flowRules();
        assertThat(rules, hasSize(3));

        Collection<FlowRule> rulesS1 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d1p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS1, hasSize(1));
        FlowRule ruleS1 = rulesS1.iterator().next();
        assertThat(ruleS1.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d1p10.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS1.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d1p0.port())
                        .build()
        ));

        Collection<FlowRule> rulesS2 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d2p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS2, hasSize(1));
        FlowRule ruleS2 = rulesS2.iterator().next();
        assertThat(ruleS2.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d2p0.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS2.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setOutput(d2p1.port())
                        .build()
        ));


        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p1.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), Is.is(
                DefaultTrafficSelector
                        .builder(ipPrefixSelector)
                        .matchInPort(d3p1.port())
                        .matchVlanId(((VlanIdCriterion) vlan100Selector.getCriterion(VLAN_VID)).vlanId())
                        .build()
        ));
        assertThat(ruleS3.treatment(), Is.is(
                DefaultTrafficTreatment
                        .builder()
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .popVlan()
                        .pushMpls()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p0.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of p2p with the VLAN
     * encapsulation and trivial filtered points.
     */
    @Test
    public void testVlanEncapsulationForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraintsForVlan)
                .links(linksForMp2Sp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d1p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls200Selector)))
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
                        .build()
        ));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
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
                        .pushMpls()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }


    /**
     * We test the proper compilation of p2p with the MPLS
     * encapsulation and trivial filtered points.
     */
    @Test
    public void testMplsEncapsulationForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .constraints(constraintsForMPLS)
                .links(linksForMp2Sp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d1p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls200Selector)))
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
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d1p0.port())
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
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p1.port())
                        .build()
        ));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p0.port())
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS3.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(((MplsCriterion) mpls200Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of p2p with the VLAN
     * encapsulation, filtered points, selector and treatment.
     */
    @Test
    public void testVlanEncapsulationNonTrivialForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .constraints(constraintsForVlan)
                .links(linksForMp2Sp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d1p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls69Selector)))
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
                        .setVlanId(VlanId.vlanId(LABEL))
                        .setOutput(d1p0.port())
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
                        .build()
        ));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
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
                        .setEthDst(((ModEtherInstruction) ethDstTreatment
                                .allInstructions()
                                .stream()
                                .filter(instruction -> instruction instanceof ModEtherInstruction)
                                .findFirst().get()).mac())
                        .popVlan()
                        .pushMpls()
                        .setMpls(((MplsCriterion) mpls69Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of p2p with the MPLS
     * encapsulation, filtered points, selector and treatment.
     */
    @Test
    public void testMplsEncapsulationNonTrivialForP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(ipPrefixSelector)
                .treatment(ethDstTreatment)
                .constraints(constraintsForMPLS)
                .links(linksForMp2Sp)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(d1p10, vlan100Selector)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(d3p10, mpls69Selector)))
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
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d1p0.port())
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
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
                        .build()
        ));
        assertThat(ruleS2.treatment(), is(
                DefaultTrafficTreatment
                        .builder()
                        .setMpls(MplsLabel.mplsLabel(LABEL))
                        .setOutput(d2p1.port())
                        .build()
        ));

        Collection<FlowRule> rulesS3 = rules.stream()
                .filter(rule -> rule.deviceId().equals(d3p0.deviceId()))
                .collect(Collectors.toSet());
        assertThat(rulesS3, hasSize(1));
        FlowRule ruleS3 = rulesS3.iterator().next();
        assertThat(ruleS3.selector(), is(
                DefaultTrafficSelector
                        .builder()
                        .matchInPort(d3p0.port())
                        .matchMplsLabel(MplsLabel.mplsLabel(LABEL))
                        .matchEthType(Ethernet.MPLS_UNICAST)
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
                        .setMpls(((MplsCriterion) mpls69Selector.getCriterion(MPLS_LABEL)).label())
                        .setOutput(d3p10.port())
                        .build()
        ));

        sut.deactivate();

    }

}
