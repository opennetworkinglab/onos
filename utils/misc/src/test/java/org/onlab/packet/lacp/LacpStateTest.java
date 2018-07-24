/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onlab.packet.lacp;

import org.junit.Test;

import static org.junit.Assert.*;

public class LacpStateTest {

    @Test
    public void toByte() {
        LacpState state = new LacpState((byte) 0x15);
        assertEquals((byte) 0x15, state.toByte());
    }

    @Test
    public void isActive() {
        LacpState state = new LacpState((byte) 0x1);
        assertTrue(state.isActive());
    }

    @Test
    public void isTimeout() {
        LacpState state = new LacpState((byte) 0x2);
        assertTrue(state.isTimeout());
    }

    @Test
    public void isAggregatable() {
        LacpState state = new LacpState((byte) 0x4);
        assertTrue(state.isAggregatable());
    }

    @Test
    public void isSync() {
        LacpState state = new LacpState((byte) 0x8);
        assertTrue(state.isSync());
    }

    @Test
    public void isCollecting() {
        LacpState state = new LacpState((byte) 0x10);
        assertTrue(state.isCollecting());
    }

    @Test
    public void isDistributing() {
        LacpState state = new LacpState((byte) 0x20);
        assertTrue(state.isDistributing());
    }

    @Test
    public void isDefault() {
        LacpState state = new LacpState((byte) 0x40);
        assertTrue(state.isDefault());
    }

    @Test
    public void isExpired() {
        LacpState state = new LacpState((byte) 0x80);
        assertTrue(state.isExpired());
    }

    @Test
    public void equals() {
        LacpState state1 = new LacpState((byte) 0x15);
        LacpState state2 = new LacpState((byte) 0x15);
        LacpState state3 = new LacpState((byte) 0x51);

        assertEquals(state1, state2);
        assertNotEquals(state1, state3);

    }
}