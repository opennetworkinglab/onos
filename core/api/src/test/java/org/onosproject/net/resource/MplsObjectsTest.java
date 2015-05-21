/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net.resource;

import org.junit.Test;

import com.google.common.testing.EqualsTester;
import org.onosproject.net.resource.link.MplsLabel;
import org.onosproject.net.resource.link.MplsLabelResourceAllocation;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for MPLS objects.
 */
public class MplsObjectsTest {

    MplsLabel label1 = MplsLabel.valueOf(1);
    MplsLabel label2 = MplsLabel.valueOf(2);
    MplsLabel sameAsLabel1 = MplsLabel.valueOf(1);
    MplsLabel sameAsLabel2 = MplsLabel.valueOf(2);
    MplsLabel label3 = MplsLabel.valueOf(3);

    /**
     * Tests creation of MPLS label objects.
     */
    @Test
    public void checkLabelConstruction() {
        assertThat(label1.label().toInt(), is(1));
    }

    /**
     * Tests the operation of equals(), hashCode() and toString().
     */
    @Test
    public void testLabelEqualsOperation() {
        new EqualsTester()
                .addEqualityGroup(label1, sameAsLabel1)
                .addEqualityGroup(label2, sameAsLabel2)
                .addEqualityGroup(label3)
                .testEquals();
    }

    MplsLabelResourceAllocation labelAllocation1 =
            new MplsLabelResourceAllocation(label1);
    MplsLabelResourceAllocation sameAsLabelAllocation1 =
            new MplsLabelResourceAllocation(label1);
    MplsLabelResourceAllocation labelAllocation2 =
            new MplsLabelResourceAllocation(label2);
    MplsLabelResourceAllocation sameAsLabelAllocation2 =
            new MplsLabelResourceAllocation(label2);
    MplsLabelResourceAllocation labelAllocation3 =
            new MplsLabelResourceAllocation(label3);

    /**
     * Tests creation of MPLS label objects.
     */
    @Test
    public void checkLabelResourceAllocationConstruction() {
        assertThat(labelAllocation1.mplsLabel().label().toInt(), is(1));
        assertThat(labelAllocation1.type(), is(ResourceType.MPLS_LABEL));
    }

    /**
     * Tests the operation of equals(), hashCode() and toString().
     */
    @Test
    public void testLabelResourceAllocationEqualsOperation() {
        new EqualsTester()
                .addEqualityGroup(labelAllocation1, sameAsLabelAllocation1)
                .addEqualityGroup(labelAllocation2, sameAsLabelAllocation2)
                .addEqualityGroup(labelAllocation3)
                .testEquals();
    }
}
