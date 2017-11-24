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
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiTableId;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for P4MeterModel class.
 */
public class P4MeterModelTest {

    private static final PiMeterId PI_METER_ID_1 = PiMeterId.of("Meter1");
    private static final PiMeterId PI_METER_ID_2 = PiMeterId.of("Meter2");

    private static final PiMeterType PI_METER_TYPE_1 = PiMeterType.DIRECT;
    private static final PiMeterType PI_METER_TYPE_2 = PiMeterType.INDIRECT;

    private static final PiMeterModel.Unit PI_METER_MODEL_UNIT_1 = PiMeterModel.Unit.BYTES;
    private static final PiMeterModel.Unit PI_METER_MODEL_UNIT_2 = PiMeterModel.Unit.PACKETS;

    private static final PiTableId PI_TABLE_ID_1 = PiTableId.of("Table1");
    private static final PiTableId PI_TABLE_ID_2 = PiTableId.of("Table2");

    private static final long SIZE_1 = 100;
    private static final long SIZE_2 = 200;

    private static final P4MeterModel P4_METER_MODEL_1 =
        new P4MeterModel(PI_METER_ID_1, PI_METER_TYPE_1, PI_METER_MODEL_UNIT_1, PI_TABLE_ID_1, SIZE_1);
    private static final P4MeterModel SAME_AS_P4_METER_MODEL_1 =
        new P4MeterModel(PI_METER_ID_1, PI_METER_TYPE_1, PI_METER_MODEL_UNIT_1, PI_TABLE_ID_1, SIZE_1);
    private static final P4MeterModel P4_METER_MODEL_2 =
        new P4MeterModel(PI_METER_ID_2, PI_METER_TYPE_2, PI_METER_MODEL_UNIT_2, PI_TABLE_ID_2, SIZE_2);
    private static final P4MeterModel P4_METER_MODEL_3 =
        new P4MeterModel(PI_METER_ID_1, PI_METER_TYPE_2, PI_METER_MODEL_UNIT_1, PI_TABLE_ID_2, SIZE_1);

    /**
     * Checks that the P4MeterModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4MeterModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
            .addEqualityGroup(P4_METER_MODEL_1, SAME_AS_P4_METER_MODEL_1)
            .addEqualityGroup(P4_METER_MODEL_2)
            .addEqualityGroup(P4_METER_MODEL_3)
            .testEquals();
    }


}
