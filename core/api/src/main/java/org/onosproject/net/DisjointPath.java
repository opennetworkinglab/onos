/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net;


/**
 * Representation of a contiguous directed path in a network. Path comprises
 * of a sequence of links, where adjacent links must share the same device,
 * meaning that destination of the source of one link must coincide with the
 * destination of the previous link.
 */
public interface DisjointPath extends Path {

    /**
     * Uses backup path.
     *
     * @return boolean corresponding to whether request to use
     *          backup was successful.
     *
     * @deprecated in 1.11.0
     */
    @Deprecated
    boolean useBackup();

    /**
     * Gets primary path.
     *
     * @return primary path
     */
    Path primary();

    /**
     * Gets secondary path.
     *
     * @return secondary path, or null if there is no secondary path available.
     */
    Path backup();
}
