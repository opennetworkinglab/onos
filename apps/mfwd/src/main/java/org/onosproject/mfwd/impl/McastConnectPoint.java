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
 */
package org.onosproject.mfwd.impl;

import org.onosproject.net.ConnectPoint;
import java.util.EnumSet;
import java.util.Set;

/**
 * Mulitcast ConnectPoint adds a variable to track the usage
 * of these multicast endpoints.
 */
public class McastConnectPoint {

    private ConnectPoint connectPoint;

    public enum JoinSource {
        STATIC, IGMP, PIM;
    }

    public EnumSet<JoinSource> interest = EnumSet.noneOf(JoinSource.class);

    public McastConnectPoint(ConnectPoint cp) {
        this.connectPoint = cp;
    }

    public McastConnectPoint(ConnectPoint cp, JoinSource src) {
        this.connectPoint = cp;
        interest.add(src);
    }

    public McastConnectPoint(String connectPoint, JoinSource src) {
        ConnectPoint cp = ConnectPoint.deviceConnectPoint(connectPoint);
        this.connectPoint = cp;
        this.interest.add(src);
    }

    /**
     * Get the connect point.
     *
     * @return connectPoint
     */
    public ConnectPoint getConnectPoint() {
        return connectPoint;
    }

    /**
     * Get the sources of interest for this egressPoint.
     *
     * @return interest flags
     */
    public Set<JoinSource> getInterest() {
        return interest;
    }
}
