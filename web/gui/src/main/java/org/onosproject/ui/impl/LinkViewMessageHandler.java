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
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.RequestHandler;
import org.onosproject.ui.UiMessageHandlerTwo;
import org.onosproject.ui.impl.TopologyViewMessageHandlerBase.BiLink;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.RowComparator;
import org.onosproject.ui.table.TableRow;
import org.onosproject.ui.table.TableUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.onosproject.ui.impl.TopologyViewMessageHandlerBase.addLink;

/**
 * Message handler for link view related messages.
 */
public class LinkViewMessageHandler extends UiMessageHandlerTwo {

    private static final String LINK_DATA_REQ = "linkDataRequest";


    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new LinkDataRequest());
    }

    // ======================================================================

    private final class LinkDataRequest extends RequestHandler {

        private LinkDataRequest() {
            super(LINK_DATA_REQ);
        }

        @Override
        public void process(long sid, ObjectNode payload) {
            RowComparator rc = TableUtils.createRowComparator(payload, "one");

            LinkService service = get(LinkService.class);
            TableRow[] rows = generateTableRows(service);
            Arrays.sort(rows, rc);
            ObjectNode rootNode = MAPPER.createObjectNode();
            rootNode.set("links", TableUtils.generateArrayNode(rows));

            sendMessage("linkDataResponse", 0, rootNode);
        }

        private TableRow[] generateTableRows(LinkService service) {
            List<TableRow> list = new ArrayList<>();

            // First consolidate all uni-directional links into two-directional ones.
            Map<LinkKey, BiLink> biLinks = Maps.newHashMap();
            service.getLinks().forEach(link -> addLink(biLinks, link));

            // Now scan over all bi-links and produce table rows from them.
            biLinks.values().forEach(biLink -> list.add(new LinkTableRow(biLink)));
            return list.toArray(new TableRow[list.size()]);
        }
    }

    // ======================================================================

    /**
     * TableRow implementation for {@link org.onosproject.net.Link links}.
     */
    private static class LinkTableRow extends AbstractTableRow {

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

        public LinkTableRow(BiLink link) {
            ConnectPoint src = link.one.src();
            ConnectPoint dst = link.one.dst();
            linkState(link);

            add(ONE, concat(src.elementId(), "/", src.port()));
            add(TWO, concat(dst.elementId(), "/", dst.port()));
            add(TYPE, linkType(link).toLowerCase());
            add(STATE, linkState(link));
            add(DIRECTION, link.two != null ? "A <--> B" : "A --> B");
            add(DURABLE, Boolean.toString(link.one.isDurable()));
        }

        private String linkState(BiLink link) {
            return (link.one.state() == Link.State.ACTIVE ||
                    link.two.state() == Link.State.ACTIVE) ?
                    ICON_ID_ONLINE : ICON_ID_OFFLINE;
        }

        private String linkType(BiLink link) {
            return link.two == null || link.one.type() == link.two.type() ?
                    link.one.type().toString() :
                    link.one.type().toString() + " / " + link.two.type().toString();
        }

        @Override
        protected String[] columnIds() {
            return COL_IDS;
        }
    }

}
