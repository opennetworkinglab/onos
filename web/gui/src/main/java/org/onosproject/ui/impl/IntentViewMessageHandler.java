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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.HostToHostIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.MultiPointToSinglePointIntent;
import org.onosproject.net.intent.PathIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.SinglePointToMultiPointIntent;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.RowComparator;
import org.onosproject.ui.table.TableRow;
import org.onosproject.ui.table.TableUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Message handler for intent view related messages.
 */
public class IntentViewMessageHandler extends UiMessageHandler {

    /**
     * Creates a new message handler for the intent messages.
     */
    protected IntentViewMessageHandler() {
        super(ImmutableSet.of("intentDataRequest"));
    }

    @Override
    public void process(ObjectNode message) {
        String type = eventType(message);
        if (type.equals("intentDataRequest")) {
            sendIntentList(message);
        }
    }

    private void sendIntentList(ObjectNode message) {
        ObjectNode payload = payload(message);
        RowComparator rc = TableUtils.createRowComparator(payload);

        IntentService service = get(IntentService.class);
        TableRow[] rows = generateTableRows(service);
        Arrays.sort(rows, rc);
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.set("intents", TableUtils.generateArrayNode(rows));

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

        private StringBuilder details = new StringBuilder();

        private void appendMultiPointsDetails(Set<ConnectPoint> points) {
            for (ConnectPoint point : points) {
                details.append(point.elementId())
                        .append('/')
                        .append(point.port())
                        .append(' ');
            }
        }

        private void buildConnectivityDetails(ConnectivityIntent intent) {
            Set<Criterion> criteria = intent.selector().criteria();
            List<Instruction> instructions = intent.treatment().allInstructions();
            List<Constraint> constraints = intent.constraints();

            if (!criteria.isEmpty()) {
                details.append("selector=").append(criteria);
            }
            if (!instructions.isEmpty()) {
                details.append("treatment=").append(instructions);
            }
            if (constraints != null && !constraints.isEmpty()) {
                details.append("constraints=").append(constraints);
            }
        }

        private void buildHostToHostDetails(HostToHostIntent intent) {
            details.append(" host1=")
                    .append(intent.one())
                    .append(", host2=")
                    .append(intent.two());
        }

        private void buildPointToPointDetails(PointToPointIntent intent) {
            ConnectPoint ingress = intent.ingressPoint();
            ConnectPoint egress = intent.egressPoint();
            details.append(" ingress=")
                    .append(ingress.elementId())
                    .append('/')
                    .append(ingress.port())

                    .append(", egress=")
                    .append(egress.elementId())
                    .append('/')
                    .append(egress.port())
                    .append(' ');
        }

        private void buildMPToSPDetails(MultiPointToSinglePointIntent intent) {
            ConnectPoint egress = intent.egressPoint();

            details.append(" ingress=");
            appendMultiPointsDetails(intent.ingressPoints());

            details.append(", egress=")
                    .append(egress.elementId())
                    .append('/')
                    .append(egress.port())
                    .append(' ');
        }

        private void buildSPToMPDetails(SinglePointToMultiPointIntent intent) {
            ConnectPoint ingress = intent.ingressPoint();

            details.append(" ingress=")
                    .append(ingress.elementId())
                    .append('/')
                    .append(ingress.port())
                    .append(", egress=");

            appendMultiPointsDetails(intent.egressPoints());
        }

        private void buildPathDetails(PathIntent intent) {
            details.append(" path=")
                    .append(intent.path().links())
                    .append(", cost=")
                    .append(intent.path().cost());
        }

        private void buildLinkConnectionDetails(LinkCollectionIntent intent) {
            details.append(" links=")
                    .append(intent.links())
                    .append(", egress=");

            appendMultiPointsDetails(intent.egressPoints());
        }

        private String formatDetails(Intent intent) {
            if (intent instanceof ConnectivityIntent) {
                buildConnectivityDetails((ConnectivityIntent) intent);
            }

            if (intent instanceof HostToHostIntent) {
                buildHostToHostDetails((HostToHostIntent) intent);

            } else if (intent instanceof PointToPointIntent) {
                buildPointToPointDetails((PointToPointIntent) intent);

            } else if (intent instanceof MultiPointToSinglePointIntent) {
                buildMPToSPDetails((MultiPointToSinglePointIntent) intent);

            } else if (intent instanceof SinglePointToMultiPointIntent) {
                buildSPToMPDetails((SinglePointToMultiPointIntent) intent);

            } else if (intent instanceof PathIntent) {
                buildPathDetails((PathIntent) intent);

            } else if (intent instanceof LinkCollectionIntent) {
                buildLinkConnectionDetails((LinkCollectionIntent) intent);
            }

            if (details.length() == 0) {
                details.append("(No details for this intent)");
            } else {
                details.insert(0, "Details: ");
            }
            return details.toString();
        }

        private String formatResources(Intent intent) {
            return (intent.resources().isEmpty() ?
                     "(No resources for this intent)" :
                     "Resources: " + intent.resources());
        }

        public IntentTableRow(Intent intent) {
            ApplicationId appid = intent.appId();

            add(APP_ID, concat(appid.id(), " : ", appid.name()));
            add(KEY, intent.key());
            add(TYPE, intent.getClass().getSimpleName());
            add(PRIORITY, intent.priority());
            add(RESOURCES, formatResources(intent));
            add(DETAILS, formatDetails(intent));
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
