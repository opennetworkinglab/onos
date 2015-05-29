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
import com.google.common.collect.Maps;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.TopologyViewMessageHandlerBase.BiLink;
import org.onosproject.ui.table.TableModel;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.cell.ConnectPointFormatter;
import org.onosproject.ui.table.cell.EnumFormatter;

import java.util.Collection;
import java.util.Map;

import static org.onosproject.ui.impl.TopologyViewMessageHandlerBase.addLink;

/**
 * Message handler for link view related messages.
 */
public class LinkViewMessageHandler extends UiMessageHandler {

    private static final String LINK_DATA_REQ = "linkDataRequest";
    private static final String LINK_DATA_RESP = "linkDataResponse";
    private static final String LINKS = "links";

    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String TYPE = "type";
    private static final String STATE = "_iconid_state";
    private static final String DIRECTION = "direction";
    private static final String DURABLE = "durable";

    private static final String[] COL_IDS = {
            ONE, TWO, TYPE, STATE, DIRECTION, DURABLE
    };

    private static final String ICON_ID_ONLINE = "active";
    private static final String ICON_ID_OFFLINE = "inactive";

    @Override
    protected Collection<RequestHandler> createRequestHandlers() {
        return ImmutableSet.of(new LinkDataRequest());
    }

    // handler for link table requests
    private final class LinkDataRequest extends TableRequestHandler {
        private LinkDataRequest() {
            super(LINK_DATA_REQ, LINK_DATA_RESP, LINKS);
        }

        @Override
        protected String[] getColumnIds() {
            return COL_IDS;
        }

        @Override
        protected String defaultColumnId() {
            return ONE;
        }

        @Override
        protected TableModel createTableModel() {
            TableModel tm = super.createTableModel();
            tm.setFormatter(ONE, ConnectPointFormatter.INSTANCE);
            tm.setFormatter(TWO, ConnectPointFormatter.INSTANCE);
            tm.setFormatter(TYPE, EnumFormatter.INSTANCE);
            return tm;
        }

        @Override
        protected void populateTable(TableModel tm, ObjectNode payload) {
            LinkService ls = get(LinkService.class);

            // First consolidate all uni-directional links into two-directional ones.
            Map<LinkKey, BiLink> biLinks = Maps.newHashMap();
            ls.getLinks().forEach(link -> addLink(biLinks, link));

            // Now scan over all bi-links and produce table rows from them.
            biLinks.values().forEach(biLink -> populateRow(tm.addRow(), biLink));
        }

        private void populateRow(TableModel.Row row, BiLink biLink) {
            row.cell(ONE, biLink.one.src())
                .cell(TWO, biLink.one.dst())
                .cell(TYPE, linkType(biLink))
                .cell(STATE, linkState(biLink))
                .cell(DIRECTION, linkDir(biLink))
                .cell(DURABLE, biLink.one.isDurable());
        }

        private String linkType(BiLink link) {
            StringBuilder sb = new StringBuilder();
            sb.append(link.one.type());
            if (link.two != null && link.two.type() != link.one.type()) {
                sb.append(" / ").append(link.two.type());
            }
            return sb.toString();
        }

        private String linkState(BiLink link) {
            return (link.one.state() == Link.State.ACTIVE ||
                    link.two.state() == Link.State.ACTIVE) ?
                    ICON_ID_ONLINE : ICON_ID_OFFLINE;
        }

        private String linkDir(BiLink link) {
            return link.two != null ? "A &harr; B" : "A &rarr; B";
        }
    }
}
