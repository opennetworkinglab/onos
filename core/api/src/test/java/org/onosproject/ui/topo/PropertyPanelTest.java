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

package org.onosproject.ui.topo;

import org.junit.Test;
import org.onosproject.ui.topo.PropertyPanel.Prop;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link PropertyPanel}.
 */
public class PropertyPanelTest {

    private static final String TITLE_ORIG = "Original Title";
    private static final String TYPE_ORIG = "Original type ID";
    private static final String TITLE_NEW = "New Title";
    private static final String TYPE_NEW = "New type";

    private static final Prop PROP_A = new Prop("A", "Hay");
    private static final Prop PROP_B = new Prop("B", "Bee");
    private static final Prop PROP_C = new Prop("C", "Sea");
    private static final Prop PROP_Z = new Prop("Z", "Zed");

    private PropertyPanel pp;


    @Test
    public void basic() {
        pp = new PropertyPanel(TITLE_ORIG, TYPE_ORIG);
        assertEquals("wrong title", TITLE_ORIG, pp.title());
        assertEquals("wrong type", TYPE_ORIG, pp.typeId());
        assertEquals("unexpected props", 0, pp.properties().size());
    }

    @Test
    public void changeTitle() {
        basic();
        pp.title(TITLE_NEW);
        assertEquals("wrong title", TITLE_NEW, pp.title());
    }

    @Test
    public void changeType() {
        basic();
        pp.typeId(TYPE_NEW);
        assertEquals("wrong type", TYPE_NEW, pp.typeId());
    }

    private void validateProps(Prop... props) {
        Iterator<Prop> iter = pp.properties().iterator();
        for (Prop p: props) {
            Prop ppProp = iter.next();
            assertEquals("Bad prop sequence", p, ppProp);
        }
    }

    @Test
    public void props() {
        basic();
        pp.add(PROP_A).add(PROP_B).add(PROP_C);
        assertEquals("bad props", 3, pp.properties().size());
        validateProps(PROP_A, PROP_B, PROP_C);
    }

    @Test
    public void removeAllProps() {
        props();
        assertEquals("wrong props", 3, pp.properties().size());
        pp.removeAllProps();
        assertEquals("unexpected props", 0, pp.properties().size());
    }

    @Test
    public void adjustProps() {
        props();
        pp.removeProps("B", "A");
        pp.add(PROP_Z);
        validateProps(PROP_C, PROP_Z);
    }
}
