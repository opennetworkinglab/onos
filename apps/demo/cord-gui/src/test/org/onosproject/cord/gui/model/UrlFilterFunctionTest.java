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
 *
 */

package org.onosproject.cord.gui.model;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link UrlFilterFunction}.
 */
public class UrlFilterFunctionTest {

    private SubscriberUser user = new SubscriberUser(1, "foo", "fooMAC", "levelX");
    private UrlFilterFunction fn;

    @Before
    public void setUp() {
        fn = new UrlFilterFunction();
    }

    @Test
    public void basic() {
        assertEquals("wrong enum const count",
                     6, UrlFilterFunction.Level.values().length);
    }

    @Test
    public void memento() {
        XosFunction.Memento memo = fn.createMemento();
        assertTrue("wrong class", memo instanceof UrlFilterFunction.UrlFilterMemento);
        UrlFilterFunction.UrlFilterMemento umemo =
                (UrlFilterFunction.UrlFilterMemento) memo;
        assertEquals("wrong default level", "G", umemo.level());
    }

    @Test
    public void memoNewLevel() {
        XosFunction.Memento memo = fn.createMemento();
        assertTrue("wrong class", memo instanceof UrlFilterFunction.UrlFilterMemento);
        UrlFilterFunction.UrlFilterMemento umemo =
                (UrlFilterFunction.UrlFilterMemento) memo;
        assertEquals("wrong default level", "G", umemo.level());
        umemo.setLevel(UrlFilterFunction.Level.R);
        assertEquals("wrong new level", "R", umemo.level());
    }

    @Test
    public void applyMemo() {
        UrlFilterFunction.UrlFilterMemento memo =
                (UrlFilterFunction.UrlFilterMemento) fn.createMemento();
        memo.setLevel(UrlFilterFunction.Level.PG_13);
        user.setMemento(XosFunctionDescriptor.URL_FILTER, memo);

        assertEquals("wrong URL suffix", "url_filter/PG_13", fn.xosUrlApply(user));
    }
}
