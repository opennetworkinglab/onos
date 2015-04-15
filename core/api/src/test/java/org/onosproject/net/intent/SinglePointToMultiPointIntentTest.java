/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Suite of tests of the single-to-multi point intent descriptor.
 */
public class SinglePointToMultiPointIntentTest extends ConnectivityIntentTest {

    /**
     * Checks that the SinglePointToMultiPointIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(SinglePointToMultiPointIntent.class);
    }

    @Test
    public void basics() {
        SinglePointToMultiPointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", P1, intent.ingressPoint());
        assertEquals("incorrect egress", PS2, intent.egressPoints());
    }

    @Override
    protected SinglePointToMultiPointIntent createOne() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P1)
                .egressPoints(PS2)
                .build();
    }

    @Override
    protected SinglePointToMultiPointIntent createAnother() {
        return SinglePointToMultiPointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoint(P2)
                .egressPoints(PS1)
                .build();
    }
}
