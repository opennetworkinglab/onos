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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.HexString;
import org.onlab.util.ImmutableByteSequence;
import org.onlab.util.SharedScheduledExecutors;
import org.onosproject.bmv2.api.context.Bmv2ActionModel;
import org.onosproject.bmv2.api.context.Bmv2Configuration;
import org.onosproject.bmv2.api.runtime.Bmv2Action;
import org.onosproject.bmv2.api.runtime.Bmv2ExactMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2LpmMatchParam;
import org.onosproject.bmv2.api.runtime.Bmv2MatchKey;
import org.onosproject.bmv2.api.runtime.Bmv2ParsedTableEntry;
import org.onosproject.bmv2.api.runtime.Bmv2TernaryMatchParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.fitByteSequence;
import static org.onosproject.bmv2.api.utils.Bmv2TranslatorUtils.ByteSequenceFitException;

/**
 * BMv2 table dump parser.
 */
public final class Bmv2TableDumpParser {

    // Examples of a BMv2 table dump can be found in Bmv2TableDumpParserTest

    // 1: entry id, 2: match string, 3: action string
    private static final String ENTRY_PATTERN_REGEX = "(\\d+): (.*) => (.*)";
    // 1: match values, 2: masks
    private static final String MATCH_TERNARY_PATTERN_REGEX = "([0-9a-fA-F ]+) &&& ([0-9a-fA-F ]+)";
    // 1: match values, 2: masks
    private static final String MATCH_LPM_PATTERN_REGEX = "([0-9a-fA-F ]+) / ([0-9a-fA-F ]+)";
    // 1: match values
    private static final String MATCH_EXACT_PATTERN_REGEX = "([0-9a-fA-F ]+)";
    // 1: action name, 2: action params
    private static final String ACTION_PATTERN_REGEX = "(.+) - ?([0-9a-fA-F ,]*)";

    private static final Pattern ENTRY_PATTERN = Pattern.compile(ENTRY_PATTERN_REGEX);
    private static final Pattern MATCH_TERNARY_PATTERN = Pattern.compile(MATCH_TERNARY_PATTERN_REGEX);
    private static final Pattern MATCH_LPM_PATTERN = Pattern.compile(MATCH_LPM_PATTERN_REGEX);
    private static final Pattern MATCH_EXACT_PATTERN = Pattern.compile(MATCH_EXACT_PATTERN_REGEX);
    private static final Pattern ACTION_PATTERN = Pattern.compile(ACTION_PATTERN_REGEX);

    // Cache to avoid re-parsing known lines.
    // The assumption here is that entries are not updated too frequently, so that the entry id doesn't change often.
    // Otherwise, we should cache only the match and action strings...
    private static final LoadingCache<Pair<String, Bmv2Configuration>, Optional<Bmv2ParsedTableEntry>> ENTRY_CACHE =
            CacheBuilder.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .recordStats()
            .build(new CacheLoader<Pair<String, Bmv2Configuration>, Optional<Bmv2ParsedTableEntry>>() {
                @Override
                public Optional<Bmv2ParsedTableEntry> load(Pair<String, Bmv2Configuration> key) throws Exception {
                    // Very expensive call.
                    return Optional.ofNullable(parseLine(key.getLeft(), key.getRight()));
                }
            });

    private static final Logger log = LoggerFactory.getLogger(Bmv2TableDumpParser.class);

    private static final long STATS_LOG_FREQUENCY = 3; // minutes

    static {
        SharedScheduledExecutors.getSingleThreadExecutor().scheduleAtFixedRate(
                () -> reportStats(), 0, STATS_LOG_FREQUENCY, TimeUnit.MINUTES);
    }

    private Bmv2TableDumpParser() {
        // Ban constructor.
    }

    /**
     * Parse the given BMv2 table dump.
     *
     * @param tableDump a string value
     * @return a list of {@link Bmv2ParsedTableEntry}
     */
    public static List<Bmv2ParsedTableEntry> parse(String tableDump, Bmv2Configuration configuration) {
        checkNotNull(tableDump, "tableDump cannot be null");
        // Parse all lines
        List<Bmv2ParsedTableEntry> result = Arrays.stream(tableDump.split("\n"))
                .map(line -> Pair.of(line, configuration))
                .map(Bmv2TableDumpParser::loadFromCache)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        return result;
    }

    private static Optional<Bmv2ParsedTableEntry> loadFromCache(Pair<String, Bmv2Configuration> key) {
        try {
            return ENTRY_CACHE.get(key);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            if (t instanceof Bmv2TableDumpParserException) {
                Bmv2TableDumpParserException parserException = (Bmv2TableDumpParserException) t;
                log.warn("{}", parserException.getMessage());
            } else {
                log.error("Exception while parsing table dump line", e);
            }
            return Optional.empty();
        }
    }

    private static void reportStats() {
        CacheStats stats = ENTRY_CACHE.stats();
        log.info("Cache stats: requestCount={}, hitRate={}, exceptionsCount={}, avgLoadPenalty={}",
                 stats.requestCount(), stats.hitRate(), stats.loadExceptionCount(), stats.averageLoadPenalty());
    }

    private static Bmv2ParsedTableEntry parseLine(String line, Bmv2Configuration configuration)
            throws Bmv2TableDumpParserException {
        Matcher matcher = ENTRY_PATTERN.matcher(line);
        if (matcher.find()) {
            long entryId = parseEntryId(matcher, 1);
            String matchString = parseMatchString(matcher, 2);
            String actionString = parseActionString(matcher, 3);
            Bmv2MatchKey matchKey = parseMatchKey(matchString);
            Bmv2Action action = parseAction(actionString, configuration);
            return new Bmv2ParsedTableEntry(entryId, matchKey, action);
        } else {
            // Not a table entry
            return null;
        }
    }

    private static Long parseEntryId(Matcher matcher, int groupIdx) throws Bmv2TableDumpParserException {
        String str = matcher.group(groupIdx);
        if (str == null) {
            throw new Bmv2TableDumpParserException("Unable to find entry ID: " + matcher.group());
        }
        long entryId;
        try {
            entryId = Long.valueOf(str.trim());
        } catch (NumberFormatException e) {
            throw new Bmv2TableDumpParserException("Unable to parse entry id for string: " + matcher.group());
        }
        return entryId;
    }

    private static String parseMatchString(Matcher matcher, int groupIdx) throws Bmv2TableDumpParserException {
        String str = matcher.group(groupIdx);
        if (str == null) {
            throw new Bmv2TableDumpParserException("Unable to find match string: " + matcher.group());
        }
        return str.trim();
    }

    private static String parseActionString(Matcher matcher, int groupIdx) throws Bmv2TableDumpParserException {
        String str = matcher.group(groupIdx);
        if (str == null) {
            throw new Bmv2TableDumpParserException("Unable to find action string: " + matcher.group());
        }
        return str.trim();
    }

    private static Bmv2MatchKey parseMatchKey(String str) throws Bmv2TableDumpParserException {

        Bmv2MatchKey.Builder builder = Bmv2MatchKey.builder();

        // Try with ternary...
        Matcher matcher = MATCH_TERNARY_PATTERN.matcher(str);
        if (matcher.find()) {
            // Ternary Match.
            List<ImmutableByteSequence> values = parseMatchValues(matcher, 1);
            List<ImmutableByteSequence> masks = parseMatchMasks(matcher, 2, values);
            for (int i = 0; i < values.size(); i++) {
                builder.add(new Bmv2TernaryMatchParam(values.get(i), masks.get(i)));
            }
            return builder.build();
        }

        // FIXME: LPM match parsing broken if table key contains also a ternary match
        // Also it assumes the lpm parameter is the last one, which is wrong.
        // Try with LPM...
        matcher = MATCH_LPM_PATTERN.matcher(str);
        if (matcher.find()) {
            // Lpm Match.
            List<ImmutableByteSequence> values = parseMatchValues(matcher, 1);
            int prefixLength = parseLpmPrefix(matcher, 2);
            for (int i = 0; i < values.size() - 1; i++) {
                builder.add(new Bmv2ExactMatchParam(values.get(i)));
            }
            builder.add(new Bmv2LpmMatchParam(values.get(values.size() - 1), prefixLength));
            return builder.build();
        }

        // Try with exact...
        matcher = MATCH_EXACT_PATTERN.matcher(str);
        if (matcher.find()) {
            // Exact match.
            parseMatchValues(matcher, 1)
                    .stream()
                    .map(Bmv2ExactMatchParam::new)
                    .forEach(builder::add);
            return builder.build();
        }

        throw new Bmv2TableDumpParserException("Unable to parse match string: " + str);
    }

    private static List<ImmutableByteSequence> parseMatchValues(Matcher matcher, int groupIdx)
            throws Bmv2TableDumpParserException {
        String matchString = matcher.group(groupIdx);
        if (matchString == null) {
            throw new Bmv2TableDumpParserException("Unable to find match params for string: " + matcher.group());
        }
        List<ImmutableByteSequence> result = Lists.newArrayList();
        for (String paramString : matchString.split(" ")) {
            byte[] bytes = HexString.fromHexString(paramString, null);
            result.add(ImmutableByteSequence.copyFrom(bytes));
        }
        return result;
    }

    private static List<ImmutableByteSequence> parseMatchMasks(Matcher matcher, int groupIdx,
                                                               List<ImmutableByteSequence> matchParams)
            throws Bmv2TableDumpParserException {
        String maskString = matcher.group(groupIdx);
        if (maskString == null) {
            throw new Bmv2TableDumpParserException("Unable to find mask for string: " + matcher.group());
        }
        List<ImmutableByteSequence> result = Lists.newArrayList();
        /*
        Mask here is a hex string with no spaces, hence individual mask params can be derived according
        to given matchParam sizes.
         */
        byte[] maskBytes = HexString.fromHexString(maskString, null);
        int startPosition = 0;
        for (ImmutableByteSequence bs : matchParams) {
            if (startPosition + bs.size() > maskBytes.length) {
                throw new Bmv2TableDumpParserException("Invalid length for mask in string: " + matcher.group());
            }
            ImmutableByteSequence maskParam = ImmutableByteSequence.copyFrom(maskBytes,
                                                                             startPosition,
                                                                             startPosition + bs.size() - 1);
            result.add(maskParam);
            startPosition += bs.size();
        }
        return result;
    }

    private static int parseLpmPrefix(Matcher matcher, int groupIdx)
            throws Bmv2TableDumpParserException {
        String str = matcher.group(groupIdx);
        if (str == null) {
            throw new Bmv2TableDumpParserException("Unable to find LPM prefix for string: " + matcher.group());
        }
        // For some reason the dumped prefix has 16 bits more than the one programmed
        try {
            return Integer.valueOf(str.trim()) - 16;
        } catch (NumberFormatException e) {
            throw new Bmv2TableDumpParserException("Unable to parse LPM prefix from string: " + matcher.group());
        }
    }

    private static Bmv2Action parseAction(String str, Bmv2Configuration configuration)
            throws Bmv2TableDumpParserException {
        Matcher matcher = ACTION_PATTERN.matcher(str);
        if (matcher.find()) {
            String actionName = parseActionName(matcher, 1);
            Bmv2ActionModel actionModel = configuration.action(actionName);
            if (actionModel == null) {
                throw new Bmv2TableDumpParserException("Not such an action in configuration: " + actionName);
            }
            Bmv2Action.Builder builder = Bmv2Action.builder().withName(actionName);
            List<ImmutableByteSequence> actionParams = parseActionParams(matcher, 2);
            if (actionParams.size() != actionModel.runtimeDatas().size()) {
                throw new Bmv2TableDumpParserException("Invalid number of parameters for action: " + actionName);
            }
            for (int i = 0; i < actionModel.runtimeDatas().size(); i++) {
                try {
                    // fit param byte-width according to configuration.
                    builder.addParameter(fitByteSequence(actionParams.get(i),
                                                         actionModel.runtimeDatas().get(i).bitWidth()));
                } catch (ByteSequenceFitException e) {
                    throw new Bmv2TableDumpParserException("Unable to parse action param: " + e.toString());
                }
            }
            return builder.build();
        }
        throw new Bmv2TableDumpParserException("Unable to parse action string: " + str.trim());
    }

    private static String parseActionName(Matcher matcher, int groupIdx) throws Bmv2TableDumpParserException {
        String actionName = matcher.group(groupIdx);
        if (actionName == null) {
            throw new Bmv2TableDumpParserException("Unable to find action name for string: " + matcher.group());
        }
        return actionName.trim();
    }

    private static List<ImmutableByteSequence> parseActionParams(Matcher matcher, int groupIdx)
            throws Bmv2TableDumpParserException {
        String paramsString = matcher.group(groupIdx);
        if (paramsString == null) {
            throw new Bmv2TableDumpParserException("Unable to find action params for string: " + matcher.group());
        }
        if (paramsString.length() == 0) {
            return Collections.emptyList();
        }
        return Arrays.stream(paramsString.split(","))
                .map(String::trim)
                .map(s -> HexString.fromHexString(s, null))
                .map(ImmutableByteSequence::copyFrom)
                .collect(Collectors.toList());
    }

    public static class Bmv2TableDumpParserException extends Exception {
        public Bmv2TableDumpParserException(String msg) {
            super(msg);
        }
    }
}