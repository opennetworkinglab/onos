/*
 * Copyright 2018-present Open Networking Foundation
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

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiCounterData class.
 */
public class PiCounterCellDataTest {

    private static final long PACKETS_1 = 10;
    private static final long PACKETS_2 = 20;
    private static final long BYTES_1 = 100;
    private static final long BYTES_2 = 200;

    private static final PiCounterCellData PI_COUNTER_DATA_1 =
            new PiCounterCellData(PACKETS_1, BYTES_1);
    private static final PiCounterCellData SAME_AS_PI_COUNTER_DATA_1 =
            new PiCounterCellData(PACKETS_1, BYTES_1);
    private static final PiCounterCellData PI_COUNTER_DATA_2 =
            new PiCounterCellData(PACKETS_2, BYTES_2);

    /**
     * Checks that the PiCounterCellData class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiCounterCellData.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(PI_COUNTER_DATA_1, SAME_AS_PI_COUNTER_DATA_1)
                .addEqualityGroup(PI_COUNTER_DATA_2)
                .testEquals();
    }
}
