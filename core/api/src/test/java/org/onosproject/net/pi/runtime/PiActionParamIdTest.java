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
import org.onosproject.net.pi.model.PiActionParamId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.pi.runtime.PiConstantsTest.DST_ADDR;
import static org.onosproject.net.pi.runtime.PiConstantsTest.PORT;
import static org.onosproject.net.pi.runtime.PiConstantsTest.SRC_ADDR;

/**
 * Unit tests for PiActionParamId class.
 */
public class PiActionParamIdTest {
    final PiActionParamId piActionParamId1 = PiActionParamId.of(PORT);
    final PiActionParamId sameAsPiActionParamId1 = PiActionParamId.of(PORT);
    final PiActionParamId piActionParamId2 = PiActionParamId.of(DST_ADDR);

    /**
     * Checks that the PiActionParamId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiActionParamId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(piActionParamId1, sameAsPiActionParamId1)
                .addEqualityGroup(piActionParamId2)
                .testEquals();
    }

    /**
     * Checks the construction of a PiActionParamId object.
     */
    @Test
    public void testConstruction() {
        final String param = SRC_ADDR;
        final PiActionParamId actionParamId = PiActionParamId.of(param);
        assertThat(actionParamId, is(notNullValue()));
        assertThat(actionParamId.id(), is(param));
    }
}
