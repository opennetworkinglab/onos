/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.ui.lion;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.ui.AbstractUiTest;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Unit tests for {@link LionUtils}.
 */
public class LionUtilsTest extends AbstractUiTest {

    private static Locale systemLocale;

    private ResourceBundle res;

    @BeforeClass
    public static void classSetup() {
        systemLocale = Locale.getDefault();
    }

    @AfterClass
    public static void classTeardown() {
        Locale.setDefault(systemLocale);
    }

    @Before
    public void testSetup() {
        // reset to a known default locale before starting each test
        Locale.setDefault(Locale.US);
    }

    @Test
    public void getBundleByClassAndName() {
        title("getBundleByClassAndName");
        res = LionUtils.getBundledResource(LionUtilsTest.class, "SomeResource");
        assertNotNull("missing resource bundle", res);
        String v1 = res.getString("key1");
        String v2 = res.getString("key2");
        print("v1 is %s, v2 is %s", v1, v2);
        assertEquals("v1 value wrong", "value one", v1);
        assertEquals("v2 value wrong", "value two", v2);

        res = LionUtils.getBundledResource(LionUtils.class, "SomeOtherResource");
        assertNotNull("missing OTHER resource bundle", res);
        v1 = res.getString("key1");
        v2 = res.getString("key2");
        print("v1 is %s, v2 is %s", v1, v2);
        assertEquals("v1 value wrong", "Hay", v1);
        assertEquals("v2 value wrong", "Bee", v2);
    }

    @Test
    public void getBundleByClassname() {
        title("getBundleByClassname");
        res = LionUtils.getBundledResource(LionUtils.class);
        assertNotNull("missing resource bundle", res);
        String v1 = res.getString("foo");
        String v2 = res.getString("boo");
        print("v1 is %s, v2 is %s", v1, v2);
        assertEquals("v1 value wrong", "bar", v1);
        assertEquals("v2 value wrong", "ghost", v2);
    }

    @Test
    public void getBundleByFqcn() {
        title("getBundleByFqcn");
        String fqcn = "org.onosproject.ui.lion.LionUtils";
        res = LionUtils.getBundledResource(fqcn);
        assertNotNull("missing resource bundle", res);
        String v1 = res.getString("foo");
        String v2 = res.getString("boo");
        print("v1 is %s, v2 is %s", v1, v2);
        assertEquals("v1 value wrong", "bar", v1);
        assertEquals("v2 value wrong", "ghost", v2);
    }

    @Test
    public void messageInEnglish() {
        title("messageInEnglish");
        // use default locale
        res = LionUtils.getBundledResource(LionUtils.class, "MyBundle");
        print("res locale is %s", res.getLocale().getLanguage());
        assertEquals("not disk", "disk", res.getString("disk"));
        assertEquals("not keyboard", "keyboard", res.getString("keyboard"));
    }

    @Test
    public void messageInGerman() {
        title("messageInGerman");
        Locale.setDefault(new Locale("de", "DE"));
        res = LionUtils.getBundledResource(LionUtils.class, "MyBundle");
        print("res locale is %s", res.getLocale().getLanguage());
        assertEquals("not DE-disk", "Platte", res.getString("disk"));
        assertEquals("not DE-keyboard", "Tastatur", res.getString("keyboard"));
    }
}
