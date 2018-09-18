/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent;

import java.time.Duration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.onosproject.net.intent.constraint.LatencyConstraint;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.link;

/**
 * Unit tests for the LinkCollectionIntent class.
 */
public class LinkCollectionIntentTest extends IntentTest {

    final ConnectPoint ingress = NetTestTools.connectPoint("ingress", 2);
    final ConnectPoint egress = NetTestTools.connectPoint("egress", 3);
    final TrafficSelector selector = new IntentTestsMocks.MockSelector();
    final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    final FilteredConnectPoint filteredIngress = new FilteredConnectPoint(ingress);
    final FilteredConnectPoint filteredEgress = new FilteredConnectPoint(egress);
    final ResourceGroup resourceGroup = ResourceGroup.of(0L);

    /**
     * Checks that the LinkCollectionIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(LinkCollectionIntent.class);
    }

    /**
     * Tests equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {

        final HashSet<Link> links1 = new HashSet<>();
        links1.add(link("src", 1, "dst", 2));
        final LinkCollectionIntent collectionIntent1 =
                LinkCollectionIntent.builder()
                        .appId(APP_ID)
                        .selector(selector)
                        .treatment(treatment)
                        .links(links1)
                        .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                        .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                        .resourceGroup(resourceGroup)
                        .build();

        final HashSet<Link> links2 = new HashSet<>();
        links2.add(link("src", 1, "dst", 3));
        final LinkCollectionIntent collectionIntent2 =
                LinkCollectionIntent.builder()
                        .appId(APP_ID)
                        .selector(selector)
                        .treatment(treatment)
                        .links(links2)
                        .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                        .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                        .resourceGroup(resourceGroup)
                        .build();

        new EqualsTester()
                .addEqualityGroup(collectionIntent1)
                .addEqualityGroup(collectionIntent2)
                .testEquals();
    }

    /**
     * Tests constructor without constraints.
     */
    @Test
    public void testConstructor() {
        final HashSet<Link> links1 = new HashSet<>();
        links1.add(link("src", 1, "dst", 2));
        final LinkCollectionIntent collectionIntent =
                LinkCollectionIntent.builder()
                        .appId(APP_ID)
                        .selector(selector)
                        .treatment(treatment)
                        .links(links1)
                        .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                        .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                        .resourceGroup(resourceGroup)
                        .build();

        final Set<Link> createdLinks = collectionIntent.links();
        assertThat(createdLinks, hasSize(1));
        assertThat(collectionIntent.isInstallable(), is(false));
        assertThat(collectionIntent.treatment(), is(treatment));
        assertThat(collectionIntent.selector(), is(selector));
        assertThat(collectionIntent.ingressPoints(), is(ImmutableSet.of(ingress)));
        assertThat(collectionIntent.egressPoints(), is(ImmutableSet.of(egress)));
        assertThat(collectionIntent.resourceGroup(), is(resourceGroup));
        assertThat(collectionIntent.resources(), hasSize(1));
        final List<Constraint> createdConstraints = collectionIntent.constraints();
        assertThat(createdConstraints, hasSize(0));
    }

    /**
     * Tests constructor with constraints.
     */
    @Test
    public void testConstructorWithConstraints() {
        final HashSet<Link> links1 = new HashSet<>();
        final LinkedList<Constraint> constraints = new LinkedList<>();

        links1.add(link("src", 1, "dst", 2));
        constraints.add(new LatencyConstraint(Duration.ofMillis(100)));
        final LinkCollectionIntent collectionIntent =
                LinkCollectionIntent.builder()
                        .appId(APP_ID)
                        .selector(selector)
                        .treatment(treatment)
                        .links(links1)
                        .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                        .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                        .constraints(constraints)
                        .priority(8888)
                        .build();

        final Set<Link> createdLinks = collectionIntent.links();
        assertThat(createdLinks, hasSize(1));
        assertThat(collectionIntent.isInstallable(), is(false));
        assertThat(collectionIntent.treatment(), is(treatment));
        assertThat(collectionIntent.selector(), is(selector));
        assertThat(collectionIntent.ingressPoints(), is(ImmutableSet.of(ingress)));
        assertThat(collectionIntent.egressPoints(), is(ImmutableSet.of(egress)));

        final List<Constraint> createdConstraints = collectionIntent.constraints();
        assertThat(createdConstraints, hasSize(1));
        assertThat(createdConstraints.get(0), instanceOf(LatencyConstraint.class));
    }

    /**
     * Tests constructor with constraints.
     */
    @Test
    public void testSerializerConstructor() {

        final LinkCollectionIntent collectionIntent =
                new LinkCollectionIntent();

        final Set<Link> createdLinks = collectionIntent.links();
        assertThat(createdLinks, nullValue());
        assertThat(collectionIntent.isInstallable(), is(false));
        assertThat(collectionIntent.treatment(), nullValue());
        assertThat(collectionIntent.selector(), nullValue());
        assertThat(collectionIntent.ingressPoints(), nullValue());
        assertThat(collectionIntent.egressPoints(), nullValue());

        final List<Constraint> createdConstraints = collectionIntent.constraints();
        assertThat(createdConstraints, hasSize(0));
    }

    /**
     * Test filtered connection point for LinkCollection intent.
     */
    @Test
    public void testFilteredConnectedPoint() {
        LinkCollectionIntent intent = createFilteredOne();
        Set<Link> links = Sets.newHashSet();
        links.add(link("A", 1, "B", 1));
        links.add(link("A", 2, "C", 1));

        assertThat(intent.appId(), is(APP_ID));
        assertThat(intent.treatment(), is(treatment));
        assertThat(intent.links(), is(links));
        assertThat(intent.applyTreatmentOnEgress(), is(false));
        assertThat(intent.filteredIngressPoints(), is(ImmutableSet.of(filteredIngress)));
        assertThat(intent.filteredEgressPoints(), is(ImmutableSet.of(filteredEgress)));

        intent = createAnotherFiltered();
        links = Sets.newHashSet();
        links.add(link("A", 1, "B", 1));
        links.add(link("A", 2, "C", 1));
        links.add(link("B", 2, "D", 1));
        links.add(link("B", 3, "E", 1));

        assertThat(intent.appId(), is(APP_ID));
        assertThat(intent.treatment(), is(treatment));
        assertThat(intent.links(), is(links));
        assertThat(intent.applyTreatmentOnEgress(), is(true));
        assertThat(intent.filteredIngressPoints(), is(ImmutableSet.of(filteredIngress)));
        assertThat(intent.filteredEgressPoints(), is(ImmutableSet.of(filteredEgress)));

    }

    @Override
    protected Intent createOne() {
        HashSet<Link> links1 = new HashSet<>();
        links1.add(link("src", 1, "dst", 2));
        return LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(links1)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                .build();
    }

    @Override
    protected Intent createAnother() {
        HashSet<Link> links2 = new HashSet<>();
        links2.add(link("src", 1, "dst", 3));
        return LinkCollectionIntent.builder()
                .appId(APP_ID)
                .selector(selector)
                .treatment(treatment)
                .links(links2)
                .filteredIngressPoints(ImmutableSet.of(new FilteredConnectPoint(ingress)))
                .filteredEgressPoints(ImmutableSet.of(new FilteredConnectPoint(egress)))
                .build();
    }

    protected LinkCollectionIntent createFilteredOne() {
        Set<Link> links = Sets.newHashSet();
        links.add(link("A", 1, "B", 1));
        links.add(link("A", 2, "C", 1));
        return LinkCollectionIntent.builder()
                .appId(APP_ID)
                .treatment(treatment)
                .links(links)
                .filteredIngressPoints(ImmutableSet.of(filteredIngress))
                .filteredEgressPoints(ImmutableSet.of(filteredEgress))
                .build();
    }

    protected LinkCollectionIntent createAnotherFiltered() {
        Set<Link> links = Sets.newHashSet();
        links.add(link("A", 1, "B", 1));
        links.add(link("A", 2, "C", 1));
        links.add(link("B", 2, "D", 1));
        links.add(link("B", 3, "E", 1));
        return LinkCollectionIntent.builder()
                .appId(APP_ID)
                .treatment(treatment)
                .links(links)
                .applyTreatmentOnEgress(true)
                .filteredIngressPoints(ImmutableSet.of(filteredIngress))
                .filteredEgressPoints(ImmutableSet.of(filteredEgress))
                .build();
    }
}
