/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.NetworkResource;
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
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.table.CellFormatter;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.AppIdFormatter;
import org.onosproject.ui.table.cell.EnumFormatter;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Message handler for intent view related messages.
 */
public class IntentViewMessageHandler extends UiMessageHandler {

    private static final String INTENT_DATA_REQ = "intentDataRequest";
    private static final String INTENT_DATA_RESP = "intentDataResponse";
    private static final String INTENTS = "intents";

    private static final String APP_ID = "appId";
    private static final String KEY = "key";
    private static final String TYPE = "type";
    private static final String PRIORITY = "priority";
    private static final String STATE = "state";
    private static final String RESOURCES = "resources";
    private static final String DETAILS = "details";

    private static final String[] COL_IDS = {
            APP_ID, KEY, TYPE, PRIORITY, STATE, RESOURCES, DETAILS
    };

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new IntentDataRequest());
    }

    // handler for intent table requests
    private final class IntentDataRequest extends TableRequestHandler {
        private static final String NO_ROWS_MESSAGE = "No intents found";

        private IntentDataRequest() {
            super(INTENT_DATA_REQ, INTENT_DATA_RESP, INTENTS);
        }

        @Override
        protected String defaultColumnId() {
            return APP_ID;
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String noRowsMessage(ObjectNode payload) {
            return NO_ROWS_MESSAGE;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(APP_ID, AppIdFormatter.INSTANCE);
            tm.setFormatter(RESOURCES, new ResourcesFormatter());
            tm.setFormatter(DETAILS, new DetailsFormatter());
            tm.setFormatter(STATE, EnumFormatter.INSTANCE);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            IntentService is = get(IntentService.class);
            for (Intent intent : is.getIntents()) {
                populateRow(tm.addRow(), intent, is);
            }
        }

        private void populateRow(TableModel.Row row, Intent intent, IntentService is) {
            row.cell(APP_ID, intent.appId())
                    .cell(KEY, intent.key())
                    .cell(TYPE, intent.getClass().getSimpleName())
                    .cell(PRIORITY, intent.priority())
                    .cell(STATE, is.getIntentState(intent.key()))
                    .cell(RESOURCES, intent)
                    .cell(DETAILS, intent);
        }

        private final class ResourcesFormatter implements CellFormatter {
            private static final String COMMA = ", ";

            @Override
            public String format(Object value) {
                Intent intent = (Intent) value;
                Collection<NetworkResource> resources = intent.resources();
                if (resources.isEmpty()) {
                    return "(No resources for this intent)";
                }
                StringBuilder sb = new StringBuilder("Resources: ");
                for (NetworkResource nr : resources) {
                    sb.append(nr).append(COMMA);
                }
                removeTrailingComma(sb);

                return sb.toString();
            }

            private StringBuilder removeTrailingComma(StringBuilder sb) {
                int pos = sb.lastIndexOf(COMMA);
                sb.delete(pos, sb.length());
                return sb;
            }
        }

        private final class DetailsFormatter implements CellFormatter {
            @Override
            public String format(Object value) {
                return formatDetails((Intent) value, new StringBuilder()).toString();
            }

            private StringBuilder formatDetails(Intent intent, StringBuilder sb) {
                if (intent instanceof ConnectivityIntent) {
                    buildConnectivityDetails((ConnectivityIntent) intent, sb);
                }

                if (intent instanceof HostToHostIntent) {
                    buildHostToHostDetails((HostToHostIntent) intent, sb);

                } else if (intent instanceof PointToPointIntent) {
                    buildPointToPointDetails((PointToPointIntent) intent, sb);

                } else if (intent instanceof MultiPointToSinglePointIntent) {
                    buildMPToSPDetails((MultiPointToSinglePointIntent) intent, sb);

                } else if (intent instanceof SinglePointToMultiPointIntent) {
                    buildSPToMPDetails((SinglePointToMultiPointIntent) intent, sb);

                } else if (intent instanceof PathIntent) {
                    buildPathDetails((PathIntent) intent, sb);

                } else if (intent instanceof LinkCollectionIntent) {
                    buildLinkConnectionDetails((LinkCollectionIntent) intent, sb);
                }

                if (sb.length() == 0) {
                    sb.append("(No details for this intent)");
                } else {
                    sb.insert(0, "Details: ");
                }
                return sb;
            }

            private void appendMultiPointsDetails(Set<ConnectPoint> points,
                                                  StringBuilder sb) {
                for (ConnectPoint point : points) {
                    sb.append(point.elementId())
                            .append('/')
                            .append(point.port())
                            .append(' ');
                }
            }

            private void buildConnectivityDetails(ConnectivityIntent intent,
                                                  StringBuilder sb) {
                Set<Criterion> criteria = intent.selector().criteria();
                List<Instruction> instructions = intent.treatment().allInstructions();
                List<Constraint> constraints = intent.constraints();

                if (!criteria.isEmpty()) {
                    sb.append("Selector: ").append(criteria);
                }
                if (!instructions.isEmpty()) {
                    sb.append("Treatment: ").append(instructions);
                }
                if (constraints != null && !constraints.isEmpty()) {
                    sb.append("Constraints: ").append(constraints);
                }
            }

            private void buildHostToHostDetails(HostToHostIntent intent,
                                                StringBuilder sb) {
                sb.append(" Host 1: ")
                        .append(intent.one())
                        .append(", Host 2: ")
                        .append(intent.two());
            }

            private void buildPointToPointDetails(PointToPointIntent intent,
                                                  StringBuilder sb) {
                ConnectPoint ingress = intent.filteredIngressPoint().connectPoint();
                ConnectPoint egress = intent.filteredEgressPoint().connectPoint();
                sb.append(" Ingress: ")
                        .append(ingress.elementId())
                        .append('/')
                        .append(ingress.port())

                        .append(", Egress: ")
                        .append(egress.elementId())
                        .append('/')
                        .append(egress.port())
                        .append(' ');
            }

            private void buildMPToSPDetails(MultiPointToSinglePointIntent intent,
                                            StringBuilder sb) {
                ConnectPoint egress = intent.egressPoint();

                sb.append(" Ingress=");
                appendMultiPointsDetails(intent.ingressPoints(), sb);

                sb.append(", Egress=")
                        .append(egress.elementId())
                        .append('/')
                        .append(egress.port())
                        .append(' ');
            }

            private void buildSPToMPDetails(SinglePointToMultiPointIntent intent,
                                            StringBuilder sb) {
                ConnectPoint ingress = intent.ingressPoint();

                sb.append(" Ingress=")
                        .append(ingress.elementId())
                        .append('/')
                        .append(ingress.port())
                        .append(", Egress=");

                appendMultiPointsDetails(intent.egressPoints(), sb);
            }

            private void buildPathDetails(PathIntent intent, StringBuilder sb) {
                sb.append(" path=")
                        .append(intent.path().links())
                        .append(", cost=")
                        .append(intent.path().cost());
            }

            private void buildLinkConnectionDetails(LinkCollectionIntent intent,
                                                    StringBuilder sb) {
                sb.append(" links=")
                        .append(intent.links())
                        .append(", egress=");

                appendMultiPointsDetails(intent.egressPoints(), sb);
            }

        }
    }
}
