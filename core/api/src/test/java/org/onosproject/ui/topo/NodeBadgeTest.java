/*
 *  Copyright 2015 Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.topo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link NodeBadge}.
 */
public class NodeBadgeTest {

    private static final String SOME_MSG = "a msg";
    private static final String WR_T = "wrong type";
    private static final String WR_M = "wrong message";
    private static final String WR_SF = "wrong string format";

    private NodeBadge badge;

    private String expStr(String t) {
        return "{Badge {" + t + "} \"" + SOME_MSG + "\"}";
    }

    private String expNumStr(String n) {
        return "{Badge {n} \"" + n + "\"}";
    }

    @Test
    public void info() {
        badge = NodeBadge.info(SOME_MSG);
        assertEquals(WR_T, NodeBadge.Type.INFO, badge.type());
        assertEquals(WR_M, SOME_MSG, badge.message());
        assertEquals(WR_SF, expStr("i"), badge.toString());
    }

    @Test
    public void warn() {
        badge = NodeBadge.warn(SOME_MSG);
        assertEquals(WR_T, NodeBadge.Type.WARN, badge.type());
        assertEquals(WR_M, SOME_MSG, badge.message());
        assertEquals(WR_SF, expStr("w"), badge.toString());
    }

    @Test
    public void error() {
        badge = NodeBadge.error(SOME_MSG);
        assertEquals(WR_T, NodeBadge.Type.ERROR, badge.type());
        assertEquals(WR_M, SOME_MSG, badge.message());
        assertEquals(WR_SF, expStr("e"), badge.toString());
    }

    @Test
    public void checkMark() {
        badge = NodeBadge.checkMark(SOME_MSG);
        assertEquals(WR_T, NodeBadge.Type.CHECK_MARK, badge.type());
        assertEquals(WR_M, SOME_MSG, badge.message());
        assertEquals(WR_SF, expStr("/"), badge.toString());
    }

    @Test
    public void xMark() {
        badge = NodeBadge.xMark(SOME_MSG);
        assertEquals(WR_T, NodeBadge.Type.X_MARK, badge.type());
        assertEquals(WR_M, SOME_MSG, badge.message());
        assertEquals(WR_SF, expStr("X"), badge.toString());
    }

    @Test
    public void number0() {
        badge = NodeBadge.number(0);
        assertEquals(WR_T, NodeBadge.Type.NUMBER, badge.type());
        assertEquals(WR_M, "0", badge.message());
        assertEquals(WR_SF, expNumStr("0"), badge.toString());
    }

    @Test
    public void number5() {
        badge = NodeBadge.number(5);
        assertEquals(WR_T, NodeBadge.Type.NUMBER, badge.type());
        assertEquals(WR_M, "5", badge.message());
        assertEquals(WR_SF, expNumStr("5"), badge.toString());
    }

    @Test
    public void number99() {
        badge = NodeBadge.number(99);
        assertEquals(WR_T, NodeBadge.Type.NUMBER, badge.type());
        assertEquals(WR_M, "99", badge.message());
        assertEquals(WR_SF, expNumStr("99"), badge.toString());
    }

    @Test
    public void badgeTypes() {
        assertEquals(WR_SF, "i", NodeBadge.Type.INFO.code());
        assertEquals(WR_SF, "w", NodeBadge.Type.WARN.code());
        assertEquals(WR_SF, "e", NodeBadge.Type.ERROR.code());
        assertEquals(WR_SF, "/", NodeBadge.Type.CHECK_MARK.code());
        assertEquals(WR_SF, "X", NodeBadge.Type.X_MARK.code());
        assertEquals(WR_SF, "n", NodeBadge.Type.NUMBER.code());
    }
}
