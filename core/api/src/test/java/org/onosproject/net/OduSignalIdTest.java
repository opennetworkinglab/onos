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
package org.onosproject.net;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import static org.onosproject.net.OduSignalId.oduSignalId;

/**
 * Test for OduSignalId.
 */
public class OduSignalIdTest {

    private final OduSignalId odu1 = oduSignalId(7, 80, new byte[] {16, 16, 16, 16, 16, 16, 16, 16, 16, 16});
    private final OduSignalId sameOdu1 = oduSignalId(7, 80, new byte[] {16, 16, 16, 16, 16, 16, 16, 16, 16, 16});
    private final OduSignalId odu2 = oduSignalId(21, 80, new byte[] {10, 5, 10, 5, 10, 5, 10, 5, 10, 5});
    private final OduSignalId sameOdu2 = oduSignalId(21, 80, new byte[] {10, 5, 10, 5, 10, 5, 10, 5, 10, 5});

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(odu1, sameOdu1)
                .addEqualityGroup(odu2, sameOdu2)
                .testEquals();
    }
}
