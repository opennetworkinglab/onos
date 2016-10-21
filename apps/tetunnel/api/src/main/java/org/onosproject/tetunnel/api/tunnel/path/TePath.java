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

package org.onosproject.tetunnel.api.tunnel.path;

import org.onosproject.tetunnel.api.lsp.TeLspKey;

import java.util.List;

/**
 * Representation of a TE tunnel path.
 */
public interface TePath {

    /**
     * Types of TE path.
     */
    enum Type {
        /**
         * Designates a dynamically computed path.
         */
        DYNAMIC,
        /**
         * Designates a path with explicit route.
         */
        EXPLICIT
    }

    /**
     * Returns type of this TE path.
     *
     * @return type of this TE path
     */
    Type type();

    /**
     * Returns keys of TE LSPs of this TE path.
     *
     * @return list of keys of TE LSPs
     */
    List<TeLspKey> lsps();

    /**
     * Returns specified route of ths (Explicit) TE path.
     *
     * @return list of TE route subobjects.
     */
    List<TeRouteSubobject> explicitRoute();

    /**
     * Returns secondary TE paths of this TE path.
     *
     * @return list of secondary TE paths.
     */
    List<TePath> secondaryPaths();

    //TODO add more attributes here.
}
