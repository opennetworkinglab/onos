/*
 * Copyright 2015 Open Networking Laboratory
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

/**
 * Test for OchSignal.
 */
public class OchSignalTest {

    private final Lambda och1 = Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 1, 1);
    private final Lambda sameOch1 = Lambda.ochSignal(GridType.DWDM, ChannelSpacing.CHL_100GHZ, 1, 1);
    private final Lambda och2 = Lambda.ochSignal(GridType.CWDM, ChannelSpacing.CHL_6P25GHZ, 4, 1);
    private final Lambda sameOch2 = Lambda.ochSignal(GridType.CWDM, ChannelSpacing.CHL_6P25GHZ, 4, 1);

    @Test
    public void testEquality() {
        new EqualsTester()
                .addEqualityGroup(och1, sameOch1)
                .addEqualityGroup(och2, sameOch2)
                .testEquals();
    }
}
