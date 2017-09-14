/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.behaviour.trafficcontrol;


import com.google.common.annotations.Beta;
import org.onlab.util.Identifier;

import java.net.URI;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Unique identifier for an ONOS Policer {@link org.onosproject.net.behaviour.trafficcontrol.Policer}.
 * It uniquely identifies a Policer in the scope of a single device inside ONOS. There may not be any
 * correspondence with the identifiers of the technology implementing the Policer in the device.
 * Mapping (if necessary) is left to the specific implementation.
 */
@Beta
public final class PolicerId extends Identifier<String> {

    /**
     * Represents either no id, or an unspecified id.
     */
    public static final PolicerId NONE = policerId("none:none");

    private static final int POLICER_ID_MAX_LENGTH = 1024;

    private final URI uri;

    // Not allowed
    private PolicerId(URI u) {
        super(u.toString());
        uri = u;
    }

    // Needed for serialization
    private PolicerId() {
        super();
        uri = null;
    }

    /**
     * Creates a policer id using the supplied URI.
     *
     * @param uri policer id URI
     * @return PolicerId
     */
    public static PolicerId policerId(URI uri) {
        return new PolicerId(uri);
    }

    /**
     * Creates a policer id using the supplied URI string.
     *
     * @param string policer id URI string
     * @return PolicerID
     */
    public static PolicerId policerId(String string) {
        checkArgument(string.length() <= POLICER_ID_MAX_LENGTH,
                      "URI string exceeds maximum length " + POLICER_ID_MAX_LENGTH);
        return policerId(URI.create(string));
    }

    /**
     * Returns the backing URI.
     *
     * @return backing URI
     */
    public URI uri() {
        return uri;
    }

}
