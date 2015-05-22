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

    /**
     * Denotes the URL filtering levels available.
     */
    public enum Level { PG, PG_13, R }

    /**
     * The default URL filtering level
     */
    public static final Level DEFAULT_LEVEL = Level.PG;

    public UrlFilterFunction(XosFunctionDescriptor xfd) {
        super(xfd);
    }

    @Override
    public void applyParam(SubscriberUser user, String param, String value) {
        Memento memo = user.getMemento(descriptor());
        checkNotNull(memo, "missing memento for " + descriptor());
        UrlFilterMemento ufMemo = (UrlFilterMemento) memo;

        if (LEVEL.equals(param)) {
            Level newLevel = Level.valueOf(value.toUpperCase());
            ufMemo.setLevel(newLevel);
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
    }
}
