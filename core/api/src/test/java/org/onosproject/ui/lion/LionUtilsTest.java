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
import org.junit.Ignore;
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
    private Locale locale;

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


    // --- RUNTIME Locale ---
    @Test
    public void runtimeLocale() {
        title("runtimeLocale");
        Locale runtime = LionUtils.setupRuntimeLocale();
        print("locale is [%s]", runtime);

        // NOTE:
        //   Yeah, I know, "a unit test without asserts is not a unit test".
        //
        //   But it would NOT be a good idea to assert the locale results in
        //   this method, because that is dependent on an environment variable.
        //
        //   This method is here to allow manual verification of the Locale
        //   e.g. when running tests from IntelliJ, and setting the
        //   ONOS_LOCALE env.var. via the "Edit Configurations..." dialog.
    }


    // --- LOCALE from String ---
    @Test(expected = NullPointerException.class)
    public void localeFromStringNull() {
        LionUtils.localeFromString(null);
    }

    private void checkLanguageCountry(Locale locale, String expL, String expC) {
        assertEquals("Wrong language: " + expL, expL, locale.getLanguage());
        assertEquals("Wrong country: " + expC, expC, locale.getCountry());
    }

    @Test
    public void localeFromStringEmpty() {
        title("localeFromStringEmpty");
        locale = LionUtils.localeFromString("");
        checkLanguageCountry(locale, "", "");
    }

    @Test
    public void localeFromStringRu() {
        title("localeFromStringRu");
        locale = LionUtils.localeFromString("ru");
        checkLanguageCountry(locale, "ru", "");
    }

    @Test
    public void localeFromStringEnGB() {
        title("localeFromStringEnGB");
        locale = LionUtils.localeFromString("en_GB");
        checkLanguageCountry(locale, "en", "GB");
    }

    @Test
    public void localeFromStringItIT() {
        title("localeFromStringItIT");
        locale = LionUtils.localeFromString("it_IT");
        checkLanguageCountry(locale, "it", "IT");
    }

    @Test
    public void localeFromStringFrCA() {
        title("localeFromStringFrCA");
        locale = LionUtils.localeFromString("fr_CA");
        checkLanguageCountry(locale, "fr", "CA");
    }

    @Test
    public void localeFromStringKoKR() {
        title("localeFromStringKoKR");
        locale = LionUtils.localeFromString("ko_KR");
        checkLanguageCountry(locale, "ko", "KR");
    }


    // -- Testing loading of correct bundle, based on locale
    private void checkLookups(String computer, String disk, String monitor,
                              String keyboard) {
        res = LionUtils.getBundledResource(LionUtils.class, "MyBundle");
        print("res locale is %s", res.getLocale().getLanguage());
        print("a keyboard in this language is '%s'", res.getString("keyboard"));

        assertEquals("wrong computer", computer, res.getString("computer"));
        assertEquals("wrong disk", disk, res.getString("disk"));
        assertEquals("wrong monitor", monitor, res.getString("monitor"));
        assertEquals("wrong keyboard", keyboard, res.getString("keyboard"));
    }

    @Test
    public void messagesInEnglish() {
        title("messagesInEnglish");
        // use default locale
        checkLookups("computer", "disk", "monitor", "keyboard");
    }

    @Test
    public void messagesInGerman() {
        title("messagesInGerman");
        Locale.setDefault(Locale.GERMAN);
        checkLookups("Computer", "Platte", "Monitor", "Tastatur");
    }

    @Test
    public void messagesInItalian() {
        title("messagesInItalian");
        Locale.setDefault(Locale.ITALIAN);
        checkLookups("Calcolatore", "Disco", "Schermo", "Tastiera");
    }

    // TODO: figure out why extended character sets are not handled properly
    /*  For example, the Korean test fails as follows

        === messagesInKorean ===
        res locale is ko
        a keyboard in this language is 'í¤ë³´ë'

        org.junit.ComparisonFailure: wrong computer
        Expected :컴퓨터
        Actual   :ì»´í¨í°

     */

    @Test
    @Ignore("Not chinese friendly, yet...")
    public void messagesInZhTw() {
        title("messagesInZhTW");
        Locale.setDefault(Locale.TRADITIONAL_CHINESE);
        checkLookups("電腦", "磁碟", "螢幕", "鍵盤");
    }

    @Test
    @Ignore("Not korean friendly, yet...")
    public void messagesInKorean() {
        title("messagesInKorean");
        Locale.setDefault(Locale.KOREA);
        checkLookups("컴퓨터", "디스크", "모니터", "키보드");
    }

    @Test
    @Ignore("Not chinese friendly, yet...")
    public void messagesInZhCN() {
        title("messagesInZhCN");
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE);
        checkLookups("电脑", "磁盘", "屏幕", "键盘");
    }
}
