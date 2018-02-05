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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.io.CharStreams;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A Java representation of a lion configuration file. You can create one with
 * something like the following:
 * <pre>
 *     String filepath = "/path/to/some/file.lioncfg";
 *     LionConfig cfg = new LionConfig().load(filepath);
 * </pre>
 */
public class LionConfig {
    private static final Pattern RE_COMMENT = Pattern.compile("^\\s*#.*");
    private static final Pattern RE_BLANK = Pattern.compile("^\\s*$");

    static final Pattern RE_IMPORT =
            Pattern.compile("^(\\S+)\\s+import\\s+(.*)$");

    private static final String BUNDLE = "bundle";
    private static final String ALIAS = "alias";
    private static final String FROM = "from";
    private static final String STAR = "*";
    private static final char SPC = ' ';
    private static final char DOT = '.';

    private List<String> lines;
    private List<String> badLines;

    private CmdBundle bundle;
    private final Set<CmdAlias> aliases = new TreeSet<>();
    private final Set<CmdFrom> froms = new TreeSet<>();

    private Map<String, String> aliasMap;
    private Map<String, Set<String>> fromMap;

    /**
     * Loads in the specified file and attempts to parse it as a
     * {@code .lioncfg} format file.
     *
     * @param source path to .lioncfg file
     * @return the instance
     * @throws IllegalArgumentException if there is a problem reading the file
     */
    public LionConfig load(String source) {
        try (Reader r = new InputStreamReader(getClass().getResourceAsStream(source),
                                              UTF_8)) {
            lines = CharStreams.readLines(r);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read: " + source, e);
        }

        stripCommentsAndWhitespace();
        parse();
        processAliases();
        processFroms();

        return this;
    }


    private boolean isCommentOrBlank(String s) {
        return RE_COMMENT.matcher(s).matches() || RE_BLANK.matcher(s).matches();
    }


    private void stripCommentsAndWhitespace() {
        if (lines != null) {
            lines.removeIf(this::isCommentOrBlank);
        }
    }

    private void parse() {
        badLines = new ArrayList<>();

        lines.forEach(l -> {
            int i = l.indexOf(SPC);
            if (i < 1) {
                badLines.add(l);
                return;
            }
            String keyword = l.substring(0, i);
            String params = l.substring(i + 1);

            switch (keyword) {
                case BUNDLE:
                    CmdBundle cb = new CmdBundle(l, params);

                    if (bundle != null) {
                        // we can only declare the bundle once
                        badLines.add(l);
                    } else {
                        bundle = cb;
                    }
                    break;

                case ALIAS:
                    CmdAlias ca = new CmdAlias(l, params);
                    if (ca.malformed) {
                        badLines.add(l);
                    } else {
                        aliases.add(ca);
                    }
                    break;

                case FROM:
                    CmdFrom cf = new CmdFrom(l, params);
                    if (cf.malformed) {
                        badLines.add(l);
                    } else {
                        froms.add(cf);
                    }
                    break;

                default:
                    badLines.add(l);
                    break;
            }
        });
    }

    private void processAliases() {
        aliasMap = new HashMap<>(aliasCount());
        aliases.forEach(a -> aliasMap.put(a.alias, a.subst));
    }

    private void processFroms() {
        fromMap = new HashMap<>(fromCount());
        froms.forEach(f -> {
            f.expandAliasIfAny(aliasMap);
            if (singleStarCheck(f)) {
                fromMap.put(f.expandedRes, f.keys);
            } else {
                badLines.add(f.orig);
            }
        });
    }

    private boolean singleStarCheck(CmdFrom from) {
        from.starred = false;
        Set<String> keys = from.keys();
        for (String k : keys) {
            if (STAR.equals(k)) {
                from.starred = true;
            }
        }
        return !from.starred || keys.size() == 1;
    }

    @Override
    public String toString() {
        int nlines = lines == null ? 0 : lines.size();
        return String.format("LionConfig{#lines=%d}", nlines);
    }

    /**
     * Returns the configured bundle ID for this config.
     *
     * @return the bundle ID
     */
    String id() {
        return bundle == null ? null : bundle.id;
    }

    /**
     * Returns the number of aliases configured in this config.
     *
     * @return the alias count
     */
    int aliasCount() {
        return aliases.size();
    }

    /**
     * Returns the number of from...import lines configured in this config.
     *
     * @return the number of from...import lines
     */
    int fromCount() {
        return froms.size();
    }

    /**
     * Returns the substitution string for the given alias.
     *
     * @param a the alias
     * @return the substitution
     */
    String alias(String a) {
        return aliasMap.get(a);
    }

    /**
     * Returns the number of keys imported from the specified resource.
     *
     * @param res the resource
     * @return number of keys imported from that resource
     */
    int fromKeyCount(String res) {
        Set<String> keys = fromMap.get(res);
        return keys == null ? 0 : keys.size();
    }

    /**
     * Returns true if the specified resource exists and contains the
     * given key.
     *
     * @param res the resource
     * @param key the key
     * @return true, if resource exists and contains the key; false otherwise
     */
    boolean fromContains(String res, String key) {
        Set<String> keys = fromMap.get(res);
        return keys != null && keys.contains(key);
    }

    /**
     * Returns the set of (expanded) "from" entries in this configuration.
     *
     * @return the entries
     */
    public Set<CmdFrom> entries() {
        return froms;
    }

    /**
     * Returns the number of parse errors detected.
     *
     * @return number of bad lines
     */
    public int errorCount() {
        return badLines.size();
    }

    /**
     * Returns the lines that failed the parser.
     *
     * @return the erroneous lines in the config
     */
    public List<String> errorLines() {
        return ImmutableList.copyOf(badLines);
    }

    // ==== Mini class hierarchy of command types

    private abstract static class Cmd {
        final String orig;
        boolean malformed = false;

        Cmd(String orig) {
            this.orig = orig;
        }

        /**
         * Returns the original string from the configuration file.
         *
         * @return original from string
         */
        public String orig() {
            return orig;
        }
    }

    private static final class CmdBundle extends Cmd {
        private final String id;

        private CmdBundle(String orig, String params) {
            super(orig);
            id = params;
        }

        @Override
        public String toString() {
            return "CmdBundle{id=\"" + id + "\"}";
        }
    }

    private static final class CmdAlias extends Cmd
            implements Comparable<CmdAlias> {
        private final String alias;
        private final String subst;

        private CmdAlias(String orig, String params) {
            super(orig);
            int i = params.indexOf(SPC);
            if (i < 1) {
                malformed = true;
                alias = null;
                subst = null;
            } else {
                alias = params.substring(0, i);
                subst = params.substring(i + 1);
            }
        }

        @Override
        public String toString() {
            return "CmdAlias{alias=\"" + alias + "\", subst=\"" + subst + "\"}";
        }

        @Override
        public int compareTo(CmdAlias o) {
            return alias.compareTo(o.alias);
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CmdAlias that = (CmdAlias) obj;
            return Objects.equal(this.alias, that.alias);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.alias);
        }
    }

    /**
     * Represents a "from {res} import {stuff}" command in the configuration.
     */
    public static final class CmdFrom extends Cmd
            implements Comparable<CmdFrom> {
        private final String rawRes;
        private final Set<String> keys;
        private String expandedRes;
        private boolean starred = false;

        private CmdFrom(String orig, String params) {
            super(orig);
            Matcher m = RE_IMPORT.matcher(params);
            if (!m.matches()) {
                malformed = true;
                rawRes = null;
                keys = null;
            } else {
                rawRes = m.group(1);
                keys = genKeys(m.group(2));
            }
        }

        private Set<String> genKeys(String keys) {
            String[] k = keys.split("\\s*,\\s*");
            Set<String> allKeys = new HashSet<>();
            Collections.addAll(allKeys, k);
            return ImmutableSortedSet.copyOf(allKeys);
        }

        private void expandAliasIfAny(Map<String, String> aliases) {
            String expanded = rawRes;
            int i = rawRes.indexOf(DOT);
            if (i > 0) {
                String alias = rawRes.substring(0, i);
                String sub = aliases.get(alias);
                if (sub != null) {
                    expanded = sub + rawRes.substring(i);
                }
            }
            expandedRes = expanded;
        }

        @Override
        public String toString() {
            return "CmdFrom{res=\"" + rawRes + "\", keys=" + keys + "}";
        }

        @Override
        public int compareTo(CmdFrom o) {
            return rawRes.compareTo(o.rawRes);
        }

        @Override
        public boolean equals(Object obj) {

            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final CmdFrom that = (CmdFrom) obj;
            return Objects.equal(this.rawRes, that.rawRes);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(this.rawRes);
        }

        /**
         * Returns the resource bundle name from which to import things.
         *
         * @return the resource bundle name
         */
        public String res() {
            return expandedRes;
        }

        /**
         * Returns the set of keys which should be imported.
         *
         * @return the keys to import
         */
        public Set<String> keys() {
            return keys;
        }

        /**
         * Returns true if this "from" command is importing ALL keys from
         * the specified resource; false otherwise.
         *
         * @return true, if importing ALL keys; false otherwise
         */
        public boolean starred() {
            return starred;
        }
    }
}
