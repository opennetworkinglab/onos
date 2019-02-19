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

package org.onosproject.ui.topo;

import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.ui.topo.PropertyPanel.Prop;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link PropertyPanel}.
 */
public class PropertyPanelTest {

    // Modified property panel subclass to use ENGLISH locale formatter so
    //  we know formatted numbers will use comma for the thousand separator.
    private static final class EnglishPropertyPanel extends PropertyPanel {
        private static final NumberFormat ENGLISH_FORMATTER =
                NumberFormat.getInstance(Locale.ENGLISH);

        public EnglishPropertyPanel(String title, String glyphId) {
            super(title, glyphId);
        }

        @Override
        protected NumberFormat formatter() {
            return ENGLISH_FORMATTER;
        }
    }

    private static final String TITLE_ORIG = "Original Title";
    private static final String GLYPH_ORIG = "Original glyph ID";
    private static final String TITLE_NEW = "New Title";
    private static final String GLYPH_NEW = "New glyph ID";
    private static final String SOME_IDENTIFICATION = "It's Me!";

    private static final String KEY_A = "A";
    private static final String KEY_B = "B";
    private static final String KEY_C = "C";
    private static final String SEP = "-";
    private static final String KEY_Z = "Z";

    private static final String LABEL_A = "labA";
    private static final String LABEL_B = "labB";
    private static final String LABEL_C = "labC";
    private static final String LABEL_Z = "labZ";

    private static final String VALUE_A = "Hay";
    private static final String VALUE_B = "Bee";
    private static final String VALUE_C = "Sea";
    private static final String VALUE_Z = "Zed";

    private static final Map<String, Prop> PROP_MAP = new HashMap<>();

    private static class FooClass {
        private final String s;
        FooClass(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return ">" + s + "<";
        }
    }

    private PropertyPanel pp;



    @BeforeClass
    public static void setUpClass() {
        PROP_MAP.put(KEY_A, new Prop(KEY_A, LABEL_A, VALUE_A));
        PROP_MAP.put(KEY_B, new Prop(KEY_B, LABEL_B, VALUE_B));
        PROP_MAP.put(KEY_C, new Prop(KEY_C, LABEL_C, VALUE_C));
        PROP_MAP.put(KEY_Z, new Prop(KEY_Z, LABEL_Z, VALUE_Z));
        PROP_MAP.put(SEP, new PropertyPanel.Separator());
    }

    @Test
    public void basic() {
        pp = new EnglishPropertyPanel(TITLE_ORIG, GLYPH_ORIG);
        assertEquals("wrong title", TITLE_ORIG, pp.title());
        assertEquals("wrong glyph", GLYPH_ORIG, pp.glyphId());
        assertNull("id?", pp.id());
        assertEquals("unexpected props", 0, pp.properties().size());
        assertEquals("unexpected buttons", 0, pp.buttons().size());
    }

    @Test
    public void changeTitle() {
        basic();
        pp.title(TITLE_NEW);
        assertEquals("wrong title", TITLE_NEW, pp.title());
    }

    @Test
    public void changeGlyph() {
        basic();
        pp.glyphId(GLYPH_NEW);
        assertEquals("wrong glyph", GLYPH_NEW, pp.glyphId());
    }

    @Test
    public void setId() {
        basic();
        pp.id(SOME_IDENTIFICATION);
        assertEquals("wrong id", SOME_IDENTIFICATION, pp.id());
    }

    private void validateProps(String... keys) {
        Iterator<Prop> iter = pp.properties().iterator();
        for (String k: keys) {
            Prop exp = PROP_MAP.get(k);
            Prop act = iter.next();
            assertEquals("Bad prop sequence", exp, act);
        }
    }

    private void validateProp(String key, String expValue) {
        Iterator<Prop> iter = pp.properties().iterator();
        Prop prop = null;
        while (iter.hasNext()) {
            Prop p = iter.next();
            if (p.key().equals(key)) {
                prop = p;
                break;
            }
        }
        if (prop == null) {
            fail("no prop found with key: " + key);
        }
        assertEquals("Wrong prop value", expValue, prop.value());
    }

    @Test
    public void props() {
        basic();
        pp.addProp(KEY_A, KEY_A, VALUE_A)
                .addProp(KEY_B, KEY_B, VALUE_B)
                .addProp(KEY_C, KEY_C, VALUE_C);
        assertEquals("bad props", 3, pp.properties().size());
        validateProps(KEY_A, KEY_B, KEY_C);
    }


    @Test
    public void localizedProp() {
        basic();
        pp.addProp(KEY_A, LABEL_A, VALUE_A);
        Prop p = pp.properties().get(0);
        assertEquals("wrong key", KEY_A, p.key());
        assertEquals("wrong label", LABEL_A, p.label());
        assertEquals("wrong value", VALUE_A, p.value());
    }

    @Test
    public void nonLocalizedProp() {
        basic();
        pp.addProp(KEY_A, KEY_A, VALUE_A);
        Prop p = pp.properties().get(0);
        assertEquals("wrong key", KEY_A, p.key());
        assertEquals("wrong label", KEY_A, p.label());
        assertEquals("wrong value", VALUE_A, p.value());
    }

    @Test
    public void separator() {
        props();
        pp.addSeparator()
                .addProp(KEY_Z, KEY_Z, VALUE_Z);

        assertEquals("bad props", 5, pp.properties().size());
        validateProps(KEY_A, KEY_B, KEY_C, SEP, KEY_Z);
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
        pp.removeProps(KEY_B, KEY_A);
        pp.addProp(KEY_Z, KEY_Z, VALUE_Z);
        validateProps(KEY_C, KEY_Z);
    }

    @Test
    public void intValues() {
        basic();
        pp.addProp(KEY_A, KEY_A, 200)
                .addProp(KEY_B, KEY_B, 2000)
                .addProp(KEY_C, KEY_C, 1234567);

        validateProp(KEY_A, "200");
        validateProp(KEY_B, "2,000");
        validateProp(KEY_C, "1,234,567");
    }

    @Test
    public void longValues() {
        basic();
        pp.addProp(KEY_A, KEY_A, 200L)
                .addProp(KEY_B, KEY_B, 2000L)
                .addProp(KEY_C, KEY_C, 1234567L)
                .addProp(KEY_Z, KEY_Z, Long.MAX_VALUE);

        validateProp(KEY_A, "200");
        validateProp(KEY_B, "2,000");
        validateProp(KEY_C, "1,234,567");
        validateProp(KEY_Z, "9,223,372,036,854,775,807");
    }

    @Test
    public void objectValue() {
        basic();
        pp.addProp(KEY_A, KEY_A, new FooClass("a"))
                .addProp(KEY_B, KEY_B, new FooClass("bxyyzy"), "[xz]");

        validateProp(KEY_A, ">a<");
        validateProp(KEY_B, ">byyy<");
    }

    private static final ButtonId BD_A = new ButtonId(KEY_A);
    private static final ButtonId BD_B = new ButtonId(KEY_B);
    private static final ButtonId BD_C = new ButtonId(KEY_C);
    private static final ButtonId BD_Z = new ButtonId(KEY_Z);

    private void verifyButtons(String... keys) {
        Iterator<ButtonId> iter = pp.buttons().iterator();
        for (String k: keys) {
            assertEquals("wrong button", k, iter.next().id());
        }
        assertFalse("too many buttons", iter.hasNext());
    }

    @Test
    public void buttons() {
        basic();
        pp.addButton(BD_A)
                .addButton(BD_B);
        assertEquals("wrong buttons", 2, pp.buttons().size());
        verifyButtons(KEY_A, KEY_B);

        pp.removeButtons(BD_B)
                .addButton(BD_C)
                .addButton(BD_Z);
        assertEquals("wrong buttons", 3, pp.buttons().size());
        verifyButtons(KEY_A, KEY_C, KEY_Z);

        pp.removeAllButtons()
                .addButton(BD_B);
        assertEquals("wrong buttons", 1, pp.buttons().size());
        verifyButtons(KEY_B);
    }
}
