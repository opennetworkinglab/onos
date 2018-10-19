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

package org.onosproject.net.pi.runtime;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiTableId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DROP;

/**
 * Unit tests for PiCounterCell class.
 */
public class PiCounterCellTest {

    private static final PiTableEntry PI_TABLE_ENTRY_1 = PiTableEntry.builder()
            .forTable(PiTableId.of("T10"))
            .withCookie(0xac)
            .withPriority(10)
            .withAction(PiAction.builder().withId(PiActionId.of(DROP)).build())
            .withTimeout(100)
            .build();
    private static final PiTableEntry PI_TABLE_ENTRY_2 = PiTableEntry.builder()
            .forTable(PiTableId.of("T20"))
            .withCookie(0xac)
            .withPriority(10)
            .withAction(PiAction.builder().withId(PiActionId.of(DROP)).build())
            .withTimeout(1000)
            .build();

    private static final PiCounterCellId PI_COUNTER_CELL_ID_1 =
            PiCounterCellId.ofDirect(PI_TABLE_ENTRY_1);
    private static final PiCounterCellId PI_COUNTER_CELL_ID_2 =
            PiCounterCellId.ofDirect(PI_TABLE_ENTRY_2);

    private static final long PACKETS_1 = 10;
    private static final long PACKETS_2 = 20;
    private static final long BYTES_1 = 100;
    private static final long BYTES_2 = 200;

    private static final PiCounterCell PI_COUNTER_CELL_1 =
            new PiCounterCell(PI_COUNTER_CELL_ID_1, PACKETS_1, BYTES_1);
    private static final PiCounterCell SAME_AS_PI_COUNTER_CELL_1 =
            new PiCounterCell(PI_COUNTER_CELL_ID_1, PACKETS_1, BYTES_1);
    private static final PiCounterCell PI_COUNTER_CELL_2 =
            new PiCounterCell(PI_COUNTER_CELL_ID_2, PACKETS_2, BYTES_2);

    /**
     * Checks that the PiCounterCell class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiCounterCell.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(PI_COUNTER_CELL_1, SAME_AS_PI_COUNTER_CELL_1)
                .addEqualityGroup(PI_COUNTER_CELL_2)
                .testEquals();
    }
}
