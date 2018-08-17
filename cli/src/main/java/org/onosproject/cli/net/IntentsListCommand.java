/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.util.StringFilter;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.net.intent.WorkPartitionService;

import static com.google.common.base.MoreObjects.firstNonNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.text.WordUtils.uncapitalize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lists the inventory of intents and their states.
 */
@Service
@Command(scope = "onos", name = "intents",
         description = "Lists the inventory of intents and their states")
public class IntentsListCommand extends AbstractShellCommand {

    // Color codes and style
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    // Messages and string formatter
    private static final String APP_ID = BOLD + "Application Id:" + RESET + " %s";

    private static final String LEADER_ID = BOLD + "Leader Id:" + RESET + " %s";

    private static final String COMMON_SELECTOR = BOLD + "Common ingress " +
            "selector:" + RESET + " %s";

    private static final String CP = BOLD + "Connect Point:" + RESET + " %s";

    private static final String CONSTRAINTS = BOLD + "Constraints:" + RESET + " %s";

    private static final String DST = BOLD + "Destination " + RESET;

    private static final String EGRESS = BOLD + "Egress ";

    private static final String FILTERED_CPS = "connect points and individual selectors" + RESET;

    private static final String HOST = "host:" + RESET + " %s";

    private static final String ID = BOLD + "Id:" + RESET + " %s";

    private static final String INHERITED = "Inherited";

    private static final String INGRESS = BOLD + "Ingress ";

    private static final String INDENTATION = " -> ";

    private static final String INSTALLABLE = BOLD + "Installable:" + RESET + " %s";

    private static final String KEY = BOLD + "Key:" + RESET + " %s";

    private static final String NONE = "None";

    private static final String RESOURCES = BOLD + "Resources:" + RESET + " %s";

    private static final String SELECTOR = BOLD + "Selector:" + RESET + " %s";

    private static final String SEPARATOR = StringUtils.repeat("-", 172);

    private static final String SPACE = "   ";

    private static final String SRC = BOLD + "Source ";

    private static final String STATE = BOLD + "State:" + RESET + " %s";

    private static final String TREATMENT = BOLD + "Treatment:" + RESET + " %s";

    private static final String TYPE = BOLD + "Intent type:" + RESET + " %s";

    /**
     * {@value #SUMMARY_TITLES}.
     */
    private static final String SUMMARY_TITLES =
            BOLD + format(
            "%n%1s%21s%14s%14s%14s%14s%14s%14s%14s%14s%14s%14s",
            "Intent type",
            "Total",
            "Installed",
            "Withdrawn",
            "Failed",
            "InstallReq",
            "Compiling",
            "Installing",
            "Recompiling",
            "WithdrawReq",
            "Withdrawing",
            "UnknownState") +
            RESET;

    @Option(name = "-i", aliases = "--installable",
            description = "Output Installable Intents",
            required = false, multiValued = false)
    private boolean showInstallable = false;

    @Option(name = "-s", aliases = "--summary",
            description = "Intents summary",
            required = false, multiValued = false)
    private boolean intentsSummary = false;

    @Option(name = "-m", aliases = "--mini-summary",
            description = "Intents mini summary",
            required = false, multiValued = false)
    private boolean miniSummary = false;

    @Option(name = "-p", aliases = "--pending",
            description = "Show information about pending intents",
            required = false, multiValued = false)
    private boolean pending = false;

    @Option(name = "-d", aliases = "--details",
            description = "Show details for intents, filtered by ID",
            required = false, multiValued = true)
    private List<String> intentIds = new ArrayList<>();

    @Option(name = "-f", aliases = "--filter",
            description = "Filter intents by specific keyword",
            required = false, multiValued = true)
    private List<String> filter = new ArrayList<>();

    @Option(name = "-r", aliases = "--remove",
            description = "Remove and purge intents by specific keyword",
            required = false, multiValued = false)
    private String remove = null;

    private StringFilter contentFilter;
    private IntentService service;
    private WorkPartitionService workPartitionService;

    @Override
    protected void doExecute() {
        service = get(IntentService.class);
        workPartitionService = get(WorkPartitionService.class);

        if (workPartitionService == null) {
            return;
        }

        contentFilter = new StringFilter(filter, StringFilter.Strategy.AND);

        Iterable<Intent> intents;
        if (pending) {
            intents = service.getPending();
        } else {
            intents = service.getIntents();
        }

        // Remove intents
        if (remove != null && !remove.isEmpty()) {
            filter.add(remove);
            contentFilter = new StringFilter(filter, StringFilter.Strategy.AND);
            IntentRemoveCommand intentRemoveCmd = new IntentRemoveCommand();
            if (!remove.isEmpty()) {
                intentRemoveCmd.purgeIntentsInteractive(filterIntents(service));
            }
            return;
        }

        // Show detailed intents
        if (!intentIds.isEmpty()) {
            IntentDetailsCommand intentDetailsCmd = new IntentDetailsCommand();
            intentDetailsCmd.detailIntents(intentIds);
            return;
        }

        // Show brief intents
        if (intentsSummary || miniSummary) {
            Map<String, IntentSummary> summarized = summarize(intents);
            if (outputJson()) {
                ObjectNode summaries = mapper().createObjectNode();
                summarized.forEach((n, s) -> summaries.set(uncapitalize(n), s.json(mapper())));
                print("%s", summaries);
            } else if (miniSummary) {
                StringBuilder builder = new StringBuilder();
                builder.append(summarized.remove("All").miniSummary());
                summarized.values().forEach(s -> builder.append(s.miniSummary()));
                print("%s", builder.toString());
            } else {
                StringBuilder builder = new StringBuilder();
                builder.append(SUMMARY_TITLES);
                builder.append('\n').append(SEPARATOR);
                builder.append(summarized.remove("All").summary());
                summarized.values().forEach(s -> builder.append(s.summary()));
                print("%s", builder.toString());
            }
            return;
        }

        // JSON or default output
        if (outputJson()) {
            print("%s", json(intents));
        } else {
            for (Intent intent : intents) {
                IntentState state = service.getIntentState(intent.key());
                StringBuilder intentFormat = fullFormat(intent, state);
                StringBuilder detailsIntentFormat = detailsFormat(intent, state);
                String formatted = intentFormat.append(detailsIntentFormat).toString();
                if (contentFilter.filter(formatted)) {
                    print("%s\n", formatted);
                }
            }
        }
    }

    /**
     * Filter a given list of intents based on the existing content filter.
     *
     * @param service IntentService object
     * @return further filtered list of intents
     */
    private List<Intent> filterIntents(IntentService service) {
        return filterIntents(service.getIntents());
    }

    /**
     * Filter a given list of intents based on the existing content filter.
     *
     * @param intents Iterable of intents
     * @return further filtered list of intents
     */
    private List<Intent> filterIntents(Iterable<Intent> intents) {
        return Tools.stream(intents)
                .filter(i -> contentFilter.filter(i)).collect(Collectors.toList());
    }

    /**
     * Internal local class to keep track of a single type Intent summary.
     */
    private class IntentSummary {
        private final String intentType;
        private int total = 0;
        private int installReq = 0;
        private int compiling = 0;
        private int installing = 0;
        private int installed = 0;
        private int recompiling = 0;
        private int withdrawReq = 0;
        private int withdrawing = 0;
        private int withdrawn = 0;
        private int failed = 0;
        private int unknownState = 0;

        /**
         * Creates empty {@link IntentSummary} for specified {@code intentType}.
         *
         * @param intentType the string describing the Intent type
         */
        IntentSummary(String intentType) {
            this.intentType = intentType;
        }

        /**
         * Creates {@link IntentSummary} initialized with given {@code intent}.
         *
         * @param intent to initialize with
         */
        IntentSummary(Intent intent) {
            // remove "Intent" from intentType label
            this(intentType(intent));
            if (contentFilter.filter(intent)) {
                update(service.getIntentState(intent.key()));
            }
        }

        // for identity element, when reducing
        IntentSummary() {
            this.intentType = null;
        }

        /**
         * Updates the Intent Summary.
         *
         * @param intentState the state of the intent
         */
        void update(IntentState intentState) {
            total++;
            switch (intentState) {
            case INSTALL_REQ:
                installReq++;
                break;
            case COMPILING:
                compiling++;
                break;
            case INSTALLING:
                installing++;
                break;
            case INSTALLED:
                installed++;
                break;
            case RECOMPILING:
                recompiling++;
                break;
            case WITHDRAW_REQ:
                withdrawReq++;
                break;
            case WITHDRAWING:
                withdrawing++;
                break;
            case WITHDRAWN:
                withdrawn++;
                break;
            case FAILED:
                failed++;
                break;
            default:
                unknownState++;
                break;
            }
        }

        /**
         * Prints the Intent Summary.
         *
         */
        StringBuilder summary() {
            StringBuilder builder = new StringBuilder();

            builder.append(format(
                    "%n%1s%s%14d%14d%14d%14d%14d%14d%14d%14d%14d%14d",
                    BOLD + intentType + RESET,
                    Strings.padStart(String.valueOf(total),
                                     (32 - intentType.length()),
                                     ' '),
                    installed,
                    withdrawn,
                    failed,
                    installReq,
                    compiling,
                    installing,
                    recompiling,
                    withdrawReq,
                    withdrawing,
                    unknownState));
            builder.append('\n').append(SEPARATOR);

            return builder;
        }

        StringBuilder miniSummary() {
            StringBuilder builder = new StringBuilder();
            builder.append(BOLD).append(intentType).append(RESET)
                    .append(" (").append(total).append(')').append('\n');
            builder.append('\t')
                   .append("installed: ").append(installed).append(' ')
                   .append("withdrawn: ").append(withdrawn).append(' ')
                   .append("failed: ").append(failed)
                   .append('\n');
            builder.append('\t')
                   .append("compiling: ").append(compiling).append(' ')
                   .append("installing: ").append(installing).append(' ')
                   .append("recompiling: ").append(recompiling).append(' ')
                   .append("withdrawing: ").append(withdrawing)
                   .append('\n');
            builder.append('\t')
                   .append("installReq: ").append(installReq).append(' ')
                   .append("withdrawReq: ").append(withdrawReq).append(' ')
                   .append("unknownState: ").append(unknownState)
                   .append('\n')
                   .append('\n');
            return builder;
        }

        /**
         * Gets the JSON representation of the Intent Summary.
         *
         * @param mapper the object mapper
         * @return the JSON representation of the Intent Summary
         */
        JsonNode json(ObjectMapper mapper) {
            ObjectNode result = mapper.createObjectNode()
                .put("total", total)
                .put("installed", installed)
                .put("failed", failed)
                .put("installReq", installReq)
                .put("installing", installing)
                .put("compiling", compiling)
                .put("recompiling", recompiling)
                .put("withdrawReq", withdrawReq)
                .put("withdrawing", withdrawing)
                .put("withdrawn", withdrawn)
                .put("unknownState", unknownState);

            return result;
        }
    }

    /**
     * Merges 2 {@link IntentSummary} together.
     *
     * @param a element to merge
     * @param b element to merge
     * @return merged {@link IntentSummary}
     */
    IntentSummary merge(IntentSummary a, IntentSummary b) {
        IntentSummary m = new IntentSummary(firstNonNull(a.intentType, b.intentType));
        m.total         = a.total + b.total;
        m.installReq    = a.installReq + b.installReq;
        m.compiling     = a.compiling + b.compiling;
        m.installing    = a.installing + b.installing;
        m.installed     = a.installed + b.installed;
        m.recompiling   = a.recompiling + b.recompiling;
        m.withdrawing   = a.withdrawing + b.withdrawing;
        m.withdrawReq   = a.withdrawReq + b.withdrawReq;
        m.withdrawn     = a.withdrawn + b.withdrawn;
        m.failed        = a.failed + b.failed;
        m.unknownState  = a.unknownState + b.unknownState;
        return m;
    }

    /**
     * Returns IntentType string.
     *
     * @param intent input
     * @return IntentType string
     */
    private static String intentType(Intent intent) {
        return intent.getClass().getSimpleName().replace("Intent", "");
    }

    /**
     * Build summary of intents per intent type.
     *
     * @param intents to summarize
     * @return summaries per Intent type
     */
    private Map<String, IntentSummary> summarize(Iterable<Intent> intents) {
        Map<String, List<Intent>> perIntent = Tools.stream(intents)
            .collect(Collectors.groupingBy(IntentsListCommand::intentType));

        List<IntentSummary> collect = perIntent.values().stream()
            .map(il ->
                il.stream()
                    .map(IntentSummary::new)
                    .reduce(new IntentSummary(), this::merge)
            ).collect(Collectors.toList());

        Map<String, IntentSummary> summaries = new HashMap<>();

        // individual
        collect.forEach(is -> summaries.put(is.intentType, is));

        // all summarised
        summaries.put("All", collect.stream()
                              .reduce(new IntentSummary("All"), this::merge));
        return summaries;
    }

    /**
     * Returns detailed information text about a specific intent.
     *
     * @param intent to print
     * @param state of intent
     * @return detailed information or "" if {@code state} was null
     */
    private StringBuilder detailsFormat(Intent intent, IntentState state) {
        StringBuilder builder = new StringBuilder();
        if (state == null) {
            return builder;
        }
        if (!intent.resources().isEmpty()) {
            builder.append('\n').append(format(RESOURCES, intent.resources()));
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                builder.append('\n').append(format(COMMON_SELECTOR, formatSelector(ci.selector())));
            }
            if (!ci.treatment().allInstructions().isEmpty()) {
                builder.append('\n').append(format(TREATMENT, ci.treatment().allInstructions()));
            }
            if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                builder.append('\n').append(format(CONSTRAINTS, ci.constraints()));
            }
        }

        if (intent instanceof HostToHostIntent) {
            HostToHostIntent pi = (HostToHostIntent) intent;
            builder.append('\n').append(format(SRC + HOST, pi.one()));
            builder.append('\n').append(format(DST + HOST, pi.two()));
        } else if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            builder.append('\n').append(formatFilteredCps(Sets.newHashSet(pi.filteredIngressPoint()), INGRESS));
            builder.append('\n').append(formatFilteredCps(Sets.newHashSet(pi.filteredEgressPoint()), EGRESS));
        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            builder.append('\n').append(formatFilteredCps(pi.filteredIngressPoints(), INGRESS));
            builder.append('\n').append(formatFilteredCps(Sets.newHashSet(pi.filteredEgressPoint()), EGRESS));
        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            builder.append('\n').append(formatFilteredCps(Sets.newHashSet(pi.filteredIngressPoint()), INGRESS));
            builder.append('\n').append(formatFilteredCps(pi.filteredEgressPoints(), EGRESS));
        } else if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            builder.append(format("path=%s, cost=%f", pi.path().links(), pi.path().cost()));
        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            builder.append('\n').append(format("links=%s", li.links()));
            builder.append('\n').append(format(CP, li.egressPoints()));
        } else if (intent instanceof OpticalCircuitIntent) {
            OpticalCircuitIntent ci = (OpticalCircuitIntent) intent;
            builder.append('\n').append(format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
            builder.append('\n').append(format("signal type=%s", ci.getSignalType()));
            builder.append('\n').append(format("bidirectional=%s", ci.isBidirectional()));
        } else if (intent instanceof OpticalConnectivityIntent) {
            OpticalConnectivityIntent ci = (OpticalConnectivityIntent) intent;
            builder.append('\n').append(format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
            builder.append('\n').append(format("signal type=%s", ci.getSignalType()));
            builder.append('\n').append(format("bidirectional=%s", ci.isBidirectional()));
            builder.append('\n').append(format("ochSignal=%s", ci.ochSignal()));
        } else if (intent instanceof OpticalOduIntent) {
            OpticalOduIntent ci = (OpticalOduIntent) intent;
            builder.append('\n').append(format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
            builder.append('\n').append(format("signal type=%s", ci.getSignalType()));
            builder.append('\n').append(format("bidirectional=%s", ci.isBidirectional()));
        }

        List<Intent> installable = service.getInstallableIntents(intent.key())
            .stream().filter(i -> contentFilter.filter(i))
            .collect(Collectors.toList());
        if (showInstallable && installable != null && !installable.isEmpty()) {
            builder.append('\n').append(format(INSTALLABLE, installable));
        }
        return builder;
    }

    /*
     * Prints out a formatted string, given a list of connect points.
     */
    private StringBuilder formatFilteredCps(Set<FilteredConnectPoint> fCps, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(FILTERED_CPS);
        fCps.forEach(fCp -> builder.append('\n').append(formatFilteredCp(fCp)));

        return builder;
    }

    /*
     * Prints out a formatted string, given a filtered connect point.
     */
    private StringBuilder formatFilteredCp(FilteredConnectPoint fCp) {
        ConnectPoint connectPoint = fCp.connectPoint();
        TrafficSelector selector = fCp.trafficSelector();
        StringBuilder builder = new StringBuilder();
        builder.append(INDENTATION).append(format(CP, connectPoint));
        builder.append(SPACE).append(format(SELECTOR, formatSelector(selector)));

        return builder;
    }

    /*
     * Prints out a formatted string, given a traffic selector
     */
    private StringBuilder formatSelector(TrafficSelector ts) {
        StringBuilder builder = new StringBuilder();
        List<Criterion> criteria = Lists.newArrayList(ts.criteria());

        if (criteria == null || criteria.isEmpty()) {
            builder.append(INHERITED);
            return builder;
        }

        criteria.forEach(c -> {
            builder.append(c.toString());
            if (criteria.indexOf(c) < criteria.size() - 1) {
                builder.append(", ");
            }
        });

        return builder;
    }

    /*
     * Prints information about the intent state, given an intent.
     */
    private StringBuilder fullFormat(Intent intent, IntentState state) {
        StringBuilder builder = new StringBuilder();
        NodeId nodeId = workPartitionService.getLeader(intent.key(), Key::hash);

        builder.append(format(ID, intent.id()));
        if (state != null) {
            builder.append('\n').append(format(STATE, state));
        }
        builder.append('\n').append(format(KEY, intent.key()));
        builder.append('\n').append(format(TYPE, intent.getClass().getSimpleName()));
        builder.append('\n').append(format(APP_ID, intent.appId().name()));
        builder.append('\n').append(nodeId == null ? NONE : format(LEADER_ID, nodeId.id()));

        return builder;
    }

    /*
     * Produces a JSON array from the intents specified.
     */
    private JsonNode json(Iterable<Intent> intents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        Tools.stream(intents)
                .filter(intent -> contentFilter.filter(jsonForEntity(intent, Intent.class).toString()))
                .forEach(intent -> result.add(jsonForEntity(intent, Intent.class)));
        return result;
    }
}
