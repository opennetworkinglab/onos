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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiTableId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test for P4CounterModel class.
 */
public class P4CounterModelTest {
    private static final String TABLE_0 = "table0";
    private static final String WCMP_TABLE = "wcmp_table";

    private final PiTableId tableId = PiTableId.of(TABLE_0);
    private final PiTableId sameAsTableId = PiTableId.of(TABLE_0);
    private final PiTableId tableId2 = PiTableId.of(WCMP_TABLE);

    private final PiCounterId counterId = PiCounterId.of("name");

    private final P4CounterModel counterModel = new P4CounterModel(counterId, PiCounterType.DIRECT,
                                                                   PiCounterModel.Unit.BYTES, tableId, 16);

    private final P4CounterModel sameAsCounterModel = new P4CounterModel(counterId, PiCounterType.DIRECT,
                                                                         PiCounterModel.Unit.BYTES, sameAsTableId, 16);

    private final P4CounterModel counterModel2 = new P4CounterModel(counterId, PiCounterType.INDIRECT,
                                                                    PiCounterModel.Unit.BYTES, tableId, 16);

    private final P4CounterModel counterModel3 = new P4CounterModel(counterId, PiCounterType.DIRECT,
                                                                    PiCounterModel.Unit.PACKETS, tableId, 16);

    private final P4CounterModel counterModel4 = new P4CounterModel(counterId, PiCounterType.DIRECT,
                                                                    PiCounterModel.Unit.BYTES, tableId2, 16);

    private final P4CounterModel counterModel5 = new P4CounterModel(counterId, PiCounterType.DIRECT,
                                                                    PiCounterModel.Unit.BYTES, tableId, 32);
    /**
     * Checks that the P4CounterModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4CounterModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(counterModel, sameAsCounterModel)
                .addEqualityGroup(counterModel2)
                .addEqualityGroup(counterModel3)
                .addEqualityGroup(counterModel4)
                .addEqualityGroup(counterModel5)
                .testEquals();
    }
}