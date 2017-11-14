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
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.pi.model.PiActionParamId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.SRC_ADDR;

/**
 * Unit tests for PiActionParam class.
 */
public class PiActionParamTest {
    private ImmutableByteSequence value1 = copyFrom(0x0a010101);
    private ImmutableByteSequence value2 = copyFrom(0x0a010102);
    private final PiActionParam piActionParam1 = new PiActionParam(PiActionParamId.of(DST_ADDR), value1);
    private final PiActionParam sameAsPiActionParam1 = new PiActionParam(PiActionParamId.of(DST_ADDR), value1);
    private final PiActionParam piActionParam2 = new PiActionParam(PiActionParamId.of(DST_ADDR), value2);

    /**
     * Checks that the PiActionParam class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiActionParam.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piActionParam1, sameAsPiActionParam1)
                .addEqualityGroup(piActionParam2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiActionParam object.
     */
    @Test
    public void testConstruction() {
        ImmutableByteSequence value = copyFrom(0x0b010102);
        final PiActionParamId piActionParamId = PiActionParamId.of(SRC_ADDR);
        final PiActionParam piActionParam = new PiActionParam(piActionParamId, value);
        assertThat(piActionParam, is(notNullValue()));
        assertThat(piActionParam.id(), is(piActionParamId));
        assertThat(piActionParam.value(), is(value));
    }
}
