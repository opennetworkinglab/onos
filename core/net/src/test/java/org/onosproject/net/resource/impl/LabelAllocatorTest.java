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

package org.onosproject.net.resource.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MplsLabel;
import org.onlab.packet.VlanId;
import org.onlab.util.Identifier;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.EncapsulationType;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.MockIdGenerator;
import org.onosproject.net.resource.MockResourceService;
import org.onosproject.net.resource.impl.LabelAllocator.FirstFitSelection;
import org.onosproject.net.resource.impl.LabelAllocator.LabelSelection;
import org.onosproject.net.resource.impl.LabelAllocator.RandomSelection;


import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.NetTestTools.PID;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for LabelAllocator.
 */
public class LabelAllocatorTest {

    private LabelAllocator allocator;
    private MockResourceService resourceService;
    private IdGenerator idGenerator = MockIdGenerator.INSTANCE;

    private final ConnectPoint d1p0 = connectPoint("s1", 0);
    private final ConnectPoint d1p1 = connectPoint("s1", 1);
    private final ConnectPoint d2p0 = connectPoint("s2", 0);
    private final ConnectPoint d2p1 = connectPoint("s2", 1);
    private final ConnectPoint d3p0 = connectPoint("s3", 0);
    private final ConnectPoint d3p1 = connectPoint("s3", 1);
    private final ConnectPoint d4p0 = connectPoint("s4", 0);
    private final ConnectPoint d4p1 = connectPoint("s4", 1);

    private final List<Link> links = Arrays.asList(
            createEdgeLink(d1p0, true),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d3p1).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d3p0).dst(d2p1).type(DIRECT).build(),
            createEdgeLink(d2p0, false)
    );

    private final List<Link> links2 = Arrays.asList(
            createEdgeLink(d1p0, true),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d3p1).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d3p0).dst(d4p1).type(DIRECT).build(),
            DefaultLink.builder().providerId(PID).src(d4p0).dst(d2p1).type(DIRECT).build(),
            createEdgeLink(d2p0, false)
    );

    // Selection behavior
    private final String firstFit = "FIRST_FIT";
    private final String random = "RANDOM";
    private final String wrong = "BLAHBLAHBLAH";

    // Optimization behavior
    private final String none = "NONE";
    private final String noswap = "NO_SWAP";
    private final String minswap = "MIN_SWAP";

    @Before
    public void setUp() {
        this.resourceService = new MockResourceService();
        this.allocator = new LabelAllocator(this.resourceService);
    }

    @After
    public void tearDown() {

    }

    /**
     * To test changes to the selection behavior.
     */
    @Test
    public void testChangeSelBehavior() {
        // It has to be an instance of LabelSelection
        assertThat(this.allocator.getLabelSelection(), instanceOf(LabelSelection.class));
        // By default we have Random Selection
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // We change to Random and we test the change
        this.allocator.setLabelSelection(random);
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // We put a wrong type and we should have a Random selection
        this.allocator.setLabelSelection(wrong);
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // We change to first_fit and we test the change
        this.allocator.setLabelSelection("first_fit");
        // The change does not happen
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        this.allocator.setLabelSelection(firstFit);
        // We change to Random and we test the change
        this.allocator.setLabelSelection("random");
        // The change does not happen
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
    }

    /**
     * To test changes to the optimization behavior.
     */
    @Test
    public void testChangeOptBehavior() {
        // It has to be an instance of NONE
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // Change to MIN_SWAP
        this.allocator.setOptLabelSelection(minswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        // Change to NO_SWAP
        this.allocator.setOptLabelSelection(noswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
        // Change to NONE
        this.allocator.setOptLabelSelection(none);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // Change to MIN_SWAP
        this.allocator.setOptLabelSelection("miN_swap");
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        this.allocator.setOptLabelSelection(minswap);
        // Change to NO_SWAP
        this.allocator.setOptLabelSelection("No_swap");
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        this.allocator.setOptLabelSelection(noswap);
        // Change to NONE
        this.allocator.setOptLabelSelection("none");
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
    }

    /**
     * To test the first fit behavior. Using NONE optimization
     */
    @Test
    public void testFirstFitBehaviorNone() {
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // It has to be an instance of NONE
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 1,
                (short) 20,
                (short) 100
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be a Vlan Id
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // We test the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
    }

    /**
     * To test the first fit behavior. Using NO_SWAP optimization
     */
    @Test
    public void testFirstFitBehaviorNoSwap() {
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        /// Change to NO_SWAP
        this.allocator.setOptLabelSelection(noswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                1,
                100,
                1000
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be a MPLS label
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        // We test the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
    }

    /**
     * To test the first fit behavior. Using MIN_SWAP optimization
     */
    @Test
    public void testFirstFitBehaviorMinSwap() {
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        /// Change to MIN_SWAP
        this.allocator.setOptLabelSelection(minswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 2,
                (short) 20,
                (short) 200
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links2.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d4p1));
        // value has to be a VLAN id
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // We test the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links2.subList(1, 4)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        id = allocation.get(LinkKey.linkKey(d3p0, d4p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        id = allocation.get(LinkKey.linkKey(d4p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
    }

    /**
     * To test random behavior. Using NONE optimization
     */
    @Test
    public void testRandomBehaviorNone() {
        // By default Random is the selection behavior used
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // It has to be an instance of NONE
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                1,
                2,
                3,
                4,
                5,
                6
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be a MPLS label
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        // We test the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
    }

    /**
     * To test random behavior. Using NO_SWAP optimization
     */
    @Test
    public void testRandomBehaviorNoSwap() {
        // By default Random is the selection behavior used
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // Change to NO_SWAP
        this.allocator.setOptLabelSelection(noswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 1,
                (short) 2,
                (short) 3,
                (short) 4,
                (short) 5,
                (short) 6
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be a VLAN Id
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // We test the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
    }

    /**
     * To test the random behavior. Using MIN_SWAP optimization
     */
    @Test
    public void testRandomBehaviorMinSwap() {
        // By default Random is the selection behavior used
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // Change to MIN_SWAP
        this.allocator.setOptLabelSelection(minswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        // Filter reservations
        this.resourceService.filterAssignment = true;
        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8
        );
        // First allocation on a subset of links
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links2.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d4p1));
        // value has to be a MPLS label
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        // We test the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links2.subList(1, 4)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        id = allocation.get(LinkKey.linkKey(d3p0, d4p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        id = allocation.get(LinkKey.linkKey(d4p0, d2p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
    }


    /**
     * To test the port key based API.
     */
    @Test
    public void testPortKey() {
        // Verify the first fit behavior
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // We test the behavior for VLAN
        Map<ConnectPoint, Identifier<?>> allocation = this.allocator.assignLabelToPorts(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(new ConnectPoint(d1p1.elementId(), d1p1.port()));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId prevVlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < prevVlanId.toShort() && prevVlanId.toShort() < VlanId.MAX_VLAN);
        // value has to be 1
        assertEquals(1, prevVlanId.toShort());
        // verify same applies for d2p1
        id = allocation.get(new ConnectPoint(d3p1.elementId(), d3p1.port()));
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // value has to be 1
        assertEquals(prevVlanId, vlanId);
    }

    /**
     * To test the developed algorithms when there are no labels.
     */
    @Test
    public void noLabelsTest() {
        // Verify the first fit behavior with NONE optimization
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // It has to be an instance of NONE
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 10
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for VLAN
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // Next hop
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);

        // Verify the random behavior with NONE_SWAP optimization
        this.allocator.setLabelSelection(random);
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // Change to NO_SWAP
        this.allocator.setOptLabelSelection(noswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                2000
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be a Mplslabel
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 <= mplsLabel.toInt() && mplsLabel.toInt() <= MplsLabel.MAX_MPLS);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(MplsLabel.class));
        mplsLabel = (MplsLabel) id;
        assertTrue(0 <= mplsLabel.toInt() && mplsLabel.toInt() <= MplsLabel.MAX_MPLS);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);

        // Verify the first fit behavior with MIN optimization
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // Change to MIN_SWAP
        this.allocator.setOptLabelSelection(minswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 11
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // Next hop
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);
    }

    /**
     * To test the developed algorithms when there are no labels on a specific link.
     */
    @Test
    public void noLabelsOnLinkTest() {
        // Verify the first fit behavior with NONE optimization
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // It has to be an instance of NONE
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NONE);
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 10
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for VLAN
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        VlanId label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);

        // Verify the random behavior with NONE_SWAP optimization
        this.allocator.setLabelSelection(random);
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // Change to NO_SWAP
        this.allocator.setOptLabelSelection(noswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.NO_SWAP);
        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                2000
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(MplsLabel.class));
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 <= mplsLabel.toInt() && mplsLabel.toInt() <= MplsLabel.MAX_MPLS);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);

        // Verify the first fit behavior with MIN optimization
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // Change to MIN_SWAP
        this.allocator.setOptLabelSelection(minswap);
        assertEquals(this.allocator.getOptLabelSelection(), LabelAllocator.OptimizationBehavior.MIN_SWAP);
        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 11
        );
        // Enable filtering of the reservation
        this.resourceService.filterAssignment = true;
        // We test the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(2, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        assertThat(id, instanceOf(VlanId.class));
        label = (VlanId) id;
        assertTrue(VlanId.NO_VID < label.toShort() && label.toShort() < VlanId.MAX_VLAN);
        // No labels are available, reservation is not possible
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 3)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d3p1));
        // value has to be null
        assertNull(id);
        id = allocation.get(LinkKey.linkKey(d3p0, d2p1));
        // value has to be null
        assertNull(id);
    }

}
