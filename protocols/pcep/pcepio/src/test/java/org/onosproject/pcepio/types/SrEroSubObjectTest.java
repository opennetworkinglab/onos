/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.pcepio.types;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.pcepio.protocol.PcepNai;

public class SrEroSubObjectTest {

    private final boolean bFFlag = false;
    private final boolean bSFlag = false;
    private final boolean bCFlag = false;
    private final boolean bMFlag = false;
    private final byte st = 1;
    private final int sID = 1;
    private final PcepNai nai = null;

    private final SrEroSubObject tlv1 = SrEroSubObject.of(st, bFFlag, bSFlag, bCFlag, bMFlag, sID, nai);

    private final boolean bFFlag1 = false;
    private final boolean bSFlag1 = false;
    private final boolean bCFlag1 = false;
    private final boolean bMFlag1 = false;
    private final byte st1 = 1;
    private final int sID1 = 1;
    private final PcepNai nai1 = null;

    private final SrEroSubObject tlv2 = SrEroSubObject.of(st1, bFFlag1, bSFlag1, bCFlag1, bMFlag1, sID1, nai1);

    private final boolean bFFlag2 = true;
    private final boolean bSFlag2 = true;
    private final boolean bCFlag2 = true;
    private final boolean bMFlag2 = true;
    private final byte st2 = 2;
    private final int sID2 = 2;
    private final PcepNai nai2 = null;

    private final SrEroSubObject tlv3 = SrEroSubObject.of(st2, bFFlag2, bSFlag2, bCFlag2, bMFlag2, sID2, nai2);

    @Test
    public void basics() {
        new EqualsTester().addEqualityGroup(tlv1, tlv2).addEqualityGroup(tlv3).testEquals();
    }

}
