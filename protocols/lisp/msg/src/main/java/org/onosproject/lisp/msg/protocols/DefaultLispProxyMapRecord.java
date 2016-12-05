/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.lisp.msg.protocols;

/**
 * A default implementation class of LispMapWithProxy interface.
 */
public final class DefaultLispProxyMapRecord implements LispProxyMapRecord {

    private final LispMapRecord mapRecord;
    private final boolean proxyMapReply;

    private DefaultLispProxyMapRecord(LispMapRecord mapRecord, boolean proxyMapReply) {
        this.mapRecord = mapRecord;
        this.proxyMapReply = proxyMapReply;
    }

    @Override
    public LispMapRecord getMapRecord() {
        return mapRecord;
    }

    @Override
    public boolean isProxyMapReply() {
        return proxyMapReply;
    }

    /**
     * A default builder class that builds MapWithProxy object.
     */
    public static final class DefaultMapWithProxyBuilder implements MapWithProxyBuilder {

        private LispMapRecord mapRecord;
        private boolean proxyMapReply;

        @Override
        public MapWithProxyBuilder withMapRecord(LispMapRecord mapRecord) {
            this.mapRecord = mapRecord;
            return this;
        }

        @Override
        public MapWithProxyBuilder withIsProxyMapReply(boolean proxyMapReply) {
            this.proxyMapReply = proxyMapReply;
            return this;
        }

        @Override
        public LispProxyMapRecord build() {
            return new DefaultLispProxyMapRecord(mapRecord, proxyMapReply);
        }
    }
}
