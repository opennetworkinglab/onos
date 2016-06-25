/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.testing.EqualsTester;

import static org.junit.Assert.*;
import static org.onosproject.net.ChannelSpacing.CHL_25GHZ;
import static org.onosproject.net.ChannelSpacing.CHL_50GHZ;
import static org.onosproject.net.DefaultOchSignalComparator.newOchSignalTreeSet;
import static org.onosproject.net.OchSignal.newDwdmSlot;
import static org.onosproject.net.OchSignal.newFlexGridSlot;

import java.util.SortedSet;
import org.junit.Test;

/**
 * Test for OchSignal.
 */
public class OchSignalTest {

    @Test
    public void testEquality() {
        OchSignal och1 = newDwdmSlot(ChannelSpacing.CHL_100GHZ, 1);
        OchSignal sameOch1 = newDwdmSlot(ChannelSpacing.CHL_100GHZ, 1);
        OchSignal och2 = new OchSignal(GridType.CWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
        OchSignal sameOch2 = new OchSignal(GridType.CWDM, ChannelSpacing.CHL_100GHZ, 4, 8);
        OchSignal och3 = newDwdmSlot(ChannelSpacing.CHL_100GHZ, 3);
        OchSignal sameOch3 = newDwdmSlot(ChannelSpacing.CHL_100GHZ, 3);
        OchSignal och4 = newFlexGridSlot(3);
        OchSignal sameOch4 = newFlexGridSlot(3);

        new EqualsTester()
                .addEqualityGroup(och1, sameOch1)
                .addEqualityGroup(och2, sameOch2)
                .addEqualityGroup(och3, sameOch3)
                .addEqualityGroup(och4, sameOch4)
                .testEquals();
    }

    @Test
    public void testToFlexgrid50() {
        OchSignal input = newDwdmSlot(CHL_50GHZ, 0);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(-3), newFlexGridSlot(-1),
                    newFlexGridSlot(+1), newFlexGridSlot(+3)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFlexgrid50Plus1() {
        OchSignal input = newDwdmSlot(CHL_50GHZ, 1);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        // Note: 8 = 50Ghz / 6.25Ghz
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(8 - 3), newFlexGridSlot(8 - 1),
                    newFlexGridSlot(8 + 1), newFlexGridSlot(8 + 3)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFlexgrid50minus1() {
        OchSignal input = newDwdmSlot(CHL_50GHZ, -1);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        // Note: 8 = 50Ghz / 6.25Ghz
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(-8 - 3), newFlexGridSlot(-8 - 1),
                    newFlexGridSlot(-8 + 1), newFlexGridSlot(-8 + 3)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFlexgrid25() {
        OchSignal input = newDwdmSlot(CHL_25GHZ, 0);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(-1),
                    newFlexGridSlot(+1)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFlexgrid25Plus2() {
        OchSignal input = newDwdmSlot(CHL_25GHZ, 2);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        // Note: 8 = 25Ghz / 6.25Ghz * 2
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(8 - 1),
                    newFlexGridSlot(8 + 1)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFlexgrid25minus2() {
        OchSignal input = newDwdmSlot(CHL_25GHZ, -2);
        SortedSet<OchSignal> expected = newOchSignalTreeSet();
        // Note: 8 = 50Ghz / 6.25Ghz * 2
        expected.addAll(ImmutableList.of(
                    newFlexGridSlot(-8 - 1),
                    newFlexGridSlot(-8 + 1)));

        SortedSet<OchSignal> flexGrid = OchSignal.toFlexGrid(input);

        assertEquals(expected, flexGrid);
    }

    @Test
    public void testToFixedgrid50() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(-3), newFlexGridSlot(-1),
                    newFlexGridSlot(+1), newFlexGridSlot(+3)));

        OchSignal expected = newDwdmSlot(CHL_50GHZ, 0);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_50GHZ));
    }

    @Test
    public void testToFixedgrid50plus1() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        // Note: 8 = 50Ghz / 6.25Ghz
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(8 - 3), newFlexGridSlot(8 - 1),
                    newFlexGridSlot(8 + 1), newFlexGridSlot(8 + 3)));

        OchSignal expected = newDwdmSlot(CHL_50GHZ, 1);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_50GHZ));
    }

    @Test
    public void testToFixedgrid50minus1() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        // Note: 8 = 50Ghz / 6.25Ghz
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(-8 - 3), newFlexGridSlot(-8 - 1),
                    newFlexGridSlot(-8 + 1), newFlexGridSlot(-8 + 3)));

        OchSignal expected = newDwdmSlot(CHL_50GHZ, -1);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_50GHZ));
    }

    @Test
    public void testToFixedgrid25() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(-1),
                    newFlexGridSlot(+1)));

        OchSignal expected = newDwdmSlot(CHL_25GHZ, 0);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_25GHZ));
    }

    @Test
    public void testToFixedgrid25plus2() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        // Note: 8 = 25Ghz / 6.25Ghz * 2
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(8 - 1),
                    newFlexGridSlot(8 + 1)));

        OchSignal expected = newDwdmSlot(CHL_25GHZ, 2);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_25GHZ));
    }

    @Test
    public void testToFixedgrid25minus2() {
        SortedSet<OchSignal> input = newOchSignalTreeSet();
        // Note: 8 = 25Ghz / 6.25Ghz * 2
        input.addAll(ImmutableList.of(
                    newFlexGridSlot(-8 - 1),
                    newFlexGridSlot(-8 + 1)));

        OchSignal expected = newDwdmSlot(CHL_25GHZ, -2);
        assertEquals(expected, OchSignal.toFixedGrid(Lists.newArrayList(input), CHL_25GHZ));
    }

}
