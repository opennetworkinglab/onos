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
import org.onosproject.net.pi.model.PiCounterId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for PiCounterId class.
 */
public class PiCounterIdTest {

    private static final PiCounterId PI_COUNTER_ID_1 = PiCounterId.of("Name1");
    private static final PiCounterId SAME_AS_PI_COUNTER_ID_1 = PiCounterId.of("Name1");
    private static final PiCounterId PI_COUNTER_ID_2 = PiCounterId.of("Name2");

    /**
     * Checks that the PiCounterId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(PiCounterId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(PI_COUNTER_ID_1, SAME_AS_PI_COUNTER_ID_1)
                .addEqualityGroup(PI_COUNTER_ID_2)
                .testEquals();
    }

}

