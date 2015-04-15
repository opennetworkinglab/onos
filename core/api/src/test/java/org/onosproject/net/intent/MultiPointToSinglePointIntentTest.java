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
 * Suite of tests of the multi-to-single point intent descriptor.
 */
public class MultiPointToSinglePointIntentTest extends ConnectivityIntentTest {

    /**
     * Checks that the MultiPointToSinglePointIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(MultiPointToSinglePointIntent.class);
    }

    @Test
    public void basics() {
        MultiPointToSinglePointIntent intent = createOne();
        assertEquals("incorrect id", APPID, intent.appId());
        assertEquals("incorrect match", MATCH, intent.selector());
        assertEquals("incorrect ingress", PS1, intent.ingressPoints());
        assertEquals("incorrect egress", P2, intent.egressPoint());
    }

    @Override
    protected MultiPointToSinglePointIntent createOne() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS1)
                .egressPoint(P2)
                .build();
    }

    @Override
    protected MultiPointToSinglePointIntent createAnother() {
        return MultiPointToSinglePointIntent.builder()
                .appId(APPID)
                .selector(MATCH)
                .treatment(NOP)
                .ingressPoints(PS2)
                .egressPoint(P1)
                .build();
    }
}
