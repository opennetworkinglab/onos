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
 *
 */

package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Specialization of XosFunction for URL filtering.
 */
public class UrlFilterFunction extends DefaultXosFunction {

    private static final String LEVEL = "level";
    private static final String URI_PATTERN = "%s/%s/";

    /**
     * Denotes the URL filtering levels available. From most restrictive
     * to least restrictive. Note: <em>NONE</em> allows nothing;
     * <em>ALL</em> allows everything.
     */
    public enum Level { NONE, G, PG, PG_13, R, ALL }

    /**
     * The default URL filtering level
     */
    public static final Level DEFAULT_LEVEL = Level.G;

    public UrlFilterFunction() {
        super(XosFunctionDescriptor.URL_FILTER);
    }

    @Override
    public void applyParam(SubscriberUser user, String param, String value) {
        Memento memo = user.getMemento(descriptor());
        checkNotNull(memo, "missing memento for " + descriptor());
        UrlFilterMemento ufMemo = (UrlFilterMemento) memo;

        if (LEVEL.equals(param)) {
            Level newLevel = Level.valueOf(value.toUpperCase());
            ufMemo.setLevel(newLevel);

            // Also store the (string version) of the level
            // (not in the memento). Hackish, but that's how it is for now.
            user.setUrlFilterLevel(value);
        }
    }

    @Override
    public Memento createMemento() {
        return new UrlFilterMemento();
    }

    class UrlFilterMemento implements Memento {
        private Level level = DEFAULT_LEVEL;

        public ObjectNode toObjectNode() {
            ObjectNode node = MAPPER.createObjectNode();
            node.put(LEVEL, level.name());
            return node;
        }

        public void setLevel(Level level) {
            this.level = level;
        }

        public String level() {
            return level.toString();
        }
    }

    @Override
    public String xosUrlApply(SubscriberUser user) {
        XosFunctionDescriptor xfd = XosFunctionDescriptor.URL_FILTER;
        UrlFilterMemento memo = (UrlFilterMemento) user.getMemento(xfd);
        return String.format(URI_PATTERN, xfd.id(), memo.level());
    }
}
