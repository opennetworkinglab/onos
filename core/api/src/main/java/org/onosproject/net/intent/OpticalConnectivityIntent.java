/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.net.intent;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.ConnectPoint;

import java.util.Collections;

/**
 * An optical layer intent for connectivity from one transponder port to another
 * transponder port. No traffic selector or traffic treatment are needed.
 */
public class OpticalConnectivityIntent extends Intent {
    protected final ConnectPoint src;
    protected final ConnectPoint dst;

    /**
     * Creates an optical connectivity intent between the specified
     * connection points.
     *
     * @param appId application identification
     * @param src the source transponder port
     * @param dst the destination transponder port
     */
    public OpticalConnectivityIntent(ApplicationId appId,
                                     ConnectPoint src, ConnectPoint dst) {

        this(appId, null, src, dst);
    }

    /**
     * Creates an optical connectivity intent between the specified
     * connection points.
     *
     * @param appId application identification
     * @param key intent key
     * @param src the source transponder port
     * @param dst the destination transponder port
     */
    public OpticalConnectivityIntent(ApplicationId appId,
                                     Key key,
                                     ConnectPoint src, ConnectPoint dst) {
        super(appId, key, Collections.emptyList());
        this.src = src;
        this.dst = dst;
    }


    /**
     * Constructor for serializer.
     */
    protected OpticalConnectivityIntent() {
        super();
        this.src = null;
        this.dst = null;
    }

    /**
     * Returns the source transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getSrc() {
        return src;
    }

    /**
     * Returns the destination transponder port.
     *
     * @return source transponder port
     */
    public ConnectPoint getDst() {
        return dst;
    }
}
