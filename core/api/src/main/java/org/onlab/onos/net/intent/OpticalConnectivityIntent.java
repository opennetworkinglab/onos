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
package org.onlab.onos.net.intent;

import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.net.ConnectPoint;

/**
 * An optical layer Intent for a connectivity from one Transponder port to another
 * Transponder port. No trafficSelector as well as trafficTreament are needed.
 *
 */
public class OpticalConnectivityIntent extends Intent {
    protected ConnectPoint src;
    protected ConnectPoint dst;

    /**
     * Constructor.
     *
     * @param id  ID for this new Intent object.
     * @param src The source transponder port.
     * @param dst The destination transponder port.
     */
    public OpticalConnectivityIntent(ApplicationId appId, ConnectPoint src, ConnectPoint dst) {
        super(id(OpticalConnectivityIntent.class, src, dst),
                appId, null);
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
     * Gets source transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getSrcConnectPoint() {
        return src;
    }

    /**
     * Gets destination transponder port.
     *
     * @return The source transponder port.
     */
    public ConnectPoint getDst() {
        return dst;
    }
}
