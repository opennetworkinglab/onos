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

    private final List<Link> links = Arrays.asList(
            createEdgeLink(d1p0, true),
            DefaultLink.builder().providerId(PID).src(d1p1).dst(d2p1).type(DIRECT).build(),
            createEdgeLink(d2p0, false)
    );

    private final String firstFit = "FIRST_FIT";
    private final String random = "RANDOM";
    private final String wrong = "BLAHBLAHBLAH";

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
    public void testChangeBehavior() {
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
    }

    /**
     * To test the first fit behavior with VLAN Id. In the First step
     * we use the default set, for the first selection the selected label
     * has to be 1. In the Second step we change the default set and for
     * the first fit selection the selected has to be 2.
     */
    @Test
    public void testFirstFitBehaviorVlan() {
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // We test the behavior for VLAN
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // value will be always 1
        assertEquals(1, vlanId.toShort());

        // We change the available Ids
        this.resourceService.availableVlanLabels = ImmutableSet.of(
                (short) 100,
                (short) 11,
                (short) 20,
                (short) 2,
                (short) 3
        );
        // We test again the behavior for VLAN
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // value will be always 2
        assertEquals(2, vlanId.toShort());
    }

    /**
     * To test the first fit behavior with MPLS label. In the First step
     * we use the default set, for the first selection the selected label
     * has to be 1. In the Second step we change the default set and for
     * the first fit selection the selected has to be 100.
     */
    @Test
    public void testFirstFitBehaviorMpls() {
        // We change to FirstFit and we test the change
        this.allocator.setLabelSelection(firstFit);
        assertThat(this.allocator.getLabelSelection(), instanceOf(FirstFitSelection.class));
        // We test the behavior for MPLS
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a Mplslabel
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        // value will be always 1
        assertEquals(1, mplsLabel.toInt());

        // We change the available Ids
        this.resourceService.availableMplsLabels = ImmutableSet.of(
                100,
                200,
                1000
        );
        // We test again the behavior for MPLS
        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a Mplslabel
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        mplsLabel = (MplsLabel) id;
        assertTrue(0 < mplsLabel.toInt() && mplsLabel.toInt() < MplsLabel.MAX_MPLS);
        // value will be always 100
        assertEquals(100, mplsLabel.toInt());
    }

    /**
     * To test the random behavior with VLAN Id. We make two selection,
     * we test that these two selection are different.
     */
    @Test
    public void testRandomBehaviorVlan() {
        // Verify the random behavior
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // We test the behavior for VLAN
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        Short value = Short.parseShort(id.toString());
        VlanId prevVlanId = VlanId.vlanId(value);
        assertTrue(VlanId.NO_VID <= prevVlanId.toShort() && prevVlanId.toShort() <= VlanId.MAX_VLAN);

        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.VLAN);
         id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a VlanId
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID <= vlanId.toShort() && vlanId.toShort() <= VlanId.MAX_VLAN);

    }

    /**
     * To test random behavior with MPLS label. We make two selection,
     * we test that these two selection are different.
     */
    @Test
    public void testRandomBehaviorMpls() {
        // Verify the random behavior
        assertThat(this.allocator.getLabelSelection(), instanceOf(RandomSelection.class));
        // We test the behavior for MPLS
        Map<LinkKey, Identifier<?>> allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        Identifier<?> id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a Mplslabel
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel prevMplsId = (MplsLabel) id;
        assertTrue(0 <= prevMplsId.toInt() && prevMplsId.toInt() <= MplsLabel.MAX_MPLS);

        allocation = this.allocator.assignLabelToLinks(
                ImmutableSet.copyOf(links.subList(1, 2)),
                IntentId.valueOf(idGenerator.getNewId()),
                EncapsulationType.MPLS);
        id = allocation.get(LinkKey.linkKey(d1p1, d2p1));
        // value has to be a Mplslabel
        assertThat(id, instanceOf(MplsLabel.class));
        // value should not be a forbidden value
        MplsLabel mplsId = (MplsLabel) id;
        assertTrue(0 <= mplsId.toInt() && mplsId.toInt() <= MplsLabel.MAX_MPLS);
    }

    /**
     * To test the port key based API.
     */
    @Test
    public void testPortKey() {
        // Verify the first behavior
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
        id = allocation.get(new ConnectPoint(d2p1.elementId(), d2p1.port()));
        assertThat(id, instanceOf(VlanId.class));
        // value should not be a forbidden value
        VlanId vlanId = (VlanId) id;
        assertTrue(VlanId.NO_VID < vlanId.toShort() && vlanId.toShort() < VlanId.MAX_VLAN);
        // value has to be 1
        assertEquals(prevVlanId, vlanId);
    }



}
