/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.meter;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.DeviceId;

import java.util.Collection;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;

/**
 * Unit tests for the MeterOperationTest object.
 */
public class MeterOperationTest {



    /**
     * Checks that the MeterOperation class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(MeterOperation.class);
    }

    @Test
    public void testEquality() {
        final Meter m1 = new TestMeter();
        final Meter m2 = new TestMeter();
        final MeterOperation op1 = new MeterOperation(m1,
                                                      MeterOperation.Type.ADD);
        final MeterOperation sameAsOp1 = new MeterOperation(m1,
                                                            MeterOperation.Type.ADD);
        final MeterOperation op2 = new MeterOperation(m2,
                                            MeterOperation.Type.ADD);

        new EqualsTester()
                .addEqualityGroup(op1, sameAsOp1)
                .addEqualityGroup(op2)
                .testEquals();
    }

    @Test
    public void testConstruction() {
        final Meter m1 = new TestMeter();

        final MeterOperation op = new MeterOperation(m1, MeterOperation.Type.ADD);

        assertThat(op.meter(), is(m1));
    }

    private static final class TestMeter extends AbstractAnnotated implements Meter {

        @Override
        public DeviceId deviceId() {
            return null;
        }

        @Override
        public MeterId id() {
            return null;
        }

        @Override
        public MeterCellId meterCellId() {
            return null;
        }

        @Override
        public ApplicationId appId() {
            return null;
        }

        @Override
        public Unit unit() {
            return null;
        }

        @Override
        public boolean isBurst() {
            return false;
        }

        @Override
        public Collection<Band> bands() {
            return null;
        }

        @Override
        public MeterState state() {
            return null;
        }

        @Override
        public long life() {
            return 0;
        }

        @Override
        public long referenceCount() {
            return 0;
        }

        @Override
        public long packetsSeen() {
            return 0;
        }

        @Override
        public long bytesSeen() {
            return 0;
        }
    }

}
