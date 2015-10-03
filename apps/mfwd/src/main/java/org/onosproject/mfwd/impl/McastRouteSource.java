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

import static com.google.common.base.Preconditions.checkNotNull;
import org.onlab.packet.IpPrefix;

/**
 * This class represents and specific multicast senders source address.  Objects from
 * this class will belong to the sources collection of the multicast group.
 */
public class McastRouteSource extends McastRouteBase {

    // A reference to our parent group
    private McastRouteGroup group;

    /**
     * Create a multicast source with IpPrefixes.
     *
     * @param source the source address
     * @param group the group address
     */
    public McastRouteSource(IpPrefix source, IpPrefix group) {
        super(checkNotNull(source), checkNotNull(group));
    }

    /**
     * Set our parent multicast group.
     *
     * @param group the group this source belongs to
     */
    public void setGroup(McastRouteGroup group) {
        this.group = group;
    }
}