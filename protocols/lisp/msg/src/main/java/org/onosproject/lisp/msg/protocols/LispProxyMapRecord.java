/*
 * Copyright 2016-present Open Networking Foundation
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
 * An interface that wraps LispMapRecord with proxy-bit flag.
 */
public interface LispProxyMapRecord {

    /**
     * Obtains LISP map record.
     *
     * @return LISP map record
     */
    LispMapRecord getMapRecord();

    /**
     * Obtains proxy-map-reply flag.
     *
     * @return proxy-map-reply flag
     */
    boolean isProxyMapReply();

    /**
     * A builder of LISP map with proxy flag internal data structure.
     */
    interface MapWithProxyBuilder {

        /**
         * Sets LISP map record.
         *
         * @param mapRecord map record
         * @return MapWithProxyBuilder object
         */
        MapWithProxyBuilder withMapRecord(LispMapRecord mapRecord);

        /**
         * Sets isProxyMapReply flag.
         *
         * @param isProxyMapReply isProxyMapReply flag
         * @return MapWithProxyBuilder object
         */
        MapWithProxyBuilder withIsProxyMapReply(boolean isProxyMapReply);

        /**
         * Builds LISP map with proxy data object.
         *
         * @return LISP map with proxy data object
         */
        LispProxyMapRecord build();
    }
}
