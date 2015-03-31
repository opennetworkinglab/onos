/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Message handler for intent view related messages.
 */
public class IntentViewMessageHandler extends AbstractTabularViewMessageHandler {

    /**
     * Creates a new message handler for the intent messages.
     */
    protected IntentViewMessageHandler() {
        super(ImmutableSet.of("intentDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        ObjectNode payload = payload(message);
        String sortCol = string(payload, "sortCol", "appId");
        String sortDir = string(payload, "sortDir", "asc");

        IntentService service = get(IntentService.class);
        TableRow[] rows = generateTableRows(service);
        RowComparator rc =
                new RowComparator(sortCol, RowComparator.direction(sortDir));
        Arrays.sort(rows, rc);
        ArrayNode intents = generateArrayNode(rows);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("intents", intents);

        connection().sendMessage("intentDataResponse", 0, rootNode);
    }

    private TableRow[] generateTableRows(IntentService service) {
        List<TableRow> list = new ArrayList<>();
        for (Intent intent : service.getIntents()) {
            list.add(new IntentTableRow(intent));
        }
        return list.toArray(new TableRow[list.size()]);
    }

    /**
     * TableRow implementation for {@link Intent intents}.
     */
    private static class IntentTableRow extends AbstractTableRow {

        private static final String APP_ID = "appId";
        private static final String KEY = "key";
        private static final String TYPE = "type";
        private static final String PRIORITY = "priority";
        private static final String RESOURCES = "resources";
        private static final String DETAILS = "details";

        private static final String[] COL_IDS = {
                APP_ID, KEY, TYPE, PRIORITY, RESOURCES, DETAILS
        };

        private String formatDetails(Intent intent) {
            StringBuilder details = new StringBuilder("");

            if (intent instanceof ConnectivityIntent) {
                ConnectivityIntent ci = (ConnectivityIntent) intent;
                if (!ci.selector().criteria().isEmpty()) {
                    details.append("selector=")
                            .append(ci.selector().criteria().toString());
                }
                if (!ci.treatment().allInstructions().isEmpty()) {
                    details.append("treatment=")
                            .append(ci.treatment().allInstructions().toString());
                }
                if (ci.constraints() != null && !ci.constraints().isEmpty()) {
                    details.append("constraints=")
                            .append(ci.constraints().toString());
                }
            }

            if (intent instanceof HostToHostIntent) {
                HostToHostIntent pi = (HostToHostIntent) intent;
                details.append(" host1=")
                        .append(pi.one().toString())
                        .append(", host2=")
                        .append(pi.two().toString());

            } else if (intent instanceof PointToPointIntent) {
                PointToPointIntent pi = (PointToPointIntent) intent;
                ConnectPoint ingress = pi.ingressPoint();
                ConnectPoint egress = pi.egressPoint();
                details.append(" ingress=")
                        .append(ingress.elementId().toString())
                        .append('/')
                        .append(ingress.port().toString())

                        .append(", egress=")
                        .append(egress.elementId().toString())
                        .append('/')
                        .append(egress.port().toString())
                        .append(' ');

            } else if (intent instanceof MultiPointToSinglePointIntent) {
                MultiPointToSinglePointIntent pi
                        = (MultiPointToSinglePointIntent) intent;
                Set<ConnectPoint> ingresses = pi.ingressPoints();
                ConnectPoint egress = pi.egressPoint();

                details.append(" ingress=");
                for (ConnectPoint ingress : ingresses) {
                    details.append(ingress.elementId().toString())
                            .append('/')
                            .append(ingress.port().toString())
                            .append(' ');
                }

                details.append(", egress=")
                        .append(egress.elementId().toString())
                        .append('/')
                        .append(egress.port().toString())
                        .append(' ');

            } else if (intent instanceof SinglePointToMultiPointIntent) {
                SinglePointToMultiPointIntent pi
                        = (SinglePointToMultiPointIntent) intent;
                ConnectPoint ingress = pi.ingressPoint();
                Set<ConnectPoint> egresses = pi.egressPoints();

                details.append(" ingress=")
                        .append(ingress.elementId().toString())
                        .append('/')
                        .append(ingress.port().toString())
                        .append(", egress=");

                for (ConnectPoint egress : egresses) {
                    details.append(egress.elementId().toString())
                            .append('/')
                            .append(egress.port().toString())
                            .append(' ');
                }

            } else if (intent instanceof PathIntent) {
                PathIntent pi = (PathIntent) intent;
                details.append(" path=")
                        .append(pi.path().links().toString())
                        .append(", cost=")
                        .append(pi.path().cost());

            } else if (intent instanceof LinkCollectionIntent) {
                LinkCollectionIntent li = (LinkCollectionIntent) intent;
                Set<ConnectPoint> egresses = li.egressPoints();

                details.append(" links=")
                        .append(li.links().toString())
                        .append(", egress=");

                for (ConnectPoint egress : egresses) {
                    details.append(egress.elementId().toString())
                            .append('/')
                            .append(egress.port().toString())
                            .append(' ');
                }
            }

            if (details.toString().equals("")) {
                details.append("No details for this intent");
            } else {
                details.insert(0, "Details: ");
            }
            return details.toString();
        }

        private String formatResources(Intent i) {
            if (!i.resources().isEmpty()) {
                return "Resources: " + i.resources();
            }
            return "No resources for this intent.";
        }

        public IntentTableRow(Intent i) {
            ApplicationId appid = i.appId();

            add(APP_ID, String.valueOf(appid.id()) + " : " + appid.name());
            add(KEY, i.key().toString());
            add(TYPE, i.getClass().getSimpleName());
            add(PRIORITY, Integer.toString(i.priority()));
            add(RESOURCES, formatResources(i));
            add(DETAILS, formatDetails(i));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
