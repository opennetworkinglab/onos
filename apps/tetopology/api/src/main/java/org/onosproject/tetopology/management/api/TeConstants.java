/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api;

/**
 * The common TE constants.
 */
public final class TeConstants {
    /**
     * Lowest priority of a GMPLS traffic link.
     */
    public static final short MIN_PRIORITY = 0;

    /**
     * Highest priority of a GMPLS traffic link.
     */
    public static final short MAX_PRIORITY = 7;

    /**
     * Size of the BitSet flags used in TE Topology data structures, such as
     * TE links, TE nodes, and TE topologies.
     */
    public static final short FLAG_MAX_BITS = 16;

    /**
     * Indication of a Nil flag or a uninitialized long integer.
     */
    public static final long NIL_LONG_VALUE = 0;

    // no instantiation
    private TeConstants() {
    }
}
