/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.ctl;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * String parser for the BMv2 table dump.
 */
public class Bmv2TableDumpParser {

    /*
    Example of BMv2 table dump:
    0: 0000 000000000000 000000000000 0806 &&& 0000000000000000000000000000ffff => send_to_cpu -

    For each entry, we want to match the id and all the rest.
     */
    private static final String ENTRY_PATTERN_STRING = "(\\d+):(.+)";
    private static final Pattern ENTRY_PATTERN = Pattern.compile(ENTRY_PATTERN_STRING);

    /**
     * Returns a list of entry Ids for the given table dump.
     *
     * @param tableDump a string value
     * @return a list of long values
     * @throws Bmv2TableDumpParserException if dump can't be parsed
     */
    public List<Long> getEntryIds(String tableDump) throws Bmv2TableDumpParserException {
        return parse(tableDump).stream().map(Pair::getKey).collect(Collectors.toList());
    }

    private List<Pair<Long, String>> parse(String tableDump) throws Bmv2TableDumpParserException {
        checkNotNull(tableDump, "tableDump cannot be null");

        List<Pair<Long, String>> results = Lists.newArrayList();

        // TODO: consider caching parser results for speed.

        Matcher matcher = ENTRY_PATTERN.matcher(tableDump);

        while (matcher.find()) {
            String entryString = matcher.group(1);
            if (entryString == null) {
                throw new Bmv2TableDumpParserException("Unable to parse entry for string: " + matcher.group());
            }
            Long entryId = -1L;
            try {
                entryId = Long.valueOf(entryString.trim());
            } catch (NumberFormatException e) {
                throw new Bmv2TableDumpParserException("Unable to parse entry id for string: " + matcher.group());
            }
            String allTheRest = matcher.group(2);
            if (allTheRest == null) {
                throw new Bmv2TableDumpParserException("Unable to parse entry for string: " + matcher.group());
            }
            results.add(Pair.of(entryId, allTheRest));
        }

        return results;
    }

    public class Bmv2TableDumpParserException extends Throwable {
        public Bmv2TableDumpParserException(String msg) {
            super(msg);
        }
    }
}
