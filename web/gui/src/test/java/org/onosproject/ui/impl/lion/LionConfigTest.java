/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ui.impl.lion;

import org.junit.Test;
import org.onosproject.ui.AbstractUiTest;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link LionConfig}.
 */
public class LionConfigTest extends AbstractUiTest {

    private static final String ROOT = "/org/onosproject/ui/lion/";
    private static final String CMD_ROOT = ROOT + "_cmd/";
    private static final String CONFIG_ROOT = ROOT + "_config/";

    private static final String SUFFIX = ".lioncfg";

    private static final String CARD_GAME_1 = CONFIG_ROOT + "CardGame1" + SUFFIX;

    private LionConfig cfg;
    private LionConfig.CmdFrom from;


    private String cmdPath(String name) {
        return CMD_ROOT + name + SUFFIX;
    }

    private String configPath(String name) {
        return CONFIG_ROOT + name + SUFFIX;
    }

    private LionConfig cfg() {
        return new LionConfig();
    }

    private void verifyStats(String expId, int expAliases, int expFroms) {
        assertEquals("wrong bundle ID", expId, cfg.id());
        assertEquals("wrong alias count", expAliases, cfg.aliasCount());
        assertEquals("wrong from count", expFroms, cfg.fromCount());
    }

    private void verifyKeys(String res, String... keys) {
        int nkeys = keys.length;
        for (String k: keys) {
            assertEquals("key not found: " + k, true, cfg.fromContains(res, k));
        }
        assertEquals("wrong key count", nkeys, cfg.fromKeyCount(res));
    }

    @Test
    public void importMatch() {
        title("importMatch");
        String fromParams = "cs.rank import *";
        Matcher m = LionConfig.RE_IMPORT.matcher(fromParams);
        assertEquals("no match", true, m.matches());
        assertEquals("bad group 1", "cs.rank", m.group(1));
        assertEquals("bad group 2", "*", m.group(2));
    }

    @Test
    public void basic() {
        title("basic");
        cfg = cfg().load(CARD_GAME_1);
        print(cfg);
        verifyStats("CardGame1", 1, 3);
    }

    @Test
    public void cmd01GoodBundle() {
        title("cmd01GoodBundle");
        cfg = cfg().load(cmdPath("01-bundle"));
        verifyStats("foo.bar", 0, 0);
        assertEquals("wrong ID", "foo.bar", cfg.id());
    }

    @Test
    public void cmd02GoodAlias() {
        title("cmd02GoodAlias");
        cfg = cfg().load(cmdPath("02-alias"));
        verifyStats(null, 1, 0);
        assertEquals("alias/subst not found", "xyzzy.wizard", cfg.alias("xy"));
    }

    @Test
    public void cmd03GoodFrom() {
        title("cmd03GoodFrom");
        cfg = cfg().load(cmdPath("03-from"));
        verifyStats(null, 0, 1);
        assertEquals("from/keys bad count", 0, cfg.fromKeyCount("non.exist"));
        assertEquals("from/keys bad count", 1, cfg.fromKeyCount("foo.bar"));
        assertEquals("from/keys not found", true,
                     cfg.fromContains("foo.bar", "fizzbuzz"));

        from = cfg.entries().iterator().next();
        assertFalse("expected no star", from.starred());
    }

    @Test
    public void cmd04GoodFromFour() {
        title("cmd04GoodFromFour");
        cfg = cfg().load(cmdPath("04-from-four"));
        assertEquals("from/keys bad count", 4, cfg.fromKeyCount("hooray"));
        verifyKeys("hooray", "ford", "arthur", "joe", "henry");
    }

    @Test
    public void cmd05FromExpand() {
        title("cmd05FromExpand");
        cfg = cfg().load(cmdPath("05-from-expand"));
        assertEquals("no expand 1", 0, cfg.fromKeyCount("xy.spell"));
        assertEquals("no expand 2", 0, cfg.fromKeyCount("xy.learn"));
        verifyKeys("xyzzy.wizard.spell", "zonk", "zip", "zuffer");
        verifyKeys("xyzzy.wizard.learn", "zap", "zigzag");
    }

    @Test
    public void cmd06FromStar() {
        title("cmd06FromStar");
        cfg = cfg().load(cmdPath("06-from-star"));
        print(cfg);
        assertEquals("bad from count", 1, cfg.fromCount());

        from = cfg.entries().iterator().next();
        assertTrue("expected a star", from.starred());
    }

    @Test
    public void cmd07StarIsSpecial() {
        title("cmd06StarIsSpecial");
        cfg = cfg().load(cmdPath("07-star-is-special"));
        print(cfg);
        assertEquals("no error detected", 3, cfg.errorCount());

        int iBad = 0;
        for (String line: cfg.errorLines()) {
            print(line);
            iBad++;
            String prefix = "from star.bad" + iBad + " import ";
            assertTrue("unexpected bad line", line.startsWith(prefix));
        }
    }

    @Test
    public void cardGameConfig() {
        title("cardGameConfig");
        cfg = cfg().load(configPath("CardGame1"));
        assertEquals("wrong id", "CardGame1", cfg.id());
        assertEquals("wrong alias count", 1, cfg.aliasCount());
        assertEquals("wrong from count", 3, cfg.fromCount());

        verifyKeys("app.Cards", "*");
        verifyKeys("core.stuff.Rank", "ten", "jack", "queen", "king", "ace");
        verifyKeys("core.stuff.Suit", "spades", "clubs");
    }
}
