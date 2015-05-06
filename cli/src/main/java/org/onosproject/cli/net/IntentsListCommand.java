/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Lists the inventory of intents and their states.
 */
@Command(scope = "onos", name = "intents",
         description = "Lists the inventory of intents and their states")
public class IntentsListCommand extends AbstractShellCommand {

    @Option(name = "-i", aliases = "--installable",
            description = "Output Installable Intents",
            required = false, multiValued = false)
    private boolean showInstallable = false;

    @Option(name = "-s", aliases = "--summary",
            description = "Intents summary",
            required = false, multiValued = false)
    private boolean intentsSummary = false;

    @Option(name = "-p", aliases = "--pending",
            description = "Show inforamtion about pending intents",
            required = false, multiValued = false)
    private boolean pending = false;

    @Override
    protected void execute() {
        IntentService service = get(IntentService.class);

        if (intentsSummary) {
            IntentSummaries intentSummaries = new IntentSummaries();
            intentSummaries.collectIntentSummary(service,
                                                 service.getIntents());
            if (outputJson()) {
                print("%s", intentSummaries.json());
            } else {
                intentSummaries.printSummary();
            }
            return;
        } else if (pending) {
            if (outputJson()) {
                print("%s", json(service, service.getPending()));
            } else {
                service.getPending().forEach(intent ->
                                print("id=%s, key=%s, type=%s, appId=%s",
                                        intent.id(), intent.key(),
                                        intent.getClass().getSimpleName(),
                                        intent.appId().name())
                );
            }
            return;
        }

        if (outputJson()) {
            print("%s", json(service, service.getIntents()));
        } else {
            for (Intent intent : service.getIntents()) {
                IntentState state = service.getIntentState(intent.key());
                if (state != null) {
                    print("id=%s, state=%s, key=%s, type=%s, appId=%s",
                          intent.id(), state, intent.key(),
                          intent.getClass().getSimpleName(),
                          intent.appId().name());
                    printDetails(service, intent);
                }
            }
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
        private IntentSummary summaryUnknownType;

        /**
         * Initializes the internal state.
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
            result.set("unknownType", summaryUnknownType.json(mapper));
            result.set("all", summaryAll.json(mapper));
            return result;
        }

        /**
         * Prints summary of the intents.
         */
        private void printSummary() {
            summaryConnectivity.printState();
            summaryHostToHost.printState();
            summaryPointToPoint.printState();
            summaryMultiPointToSinglePoint.printState();
            summarySinglePointToMultiPoint.printState();
            summaryPath.printState();
            summaryLinkCollection.printState();
            summaryUnknownType.printState();
            summaryAll.printState();
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

            private static final String FORMAT_SUMMARY_LINE1 =
                "%-23s    total=        %7d   installed=   %7d";
            private static final String FORMAT_SUMMARY_LINE2 =
                "%-23s    withdrawn=    %7d   failed=      %7d";
            private static final String FORMAT_SUMMARY_LINE3 =
                "%-23s    installReq=   %7d   compiling=   %7d";
            private static final String FORMAT_SUMMARY_LINE4 =
                "%-23s    installing=   %7d   recompiling= %7d";
            private static final String FORMAT_SUMMARY_LINE5 =
                "%-23s    withdrawReq=  %7d   withdrawing= %7d";
            private static final String FORMAT_SUMMARY_LINE6 =
                "%-23s    unknownState= %7d";

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
             * @param intentState the state of the Intent
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
             */
            void printState() {
                print(FORMAT_SUMMARY_LINE1, intentType, total, installed);
                print(FORMAT_SUMMARY_LINE2, intentType, withdrawn, failed);
                print(FORMAT_SUMMARY_LINE3, intentType, installReq, compiling);
                print(FORMAT_SUMMARY_LINE4, intentType, installing, recompiling);
                print(FORMAT_SUMMARY_LINE5, intentType, withdrawReq, withdrawing);
                if (unknownState != 0) {
                    print(FORMAT_SUMMARY_LINE6, intentType, unknownState);
                }
            }

            /**
             * Gets the JSON representation of the Intent Summary.
             *
             * @return the JSON representation of the Intent Summary
             */
            JsonNode json(ObjectMapper mapper) {
                ObjectNode result = mapper.createObjectNode()
                    .put("total", total)
                    .put("installed", installed)
                    .put("failed", failed)
                    .put("installReq", installReq)
                    .put("compiling", compiling)
                    .put("installing", installing)
                    .put("recompiling", recompiling)
                    .put("withdrawReq", withdrawReq)
                    .put("withdrawing", withdrawing)
                    .put("withdrawn", withdrawn)
                    .put("unknownState", unknownState);

                return result;
            }
        }
    }

    private void printDetails(IntentService service, Intent intent) {
        if (!intent.resources().isEmpty()) {
            print("    resources=%s", intent.resources());
        }
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent ci = (ConnectivityIntent) intent;
            if (!ci.selector().criteria().isEmpty()) {
                print("    selector=%s", ci.selector().criteria());
            }
            if (!ci.treatment().allInstructions().isEmpty()) {
                print("    treatment=%s", ci.treatment().allInstructions());
            }
            if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                print("    constraints=%s", ci.constraints());
            }
        }

        if (intent instanceof HostToHostIntent) {
            HostToHostIntent pi = (HostToHostIntent) intent;
            print("    host1=%s, host2=%s", pi.one(), pi.two());
        } else if (intent instanceof PointToPointIntent) {
            PointToPointIntent pi = (PointToPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoint());
        } else if (intent instanceof MultiPointToSinglePointIntent) {
            MultiPointToSinglePointIntent pi = (MultiPointToSinglePointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoints(), pi.egressPoint());
        } else if (intent instanceof SinglePointToMultiPointIntent) {
            SinglePointToMultiPointIntent pi = (SinglePointToMultiPointIntent) intent;
            print("    ingress=%s, egress=%s", pi.ingressPoint(), pi.egressPoints());
        } else if (intent instanceof PathIntent) {
            PathIntent pi = (PathIntent) intent;
            print("    path=%s, cost=%d", pi.path().links(), pi.path().cost());
        } else if (intent instanceof LinkCollectionIntent) {
            LinkCollectionIntent li = (LinkCollectionIntent) intent;
            print("    links=%s", li.links());
            print("    egress=%s", li.egressPoints());
        }

        List<Intent> installable = service.getInstallableIntents(intent.key());
        if (showInstallable && installable != null && !installable.isEmpty()) {
            print("    installable=%s", installable);
        }
    }

    // Produces JSON array of the specified intents.
    private JsonNode json(IntentService service, Iterable<Intent> intents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        intents.forEach(intent -> result.add(jsonForEntity(intent, Intent.class)));
        return result;
    }

}
