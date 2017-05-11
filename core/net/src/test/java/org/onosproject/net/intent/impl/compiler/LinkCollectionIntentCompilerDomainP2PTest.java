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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.CoreService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.domain.DomainPointToPointIntent;
import org.onosproject.net.domain.DomainService;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentExtensionService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.constraint.DomainConstraint;
import org.onosproject.net.resource.MockResourceService;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.link;
import static org.onosproject.net.domain.DomainId.LOCAL;

/**
 * Those tests verify the compilation process with domains included in the path.
 */
public class LinkCollectionIntentCompilerDomainP2PTest extends AbstractLinkCollectionTest {

    private static List<Constraint> domainConstraint =
            ImmutableList.of(DomainConstraint.domain());

    @Before
    public void setUp() {
        sut = new LinkCollectionIntentCompiler();
        coreService = createMock(CoreService.class);
        expect(coreService.registerApplication("org.onosproject.net.intent"))
                .andReturn(appId);
        sut.coreService = coreService;

        // defining the domain assignments
        domainService = createMock(DomainService.class);
        expect(domainService.getDomain(d1Id)).andReturn(LOCAL).anyTimes();
        expect(domainService.getDomain(d2Id)).andReturn(domain).anyTimes();
        expect(domainService.getDomain(d4Id)).andReturn(domain).anyTimes();
        expect(domainService.getDomain(d3Id)).andReturn(LOCAL).anyTimes();
        sut.domainService = domainService;

        super.setUp();

        intentExtensionService = createMock(IntentExtensionService.class);
        intentExtensionService
                .registerCompiler(LinkCollectionIntent.class, sut);
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
     * We test the proper compilation of one domain device.
     */
    @Test
    public void testCompilationSingleDeviceDomainP2P() {

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
                        new FilteredConnectPoint(d3p10)
                ))
                .constraints(domainConstraint)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(2));

        DomainPointToPointIntent domainIntent =
                ((DomainPointToPointIntent) compiled.get(0));
        ConnectPoint ingress =
                domainIntent.filteredIngressPoints().iterator().next()
                        .connectPoint();
        assertThat(ingress, equalTo(d2p0));
        ConnectPoint egress =
                domainIntent.filteredEgressPoints().iterator().next()
                        .connectPoint();
        assertThat(egress, equalTo(d2p1));
        assertThat(domainIntent.links(), hasSize(0));

        Collection<FlowRule> rules =
                ((FlowRuleIntent) compiled.get(1)).flowRules();
        assertThat(rules, hasSize(2));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of a domain with two devices.
     */
    @Test
    public void testCompilationMultiHopDomainP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(domainP2Plinks)
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d3p10)
                ))
                .constraints(domainConstraint)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(2));

        DomainPointToPointIntent domainIntent =
                ((DomainPointToPointIntent) compiled.get(0));
        ConnectPoint ingress =
                domainIntent.filteredIngressPoints().iterator().next()
                        .connectPoint();
        assertThat(ingress, equalTo(d2p0));
        ConnectPoint egress =
                domainIntent.filteredEgressPoints().iterator().next()
                        .connectPoint();
        assertThat(egress, equalTo(d4p0));
        assertThat(domainIntent.links(), hasSize(1));

        Collection<FlowRule> rules =
                ((FlowRuleIntent) compiled.get(1)).flowRules();
        assertThat(rules, hasSize(2));

        sut.deactivate();

    }


    /**
     * We test the proper compilation of a domain starting with a domain device.
     */
    @Test
    public void testCompilationDomainStartP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(ImmutableSet.of(
                        link(d2p0, d1p0)
                ))
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d2p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10)
                ))
                .constraints(domainConstraint)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(2));

        DomainPointToPointIntent domainIntent =
                ((DomainPointToPointIntent) compiled.get(0));
        ConnectPoint ingress =
                domainIntent.filteredIngressPoints().iterator().next()
                        .connectPoint();
        assertThat(ingress, equalTo(d2p10));
        ConnectPoint egress =
                domainIntent.filteredEgressPoints().iterator().next()
                        .connectPoint();
        assertThat(egress, equalTo(d2p0));
        assertThat(domainIntent.links(), hasSize(0));

        Collection<FlowRule> rules =
                ((FlowRuleIntent) compiled.get(1)).flowRules();
        assertThat(rules, hasSize(1));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of a domain ending with a domain device.
     */
    @Test
    public void testCompilationDomainEndP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(ImmutableSet.of(
                        link(d1p0, d2p0)
                ))
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d1p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d2p10)
                ))
                .constraints(domainConstraint)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(2));

        DomainPointToPointIntent domainIntent =
                ((DomainPointToPointIntent) compiled.get(0));
        ConnectPoint ingress =
                domainIntent.filteredIngressPoints().iterator().next()
                        .connectPoint();
        assertThat(ingress, equalTo(d2p0));
        ConnectPoint egress =
                domainIntent.filteredEgressPoints().iterator().next()
                        .connectPoint();
        assertThat(egress, equalTo(d2p10));
        assertThat(domainIntent.links(), hasSize(0));

        Collection<FlowRule> rules =
                ((FlowRuleIntent) compiled.get(1)).flowRules();
        assertThat(rules, hasSize(1));

        sut.deactivate();

    }

    /**
     * We test the proper compilation of a path fully inside of a domain.
     */
    @Test
    public void testCompilationDomainFullP2P() {

        intent = LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .applyTreatmentOnEgress(true)
                .links(ImmutableSet.of(
                        link(d2p0, d4p0)
                ))
                .filteredIngressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d2p10)
                ))
                .filteredEgressPoints(ImmutableSet.of(
                        new FilteredConnectPoint(d4p10)
                ))
                .constraints(domainConstraint)
                .build();

        sut.activate();

        List<Intent> compiled = sut.compile(intent, Collections.emptyList());
        assertThat(compiled, hasSize(1));

        DomainPointToPointIntent domainIntent =
                ((DomainPointToPointIntent) compiled.get(0));
        ConnectPoint ingress =
                domainIntent.filteredIngressPoints().iterator().next()
                        .connectPoint();
        assertThat(ingress, equalTo(d2p10));
        ConnectPoint egress =
                domainIntent.filteredEgressPoints().iterator().next()
                        .connectPoint();
        assertThat(egress, equalTo(d4p10));
        assertThat(domainIntent.links(), hasSize(1));

        sut.deactivate();

    }

}
