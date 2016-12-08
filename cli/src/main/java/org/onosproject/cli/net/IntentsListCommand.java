/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.StringFilter;
import org.onosproject.cli.AbstractShellCommand;
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
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.OpticalOduIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

/**
 * Lists the inventory of intents and their states.
 */
@Command(scope = "onos", name = "intents",
         description = "Lists the inventory of intents and their states")
public class IntentsListCommand extends AbstractShellCommand {

    // Color codes and style
    private static final String BOLD = "\u001B[1m";
    private static final String RESET = "\u001B[0m";

    // Messages and string formatter
    private static final String APP_ID = BOLD + "Application Id:" + RESET + " %s";

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

    private static final String RESOURCES = BOLD + "Resources:" + RESET + " %s";

    private static final String SELECTOR = BOLD + "Selector:" + RESET + " %s";

    private static final String SEPARATOR = StringUtils.repeat("-", 172);;

    private static final String SPACE = "   ";

    private static final String SRC = BOLD + "Source ";

    private static final String STATE = BOLD + "State:" + RESET + " %s";

    private static final String TREATMENT = BOLD + "Treatment:" + RESET + " %s";

    private static final String TYPE = BOLD + "Intent type:" + RESET + " %s";

    private static final String SUMMARY_TITLES =
            BOLD + String.format(
            "\n%1s%21s%14s%14s%14s%14s%14s%14s%14s%14s%14s%14s",
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

    @Option(name = "-p", aliases = "--pending",
            description = "Show information about pending intents",
            required = false, multiValued = false)
    private boolean pending = false;

    @Option(name = "-f", aliases = "--filter",
            description = "Filter intents by specific keyword",
            required = false, multiValued = true)
    private List<String> filter = new ArrayList<>();

    private StringFilter contentFilter;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);
        contentFilter = new StringFilter(filter, StringFilter.Strategy.AND);

        if (intentsSummary) {
            IntentSummaries intentSummaries = new IntentSummaries();
            intentSummaries.collectIntentSummary(service,
                                                 service.getIntents());
            if (outputJson()) {
                print("%s", intentSummaries.json());
            } else {
                print(intentSummaries.summary());
            }
            return;
        } else if (pending) {
            if (outputJson()) {
                print("%s", json(service.getPending()));
            } else {
                StreamSupport.stream(service.getPending().spliterator(), false)
                        .filter(intent -> contentFilter.filter(intent))
                        .forEach(intent -> print(fullFormat(intent)));
            }
            return;
        }

        if (outputJson()) {
            print("%s", json(service.getIntents()));
        } else {
            printIntents(service);
        }
    }

    /**
     * Internal local class to keep track of all intent summaries.
     */
    private class IntentSummaries {
        private IntentSummary summaryAll;
        private IntentSummary summaryConnectivity;
        private IntentSummary summaryHostToHost;
        private IntentSummary summaryPointToPoint;
        private IntentSummary summaryMultiPointToSinglePoint;
        private IntentSummary summarySinglePointToMultiPoint;
        private IntentSummary summaryPath;
        private IntentSummary summaryLinkCollection;
        private IntentSummary summaryOpticalCircuit;
        private IntentSummary summaryOpticalConnectivity;
        private IntentSummary summaryOpticalOdu;
        private IntentSummary summaryUnknownType;

        /**
         * Initializes the internal summary.
         */
        private void init() {
            summaryAll = new IntentSummary("All");
            summaryConnectivity = new IntentSummary("Connectivity");
            summaryHostToHost = new IntentSummary("HostToHost");
            summaryPointToPoint = new IntentSummary("PointToPoint");
            summaryMultiPointToSinglePoint =
                new IntentSummary("MultiPointToSinglePoint");
            summarySinglePointToMultiPoint =
                new IntentSummary("SinglePointToMultiPoint");
            summaryPath = new IntentSummary("Path");
            summaryLinkCollection = new IntentSummary("LinkCollection");
            summaryOpticalCircuit = new IntentSummary("OpticalCircuit");
            summaryOpticalConnectivity = new IntentSummary("OpticalConnectivity");
            summaryOpticalOdu = new IntentSummary("OpticalOdu");
            summaryUnknownType = new IntentSummary("UnknownType");
        }

        /**
         * Collects summary of all intents.
         *
         * @param service the Intent Service to use
         * @param intents the intents
         */
        private void collectIntentSummary(IntentService service,
                                          Iterable<Intent> intents) {
            init();

            // Collect the summary for each intent type intents
            for (Intent intent : intents) {
                IntentState intentState = service.getIntentState(intent.key());
                if (intentState == null) {
                    continue;
                }
                if (!contentFilter.filter(intent)) {
                    break;
                }

                // Update the summary for all Intents
                summaryAll.update(intentState);

                if (intent instanceof ConnectivityIntent) {
                    summaryConnectivity.update(intentState);
                    // NOTE: ConnectivityIntent is a base type Intent
                    // continue;
                }
                if (intent instanceof HostToHostIntent) {
                    summaryHostToHost.update(intentState);
                    continue;
                }
                if (intent instanceof PointToPointIntent) {
                    summaryPointToPoint.update(intentState);
                    continue;
                }
                if (intent instanceof MultiPointToSinglePointIntent) {
                    summaryMultiPointToSinglePoint.update(intentState);
                    continue;
                }
                if (intent instanceof SinglePointToMultiPointIntent) {
                    summarySinglePointToMultiPoint.update(intentState);
                    continue;
                }
                if (intent instanceof PathIntent) {
                    summaryPath.update(intentState);
                    continue;
                }
                if (intent instanceof LinkCollectionIntent) {
                    summaryLinkCollection.update(intentState);
                    continue;
                }
                if (intent instanceof OpticalCircuitIntent) {
                    summaryOpticalCircuit.update(intentState);
                    continue;
                }
                if (intent instanceof OpticalConnectivityIntent) {
                    summaryOpticalConnectivity.update(intentState);
                    continue;
                }
                if (intent instanceof OpticalOduIntent) {
                    summaryOpticalOdu.update(intentState);
                    continue;
                }
                summaryUnknownType.update(intentState);
            }
        }

        /**
         * Gets JSON representation of all Intents summary.
         *
         * @return JSON representation of all Intents summary
         */
        ObjectNode json() {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode result = mapper.createObjectNode();
            result.set("connectivity", summaryConnectivity.json(mapper));
            result.set("hostToHost", summaryHostToHost.json(mapper));
            result.set("pointToPoint", summaryPointToPoint.json(mapper));
            result.set("multiPointToSinglePoint",
                       summaryMultiPointToSinglePoint.json(mapper));
            result.set("singlePointToMultiPoint",
                       summarySinglePointToMultiPoint.json(mapper));
            result.set("path", summaryPath.json(mapper));
            result.set("linkCollection", summaryLinkCollection.json(mapper));
            result.set("opticalCircuit", summaryOpticalCircuit.json(mapper));
            result.set("opticalConnectivity", summaryOpticalConnectivity.json(mapper));
            result.set("opticalOdu", summaryOpticalOdu.json(mapper));
            result.set("unknownType", summaryUnknownType.json(mapper));
            result.set("all", summaryAll.json(mapper));
            return result;
        }

        /**
         * Prints summary of the intents.
         */
        private String summary() {
            StringBuilder builder = new StringBuilder();
            builder.append(SUMMARY_TITLES);
            builder.append("\n" + SEPARATOR);
            builder.append(summaryAll.summary());
            builder.append(summaryPointToPoint.summary());
            builder.append(summarySinglePointToMultiPoint.summary());
            builder.append(summaryMultiPointToSinglePoint.summary());
            builder.append(summaryHostToHost.summary());
            builder.append(summaryLinkCollection.summary());
            builder.append(summaryConnectivity.summary());
            builder.append(summaryPath.summary());
            builder.append(summaryOpticalCircuit.summary());
            builder.append(summaryOpticalConnectivity.summary());
            builder.append(summaryOpticalOdu.summary());
            builder.append(summaryUnknownType.summary());

            return builder.toString();
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
             * Constructor.
             *
             * @param intentType the scring describing the Intent type
             */
            IntentSummary(String intentType) {
                this.intentType = intentType;
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
            String summary() {
                StringBuilder builder = new StringBuilder();

                builder.append(String.format(
                        "\n%1s%s%14d%14d%14d%14d%14d%14d%14d%14d%14d%14d",
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
                builder.append("\n" + SEPARATOR);

                return builder.toString();
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
    }

    /*
     * Prints detailed information about a specific intent.
     */
    private String detailsFormat(IntentService service, Intent intent) {
        StringBuilder builder = new StringBuilder();
        if (!intent.resources().isEmpty()) {
            builder.append("\n" + String.format(RESOURCES, intent.resources()));
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                builder.append("\n" + String.format(COMMON_SELECTOR, formatSelector(ci.selector())));
            }
            if (!ci.treatment().allInstructions().isEmpty()) {
                builder.append("\n" + String.format(TREATMENT, ci.treatment().allInstructions()));
            }
            if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                builder.append("\n" + String.format(CONSTRAINTS, ci.constraints()));
            }
        }

        if (intent instanceof HostToHostIntent) {
            HostToHostIntent pi = (HostToHostIntent) intent;
            builder.append("\n" + String.format(SRC + HOST, pi.one()));
            builder.append("\n" + String.format(DST + HOST, pi.two()));
        } else if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            builder.append("\n" + formatFilteredCps(Sets.newHashSet(pi.filteredIngressPoint()), INGRESS));
            builder.append("\n" + formatFilteredCps(Sets.newHashSet(pi.filteredEgressPoint()), EGRESS));
        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            builder.append("\n" + formatFilteredCps(pi.filteredIngressPoints(), INGRESS));
            builder.append("\n" + formatFilteredCps(Sets.newHashSet(pi.filteredEgressPoint()), EGRESS));
        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            builder.append("\n" + formatFilteredCps(Sets.newHashSet(pi.filteredIngressPoint()), INGRESS));
            builder.append("\n" + formatFilteredCps(pi.filteredEgressPoints(), EGRESS));
        } else if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            builder.append(String.format("path=%s, cost=%f", pi.path().links(), pi.path().cost()));
        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            builder.append("\n" + String.format("links=%s", li.links()));
            builder.append("\n" + String.format(CP, li.egressPoints()));
        } else if (intent instanceof OpticalCircuitIntent) {
            OpticalCircuitIntent ci = (OpticalCircuitIntent) intent;
            builder.append("\n" + String.format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
        } else if (intent instanceof OpticalConnectivityIntent) {
            OpticalConnectivityIntent ci = (OpticalConnectivityIntent) intent;
            builder.append("\n" + String.format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
        } else if (intent instanceof OpticalOduIntent) {
            OpticalOduIntent ci = (OpticalOduIntent) intent;
            builder.append("\n" + String.format("src=%s, dst=%s", ci.getSrc(), ci.getDst()));
        }

        List<Intent> installable = service.getInstallableIntents(intent.key());
        installable.stream().filter(i -> contentFilter.filter(i));
        if (showInstallable && installable != null && !installable.isEmpty()) {
            builder.append("\n" + String.format(INSTALLABLE, installable));
        }
        return builder.toString();
    }

    /*
     * Prints out a formatted string, given a list of connect points.
     */
    private String formatFilteredCps(Set<FilteredConnectPoint> fCps, String prefix) {
        StringBuilder builder = new StringBuilder();
        builder.append(prefix);
        builder.append(FILTERED_CPS);
        fCps.forEach(fCp -> builder.append("\n" + String.format(formatFilteredCp(fCp))));

        return builder.toString();
    }

    /*
     * Prints out a formatted string, given a filtered connect point.
     */
    private String formatFilteredCp(FilteredConnectPoint fCp) {
        ConnectPoint connectPoint = fCp.connectPoint();
        TrafficSelector selector = fCp.trafficSelector();
        StringBuilder builder = new StringBuilder();
        builder.append(INDENTATION + String.format(CP, connectPoint));
        builder.append(SPACE + String.format(SELECTOR, formatSelector(selector)));

        return builder.toString();
    }

    /*
     * Prints out a formatted string, given a traffic selector
     */
    private String formatSelector(TrafficSelector ts) {
        StringBuilder builder = new StringBuilder();
        List<Criterion> criteria = Lists.newArrayList(ts.criteria());

        if (criteria == null || criteria.isEmpty()) {
            builder.append(INHERITED);
            return builder.toString();
        }

        criteria.forEach(c -> {
            builder.append(c.toString());
            if (criteria.indexOf(c) < criteria.size() - 1) {
                builder.append(", ");
            }
        });

        return builder.toString();
    }

    private String fullFormat(Intent intent) {
        return fullFormat(intent, null);
    }

    /*
     * Prints information about the intent state, given an intent.
     */
    private String fullFormat(Intent intent, String state) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format(ID, intent.id()));
        if (state != null) {
            builder.append("\n" + String.format(STATE, state));
        }
        builder.append("\n" + String.format(KEY, intent.key()));
        builder.append("\n" + String.format(TYPE, intent.getClass().getSimpleName()));
        builder.append("\n" + String.format(APP_ID, intent.appId().name()));

        return builder.toString();
    }

    /*
     * Prints a detailed information about intents.
     */
    private void printIntents(IntentService service) {
        for (Intent intent : service.getIntents()) {
            IntentState state = service.getIntentState(intent.key());
            String intentFormat = fullFormat(intent, state.toString());
            String detailsIntentFormat = detailsFormat(service, intent);
            if (state != null && (contentFilter.filter(
                    intentFormat + detailsIntentFormat))) {
                StringBuilder builder = new StringBuilder();
                builder.append(intentFormat)
                       .append(detailsIntentFormat)
                       .append("\n");
                print(builder.toString());
            }
        }
    }

    /*
     * Produces a JSON array from the intents specified.
     */
    private JsonNode json(Iterable<Intent> intents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        StreamSupport.stream(intents.spliterator(), false)
                .filter(intent -> contentFilter.filter(jsonForEntity(intent, Intent.class).toString()))
                .forEach(intent -> result.add(jsonForEntity(intent, Intent.class)));
        return result;
    }

}
