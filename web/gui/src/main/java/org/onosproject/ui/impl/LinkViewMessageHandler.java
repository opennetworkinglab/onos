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
import org.onosproject.ui.UiMessageHandler;
import org.onosproject.ui.impl.TopologyViewMessageHandlerBase.BiLink;
import org.onosproject.ui.table.AbstractTableRow;
import org.onosproject.ui.table.TableRequestHandler;
import org.onosproject.ui.table.TableRow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

    @Override
    protected Collection<RequestHandler> getHandlers() {
        return ImmutableSet.of(new LinkDataRequest());
    }

    // handler for link table requests
    private final class LinkDataRequest extends TableRequestHandler {
        private LinkDataRequest() {
            super(LINK_DATA_REQ, LINK_DATA_RESP, LINKS);
        }

        @Override
        protected TableRow[] generateTableRows(ObjectNode payload) {
            LinkService service = get(LinkService.class);
            List<TableRow> list = new ArrayList<>();

            // First consolidate all uni-directional links into two-directional ones.
            Map<LinkKey, BiLink> biLinks = Maps.newHashMap();
            service.getLinks().forEach(link -> addLink(biLinks, link));

            // Now scan over all bi-links and produce table rows from them.
            biLinks.values().forEach(biLink -> list.add(new LinkTableRow(biLink)));
            return list.toArray(new TableRow[list.size()]);
        }

        @Override
        protected String defaultColId() {
            return ONE;
        }
    }

    /**
     * TableRow implementation for {@link org.onosproject.net.Link links}.
     */
    private static class LinkTableRow extends AbstractTableRow {

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
