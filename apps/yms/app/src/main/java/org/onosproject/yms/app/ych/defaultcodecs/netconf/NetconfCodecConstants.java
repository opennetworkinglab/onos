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

package org.onosproject.yms.app.ych.defaultcodecs.netconf;

/**
 * Represents utilities constants which are used while codec encoding
 * and decoding.
 */
public final class NetconfCodecConstants {

    // no instantiation
    private NetconfCodecConstants() {
    }

    /**
     * Static attribute for edit config string.
     */
    static final String EDIT_CONFIG = "edit-config";

    /**
     * Static attribute for edit config string.
     */
    static final String GET_CONFIG = "get-config";

    /**
     * Static attribute for edit config string.
     */
    static final String GET = "get";

    /**
     * Static attribute for edit config string.
     */
    static final String CONFIG = "config";

    /**
     * Static attribute for edit config string.
     */
    static final String DATA = "data";

    /**
     * Static attribute for edit config string.
     */
    static final String FILTER = "filter";

    /**
     * Static attribute for edit config string.
     */
    public static final String OPERATION = "operation";
}
