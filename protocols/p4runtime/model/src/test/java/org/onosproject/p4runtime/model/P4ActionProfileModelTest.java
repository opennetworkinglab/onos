/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiTableId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test for P4ActionProfile class.
 */
public class P4ActionProfileModelTest {

    private static final String TABLE_0 = "table0";
    private static final String WCMP_TABLE = "wcmp_table";

    private final PiTableId tableId = PiTableId.of(TABLE_0);
    private final PiTableId sameAsTableId = PiTableId.of(TABLE_0);
    private final PiTableId tableId2 = PiTableId.of(WCMP_TABLE);

    private final ImmutableSet<PiTableId> tables = new ImmutableSet.Builder<PiTableId>()
            .add(tableId, tableId2)
            .build();

    private final ImmutableSet<PiTableId> sameAsTables = new ImmutableSet.Builder<PiTableId>()
            .add(sameAsTableId, tableId2)
            .build();

    private final ImmutableSet<PiTableId> tables2 = new ImmutableSet.Builder<PiTableId>()
            .add(tableId, sameAsTableId)
            .build();

    private final PiActionProfileId id = PiActionProfileId.of("name");
    private final PiActionProfileId id2 = PiActionProfileId.of("name2");

    private final P4ActionProfileModel actProfModel = new P4ActionProfileModel(id, tables,
                                                                                true, 64, 10);
    private final P4ActionProfileModel sameAsActProfModel = new P4ActionProfileModel(id, sameAsTables,
                                                                                      true, 64, 10);
    private final P4ActionProfileModel actProfModel2 = new P4ActionProfileModel(id, tables2,
                                                                                 true, 64, 10);
    private final P4ActionProfileModel actProfModel3 = new P4ActionProfileModel(id2, tables,
                                                                                 true, 64, 10);
    private final P4ActionProfileModel actProfModel4 = new P4ActionProfileModel(id, tables,
                                                                                 false, 64, 10);
    private final P4ActionProfileModel actProfModel5 = new P4ActionProfileModel(id, tables,
                                                                                 true, 32, 5);

    /**
     * Checks that the P4ActionProfileModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4ActionProfileModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(actProfModel, sameAsActProfModel)
                .addEqualityGroup(actProfModel2)
                .addEqualityGroup(actProfModel3)
                .addEqualityGroup(actProfModel4)
                .addEqualityGroup(actProfModel5)
                .testEquals();
    }
}
