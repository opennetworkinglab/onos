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

package org.onosproject.imr.data;

import com.google.common.base.MoreObjects;

import java.util.List;

/**
 * Representation of the routing confiuration submitted by the off-platform application
 * It is composed by a list of {@link Route}.
 */
public class RoutingConfigurations {
    public List<Route> routingList;

    /**
     * Returns the list of all the routing submitted by the off-platform application.
     * @return the routing list
     */
    public List<Route> routingList() {
        return routingList;
    }

    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("RoutingList", this.routingList)
                .toString();
    }

}
